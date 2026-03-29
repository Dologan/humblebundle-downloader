package com.dologan.humblebrowser.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dologan.humblebrowser.data.db.dao.BundleDao
import com.dologan.humblebrowser.data.db.dao.ExclusionRuleDao
import com.dologan.humblebrowser.data.db.dao.FileDao
import com.dologan.humblebrowser.data.db.dao.HiddenPathDao
import com.dologan.humblebrowser.data.db.dao.ItemTagDao
import com.dologan.humblebrowser.data.db.dao.ProductDao
import com.dologan.humblebrowser.data.db.entities.BundleEntity
import com.dologan.humblebrowser.data.db.entities.ExclusionRuleEntity
import com.dologan.humblebrowser.data.db.entities.FileEntity
import com.dologan.humblebrowser.data.db.entities.FileEntityFts
import com.dologan.humblebrowser.data.db.entities.HiddenPathEntity
import com.dologan.humblebrowser.data.db.entities.ItemTagEntity
import com.dologan.humblebrowser.data.db.entities.ProductEntity

@Database(
    entities = [
        BundleEntity::class,
        ProductEntity::class,
        FileEntity::class,
        FileEntityFts::class,
        HiddenPathEntity::class,
        ExclusionRuleEntity::class,
        ItemTagEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class HumbleDatabase : RoomDatabase() {
    abstract fun bundleDao(): BundleDao
    abstract fun productDao(): ProductDao
    abstract fun fileDao(): FileDao
    abstract fun hiddenPathDao(): HiddenPathDao
    abstract fun exclusionRuleDao(): ExclusionRuleDao
    abstract fun itemTagDao(): ItemTagDao
}
