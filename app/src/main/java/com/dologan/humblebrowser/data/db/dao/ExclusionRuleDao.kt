package com.dologan.humblebrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dologan.humblebrowser.data.db.entities.ExclusionRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExclusionRuleDao {
    @Query("SELECT * FROM exclusion_rules ORDER BY id ASC")
    fun observeAll(): Flow<List<ExclusionRuleEntity>>

    @Query("SELECT * FROM exclusion_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<ExclusionRuleEntity>

    @Query("SELECT * FROM exclusion_rules WHERE enabled = 1")
    fun observeEnabled(): Flow<List<ExclusionRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: ExclusionRuleEntity)

    @Query("DELETE FROM exclusion_rules WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE exclusion_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Int, enabled: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<ExclusionRuleEntity>)

    @Query("DELETE FROM exclusion_rules")
    suspend fun deleteAll()
}
