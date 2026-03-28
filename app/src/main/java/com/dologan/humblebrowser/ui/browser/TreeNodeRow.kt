package com.dologan.humblebrowser.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dologan.humblebrowser.data.db.entities.DownloadState
import com.dologan.humblebrowser.domain.model.TreeNode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TreeNodeRow(
    node: TreeNode,
    onToggleExpand: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val isHidden = when (node) {
        is TreeNode.GroupNode -> node.isHidden
        is TreeNode.ProductNode -> node.isHidden
        is TreeNode.FileNode -> false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    when (node) {
                        is TreeNode.GroupNode -> onToggleExpand()
                        is TreeNode.ProductNode -> onToggleExpand()
                        is TreeNode.FileNode -> {
                            when (node.file.downloadState) {
                                DownloadState.COMPLETE -> onOpen()
                                DownloadState.DOWNLOADING -> onDownload() // triggers cancel
                                else -> showContextMenu = true
                            }
                        }
                    }
                },
                onLongClick = { showContextMenu = true },
            )
            .padding(
                start = (16 + node.depth * 24).dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp,
            )
            .alpha(if (isHidden) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Expand/collapse or file icon
        when (node) {
            is TreeNode.GroupNode -> {
                Icon(
                    imageVector = if (node.expanded) Icons.Default.KeyboardArrowDown
                    else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (node.expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            is TreeNode.ProductNode -> {
                Icon(
                    imageVector = if (node.expanded) Icons.Default.KeyboardArrowDown
                    else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (node.expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            is TreeNode.FileNode -> {
                Spacer(modifier = Modifier.width(24.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Name and metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (node) {
                    is TreeNode.GroupNode -> node.displayName
                    is TreeNode.ProductNode -> node.product.humanName
                    is TreeNode.FileNode -> node.file.filename
                },
                style = when (node) {
                    is TreeNode.FileNode -> MaterialTheme.typography.bodyMedium
                    else -> MaterialTheme.typography.bodyLarge
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            when (node) {
                is TreeNode.GroupNode -> {
                    Text(
                        text = "${node.childCount} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is TreeNode.ProductNode -> {
                    Text(
                        text = "${node.childCount} files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is TreeNode.FileNode -> {
                    val sizeText = formatFileSize(node.file.fileSize)
                    val platformText = node.file.platform
                    Text(
                        text = "$platformText · $sizeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Download progress
            if (node is TreeNode.FileNode && node.file.downloadState == DownloadState.DOWNLOADING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }

        // Download status icon for files
        if (node is TreeNode.FileNode) {
            val icon = when (node.file.downloadState) {
                DownloadState.COMPLETE -> Icons.Default.CheckCircle
                DownloadState.DOWNLOADING -> Icons.Default.Cancel
                DownloadState.FAILED -> Icons.Default.Error
                else -> Icons.Default.CloudDownload
            }
            val tint = when (node.file.downloadState) {
                DownloadState.COMPLETE -> MaterialTheme.colorScheme.primary
                DownloadState.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                imageVector = icon,
                contentDescription = node.file.downloadState,
                modifier = Modifier.size(20.dp),
                tint = tint,
            )
        }

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            if (isHidden) {
                DropdownMenuItem(
                    text = { Text("Unhide") },
                    onClick = {
                        showContextMenu = false
                        onUnhide()
                    },
                )
            } else if (node !is TreeNode.FileNode) {
                DropdownMenuItem(
                    text = { Text("Download All") },
                    onClick = {
                        showContextMenu = false
                        onDownload()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Hide") },
                    onClick = {
                        showContextMenu = false
                        onHide()
                    },
                )
            }
            if (node is TreeNode.FileNode) {
                if (node.file.downloadState == DownloadState.NONE ||
                    node.file.downloadState == DownloadState.FAILED
                ) {
                    DropdownMenuItem(
                        text = { Text("Download") },
                        onClick = {
                            showContextMenu = false
                            onDownload()
                        },
                    )
                }
                if (node.file.downloadState == DownloadState.DOWNLOADING) {
                    DropdownMenuItem(
                        text = { Text("Cancel Download") },
                        onClick = {
                            showContextMenu = false
                            onDownload()
                        },
                    )
                }
                if (node.file.downloadState == DownloadState.COMPLETE) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        onClick = {
                            showContextMenu = false
                            onOpen()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            showContextMenu = false
                            onShare()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Hide") },
                    onClick = {
                        showContextMenu = false
                        onHide()
                    },
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}
