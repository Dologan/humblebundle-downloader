package com.dologan.humblebrowser.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exclusion_rules")
data class ExclusionRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pattern: String,
    val isRegex: Boolean = false,
    val enabled: Boolean = true,
)
