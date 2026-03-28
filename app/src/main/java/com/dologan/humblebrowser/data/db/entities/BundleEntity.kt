package com.dologan.humblebrowser.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bundles")
data class BundleEntity(
    @PrimaryKey val orderId: String,
    val bundleName: String,
    val createdAt: Long = 0L,
    val lastSyncedAt: Long = 0L,
)
