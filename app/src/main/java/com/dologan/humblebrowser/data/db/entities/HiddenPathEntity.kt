package com.dologan.humblebrowser.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_paths")
data class HiddenPathEntity(
    @PrimaryKey val path: String,
    val hiddenAt: Long = System.currentTimeMillis(),
)
