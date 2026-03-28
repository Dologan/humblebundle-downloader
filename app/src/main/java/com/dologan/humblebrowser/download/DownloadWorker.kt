package com.dologan.humblebrowser.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dologan.humblebrowser.data.db.dao.FileDao
import com.dologan.humblebrowser.data.db.entities.DownloadState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val fileDao: FileDao,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val fileId = inputData.getString(KEY_FILE_ID) ?: return Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val destPath = inputData.getString(KEY_DEST_PATH) ?: return Result.failure()

        fileDao.setDownloadState(fileId, DownloadState.DOWNLOADING)

        return try {
            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()

            val request = Request.Builder().url(downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                fileDao.setDownloadState(fileId, DownloadState.FAILED)
                return Result.failure(workDataOf(KEY_ERROR to "HTTP ${response.code}"))
            }

            val body = response.body ?: run {
                fileDao.setDownloadState(fileId, DownloadState.FAILED)
                return Result.failure(workDataOf(KEY_ERROR to "Empty response body"))
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            destFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            // Cancelled - clean up partial file
                            output.close()
                            destFile.delete()
                            fileDao.updateDownloadState(fileId, DownloadState.NONE, null)
                            return Result.failure(workDataOf(KEY_ERROR to "Cancelled"))
                        }
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                }
            }

            val lastModified = response.header("Last-Modified")
            fileDao.updateDownloadState(fileId, DownloadState.COMPLETE, destPath)

            Result.success(
                workDataOf(
                    KEY_FILE_ID to fileId,
                    KEY_DEST_PATH to destPath,
                    KEY_LAST_MODIFIED to (lastModified ?: ""),
                )
            )
        } catch (e: Exception) {
            // Clean up partial file on failure
            File(destPath).delete()
            fileDao.setDownloadState(fileId, DownloadState.FAILED)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed")))
        }
    }

    companion object {
        const val KEY_FILE_ID = "file_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_DEST_PATH = "dest_path"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_LAST_MODIFIED = "last_modified"
    }
}
