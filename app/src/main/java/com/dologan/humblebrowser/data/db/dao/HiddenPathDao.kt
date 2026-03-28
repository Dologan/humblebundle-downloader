package com.dologan.humblebrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dologan.humblebrowser.data.db.entities.HiddenPathEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenPathDao {
    @Query("SELECT * FROM hidden_paths")
    fun observeAll(): Flow<List<HiddenPathEntity>>

    @Query("SELECT path FROM hidden_paths")
    fun observeAllPaths(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hide(entity: HiddenPathEntity)

    @Query("DELETE FROM hidden_paths WHERE path = :path")
    suspend fun unhide(path: String)

    @Query("DELETE FROM hidden_paths")
    suspend fun unhideAll()
}
