package com.dologan.humblebrowser.domain.model

import com.dologan.humblebrowser.data.db.entities.BundleEntity
import com.dologan.humblebrowser.data.db.entities.FileEntity
import com.dologan.humblebrowser.data.db.entities.ProductEntity

sealed class TreeNode {
    abstract val key: String
    abstract val depth: Int
    abstract val virtualPath: String

    data class GroupNode(
        override val key: String,
        override val depth: Int,
        override val virtualPath: String,
        val displayName: String,
        val expanded: Boolean,
        val childCount: Int = 0,
        val isHidden: Boolean = false,
    ) : TreeNode()

    data class ProductNode(
        val product: ProductEntity,
        val bundleName: String,
        val expanded: Boolean,
        val childCount: Int = 0,
        val isHidden: Boolean = false,
    ) : TreeNode() {
        override val key: String get() = product.id
        override val depth: Int get() = 1
        override val virtualPath: String get() = "$bundleName/${product.humanName}"
    }

    data class FileNode(
        val file: FileEntity,
        val bundleName: String,
        val productName: String,
    ) : TreeNode() {
        override val key: String get() = file.id
        override val depth: Int get() = 2
        override val virtualPath: String get() = "$bundleName/$productName/${file.filename}"
    }
}

enum class ViewMode {
    BY_BUNDLE,
    BY_TYPE,
    ALPHABETICAL,
}
