package com.dologan.humblebrowser.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_tags",
    indices = [Index("path"), Index("tag"), Index(value = ["path", "tag"], unique = true)],
)
data class ItemTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String,
    val tag: String,
)
