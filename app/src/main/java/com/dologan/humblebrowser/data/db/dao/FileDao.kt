package com.dologan.humblebrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dologan.humblebrowser.data.db.entities.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE productId = :productId ORDER BY filename ASC")
    fun observeByProduct(productId: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE productId = :productId ORDER BY filename ASC")
    suspend fun getByProduct(productId: String): List<FileEntity>

    @Query("SELECT * FROM files ORDER BY filename ASC")
    fun observeAll(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY filename ASC")
    suspend fun getAll(): List<FileEntity>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getById(id: String): FileEntity?

    @Query(
        """
        SELECT f.* FROM files f
        JOIN files_fts fts ON f.rowid = fts.rowid
        WHERE files_fts MATCH :query
        ORDER BY f.filename ASC
        """
    )
    fun search(query: String): Flow<List<FileEntity>>

    @Query("SELECT DISTINCT platform FROM files")
    fun observeDistinctPlatforms(): Flow<List<String>>

    @Query(
        """
        SELECT DISTINCT LOWER(
            CASE
                WHEN filename LIKE '%.%'
                THEN SUBSTR(filename, INSTR(filename, '.') + 1)
                ELSE ''
            END
        ) FROM files WHERE filename LIKE '%.%'
        """
    )
    fun observeDistinctExtensions(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM files WHERE productId = :productId AND downloadState = 'COMPLETE'")
    fun countDownloadedByProduct(productId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(files: List<FileEntity>)

    @Query("UPDATE files SET downloadState = :state, localPath = :localPath WHERE id = :fileId")
    suspend fun updateDownloadState(fileId: String, state: String, localPath: String? = null)

    @Query("UPDATE files SET downloadState = :state WHERE id = :fileId")
    suspend fun setDownloadState(fileId: String, state: String)

    @Query("DELETE FROM files WHERE productId IN (SELECT id FROM products WHERE orderId = :orderId)")
    suspend fun deleteByBundle(orderId: String)
}
