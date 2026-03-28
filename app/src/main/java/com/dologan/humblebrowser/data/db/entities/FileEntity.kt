package com.dologan.humblebrowser.data.db.entities

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("productId"), Index("platform"), Index("downloadState")],
)
data class FileEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val filename: String,
    val platform: String,
    val fileSize: Long = 0L,
    val md5: String? = null,
    val downloadUrl: String,
    val uploadedAt: Long? = null,
    val localPath: String? = null,
    val downloadState: String = DownloadState.NONE,
    val lastModified: String? = null,
)

object DownloadState {
    const val NONE = "NONE"
    const val DOWNLOADING = "DOWNLOADING"
    const val COMPLETE = "COMPLETE"
    const val FAILED = "FAILED"
}

@Entity(tableName = "files_fts")
@Fts4(contentEntity = FileEntity::class)
data class FileEntityFts(
    val filename: String,
)
