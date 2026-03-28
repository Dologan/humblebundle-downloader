package com.dologan.humblebrowser.download

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dologan.humblebrowser.data.db.dao.FileDao
import com.dologan.humblebrowser.data.db.entities.DownloadState
import com.dologan.humblebrowser.data.db.entities.FileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDao: FileDao,
) {
    private val workManager = WorkManager.getInstance(context)
    private val activeDownloads = mutableMapOf<String, UUID>()

    fun getDownloadDir(): File {
        val dir = File(context.getExternalFilesDir(null), "HumbleBrowser")
        dir.mkdirs()
        return dir
    }

    fun enqueueDownload(file: FileEntity, bundleName: String, productName: String): UUID {
        val destPath = File(
            getDownloadDir(),
            "$bundleName/$productName/${file.filename}",
        ).absolutePath

        val inputData = workDataOf(
            DownloadWorker.KEY_FILE_ID to file.id,
            DownloadWorker.KEY_DOWNLOAD_URL to file.downloadUrl,
            DownloadWorker.KEY_DEST_PATH to destPath,
        )

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .addTag("download")
            .addTag("file:${file.id}")
            .build()

        activeDownloads[file.id] = workRequest.id
        workManager.enqueue(workRequest)
        return workRequest.id
    }

    suspend fun cancelDownload(fileId: String) {
        val workId = activeDownloads.remove(fileId)
        if (workId != null) {
            workManager.cancelWorkById(workId)
        }
        fileDao.updateDownloadState(fileId, DownloadState.NONE, null)
    }

    fun isLargeFile(file: FileEntity): Boolean {
        return file.fileSize > LARGE_FILE_THRESHOLD
    }

    companion object {
        const val LARGE_FILE_THRESHOLD = 50L * 1024 * 1024 // 50MB
    }
}
