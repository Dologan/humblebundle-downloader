package com.dologan.humblebrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dologan.humblebrowser.data.db.entities.ItemTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemTagDao {

    @Query("SELECT * FROM item_tags ORDER BY tag ASC")
    fun observeAll(): Flow<List<ItemTagEntity>>

    @Query("SELECT DISTINCT tag FROM item_tags ORDER BY tag ASC")
    fun observeAllTags(): Flow<List<String>>

    @Query("SELECT tag FROM item_tags WHERE path = :path ORDER BY tag ASC")
    fun observeTagsForPath(path: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTag(entity: ItemTagEntity)

    @Query("DELETE FROM item_tags WHERE path = :path AND tag = :tag")
    suspend fun removeTag(path: String, tag: String)

    @Query("DELETE FROM item_tags")
    suspend fun clearAll()
}
