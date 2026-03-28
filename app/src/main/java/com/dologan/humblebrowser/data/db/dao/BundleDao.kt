package com.dologan.humblebrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dologan.humblebrowser.data.db.entities.BundleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BundleDao {
    @Query("SELECT * FROM bundles ORDER BY bundleName ASC")
    fun observeAll(): Flow<List<BundleEntity>>

    @Query("SELECT * FROM bundles ORDER BY bundleName ASC")
    suspend fun getAll(): List<BundleEntity>

    @Query("SELECT * FROM bundles WHERE orderId = :orderId")
    suspend fun getById(orderId: String): BundleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bundle: BundleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(bundles: List<BundleEntity>)

    @Query("DELETE FROM bundles")
    suspend fun deleteAll()
}
