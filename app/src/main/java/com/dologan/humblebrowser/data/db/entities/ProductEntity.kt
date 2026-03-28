package com.dologan.humblebrowser.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = BundleEntity::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val humanName: String,
    val machineName: String,
    val iconUrl: String? = null,
)
