package com.dologan.humblebrowser.download

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dologan.humblebrowser.data.api.HumbleBundleApi
import com.dologan.humblebrowser.data.db.dao.FileDao
import com.dologan.humblebrowser.data.db.entities.DownloadState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.OutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val fileDao: FileDao,
    private val okHttpClient: OkHttpClient,
    private val api: HumbleBundleApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val fileId = inputData.getString(KEY_FILE_ID) ?: return Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val destPath = inputData.getString(KEY_DEST_PATH)
        val destUriStr = inputData.getString(KEY_DEST_URI)

        if (destPath == null && destUriStr == null) return Result.failure()

        return try {
            var response = executeDownload(downloadUrl)

            if (!response.isSuccessful && (response.code == 403 || response.code == 401)) {
                response.close()
                val freshUrl = refreshDownloadUrl(fileId)
                if (freshUrl != null) {
                    response = executeDownload(freshUrl)
                } else {
                    fileDao.setDownloadState(fileId, DownloadState.FAILED)
                    return Result.failure(
                        workDataOf(KEY_ERROR to "Download URL expired and could not be refreshed")
                    )
                }
            }

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

            // Open output stream — either a file path or a SAF URI
            val outputStream: OutputStream = if (destPath != null) {
                val destFile = File(destPath)
                destFile.parentFile?.mkdirs()
                destFile.outputStream()
            } else {
                val uri = Uri.parse(destUriStr)
                applicationContext.contentResolver.openOutputStream(uri)
                    ?: run {
                        fileDao.setDownloadState(fileId, DownloadState.FAILED)
                        return Result.failure(workDataOf(KEY_ERROR to "Could not open destination"))
                    }
            }

            outputStream.use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            fileDao.updateDownloadState(fileId, DownloadState.NONE, null)
                            destPath?.let { File(it).delete() }
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

            // For path downloads, persist the local path; for URI downloads, just mark complete
            fileDao.updateDownloadState(fileId, DownloadState.COMPLETE, destPath)

            Result.success(workDataOf(KEY_FILE_ID to fileId, KEY_DEST_PATH to (destPath ?: "")))
        } catch (e: Exception) {
            destPath?.let { File(it).delete() }
            fileDao.setDownloadState(fileId, DownloadState.FAILED)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed")))
        }
    }

    private fun executeDownload(url: String): okhttp3.Response {
        val request = Request.Builder().url(url).build()
        return okHttpClient.newCall(request).execute()
    }

    private suspend fun refreshDownloadUrl(fileId: String): String? {
        return try {
            val parts = fileId.split(":", limit = 3)
            if (parts.size < 3) return null
            val orderId = parts[0]
            val filename = parts[2]

            val order = api.getOrder(orderId)
            for (sub in order.subproducts) {
                for (download in sub.downloads) {
                    for (struct in download.downloadStruct) {
                        val url = struct.url?.web ?: continue
                        val urlFilename = url.substringAfterLast("/").substringBefore("?")
                        if (urlFilename == filename || struct.name == filename) {
                            fileDao.updateDownloadUrl(fileId, url)
                            return url
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val KEY_FILE_ID = "file_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_DEST_PATH = "dest_path"
        const val KEY_DEST_URI = "dest_uri"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
    }
}
