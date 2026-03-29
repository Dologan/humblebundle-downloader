package com.dologan.humblebrowser.ui.browser

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dologan.humblebrowser.data.db.entities.DownloadState
import com.dologan.humblebrowser.data.db.entities.FileEntity
import com.dologan.humblebrowser.domain.model.TreeNode
import com.dologan.humblebrowser.ui.components.FilterBar
import com.dologan.humblebrowser.ui.components.LargeFileConfirmDialog
import com.dologan.humblebrowser.ui.components.SearchBar
import com.dologan.humblebrowser.ui.components.TagManageDialog
import com.dologan.humblebrowser.ui.components.ViewModeSelector
import com.dologan.humblebrowser.ui.components.verticalScrollbar
import com.dologan.humblebrowser.util.FileActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateToSettings: () -> Unit,
    onSignIn: () -> Unit,
    onLogout: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val libraryStats by viewModel.libraryStats.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var pendingLargeFile by remember { mutableStateOf<Triple<FileEntity, String, String>?>(null) }
    var pendingDeleteFile by remember { mutableStateOf<FileEntity?>(null) }
    var showFullReloadConfirm by remember { mutableStateOf(false) }
    var showLibraryInfo by remember { mutableStateOf(false) }
    var showClearDownloadsConfirm by remember { mutableStateOf(false) }
    var tagManageNode by remember { mutableStateOf<TreeNode?>(null) }
    // Holds the file waiting for a SAF destination to be chosen
    var pendingDownloadToFile by remember { mutableStateOf<FileEntity?>(null) }

    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        uri?.let { destUri ->
            pendingDownloadToFile?.let { file -> viewModel.downloadFileTo(file, destUri) }
        }
        pendingDownloadToFile = null
    }

    LaunchedEffect(Unit) {
        viewModel.sync()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HumbleBrowser") },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                    IconButton(onClick = { viewModel.sync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Full Reload") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showFullReloadConfirm = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.showHidden) "Hide Hidden Items" else "Show Hidden Items") },
                            onClick = {
                                viewModel.toggleShowHidden()
                                showMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.autoHideEmpty) "Show Empty Folders" else "Hide Empty Folders") },
                            onClick = {
                                viewModel.toggleAutoHideEmpty()
                                showMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.downloadedOnly) "Show All Items" else "Show Downloaded Only") },
                            onClick = {
                                viewModel.toggleDownloadedOnly()
                                showMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Library Info") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showLibraryInfo = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Switch Account / Sign Out") },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onLogout()
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ViewModeSelector(
                currentMode = state.viewMode,
                onModeSelected = viewModel::setViewMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            AnimatedVisibility(visible = showFilters) {
                FilterBar(
                    platforms = state.availablePlatforms,
                    activePlatforms = state.activePlatformFilters,
                    onTogglePlatform = viewModel::togglePlatformFilter,
                    extensions = state.availableExtensions,
                    activeExtensions = state.activeExtensionFilters,
                    onToggleExtension = viewModel::toggleExtensionFilter,
                    tags = state.availableTags,
                    activeTags = state.activeTagFilters,
                    onToggleTag = viewModel::toggleTagFilter,
                    libraryMaxFileSize = state.libraryMaxFileSize,
                    sizeFilterMin = state.sizeFilterMin,
                    sizeFilterMax = state.sizeFilterMax,
                    onSizeFilterChange = viewModel::setSizeFilter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (state.isSyncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.tree.isEmpty() && !state.isSyncing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.hasLibraryData) {
                        Text(
                            text = "No results match the current filters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "No library loaded",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Sign in to your Humble Bundle account to browse and download your library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Button(onClick = onSignIn) {
                                Text("Sign In to Humble Bundle")
                            }
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(listState),
                ) {
                    items(
                        items = state.tree,
                        key = { it.key },
                    ) { node ->
                        TreeNodeRow(
                            node = node,
                            onToggleExpand = { viewModel.toggleExpanded(node.key) },
                            onHide = { viewModel.hidePath(node.virtualPath) },
                            onUnhide = { viewModel.unhidePath(node.virtualPath) },
                            onDownload = {
                                if (node is TreeNode.FileNode) {
                                    val file = node.file
                                    if (file.downloadState == DownloadState.DOWNLOADING) {
                                        viewModel.cancelDownload(file.id)
                                    } else if (viewModel.isLargeFile(file)) {
                                        pendingLargeFile = Triple(file, node.bundleName, node.productName)
                                    } else {
                                        viewModel.downloadFile(file, node.bundleName, node.productName)
                                    }
                                } else {
                                    // Download all for group/product nodes
                                    // Collect all visible file nodes under this node and download each
                                    val prefix = node.virtualPath
                                    state.tree
                                        .filterIsInstance<TreeNode.FileNode>()
                                        .filter { it.virtualPath.startsWith(prefix) }
                                        .filter { it.file.downloadState == DownloadState.NONE || it.file.downloadState == DownloadState.FAILED }
                                        .forEach { fileNode ->
                                            viewModel.downloadFile(fileNode.file, fileNode.bundleName, fileNode.productName)
                                        }
                                }
                            },
                            onDownloadTo = {
                                if (node is TreeNode.FileNode) {
                                    pendingDownloadToFile = node.file
                                    safLauncher.launch(node.file.filename)
                                }
                            },
                            onOpen = {
                                if (node is TreeNode.FileNode) {
                                    node.file.localPath?.let { FileActions.openFile(context, it) }
                                }
                            },
                            onShare = {
                                if (node is TreeNode.FileNode) {
                                    node.file.localPath?.let { FileActions.shareFile(context, it) }
                                }
                            },
                            onDelete = {
                                if (node is TreeNode.FileNode) {
                                    pendingDeleteFile = node.file
                                }
                            },
                            onManageTags = {
                                tagManageNode = node
                            },
                        )
                    }
                }
            }
        }
    }

    // Full reload confirmation dialog
    if (showFullReloadConfirm) {
        AlertDialog(
            onDismissRequest = { showFullReloadConfirm = false },
            title = { Text("Full Reload") },
            text = { Text("This will re-download all library metadata from Humble Bundle. This may take a while if you have many purchases.") },
            confirmButton = {
                TextButton(onClick = {
                    showFullReloadConfirm = false
                    viewModel.sync(forceRefresh = true)
                }) {
                    Text("Reload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullReloadConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Library Info dialog
    if (showLibraryInfo) {
        AlertDialog(
            onDismissRequest = { showLibraryInfo = false },
            title = { Text("Library Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LibraryInfoRow("Bundles", libraryStats.totalBundles.toString())
                    LibraryInfoRow("Total files", libraryStats.totalFiles.toString())
                    LibraryInfoRow("Library size", formatSize(libraryStats.totalLibrarySize))
                    LibraryInfoRow("Downloaded files", libraryStats.downloadedFiles.toString())
                    LibraryInfoRow("Downloaded data", formatSize(libraryStats.downloadedSize))
                }
            },
            confirmButton = {
                TextButton(onClick = { showLibraryInfo = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLibraryInfo = false
                        showClearDownloadsConfirm = true
                    },
                    enabled = libraryStats.downloadedFiles > 0,
                ) {
                    Text("Clear All Downloads")
                }
            },
        )
    }

    // Clear all downloads confirmation
    if (showClearDownloadsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsConfirm = false },
            title = { Text("Clear All Downloads") },
            text = { Text("Delete all ${libraryStats.downloadedFiles} downloaded files (${formatSize(libraryStats.downloadedSize)}) from local storage? You can re-download them later.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllDownloads()
                    showClearDownloadsConfirm = false
                }) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDownloadsConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Delete downloaded file confirmation dialog
    pendingDeleteFile?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFile = null },
            title = { Text("Delete Downloaded File") },
            text = { Text("Delete \"${file.filename}\" from local storage? You can re-download it later.") },
            confirmButton = {
                TextButton(onClick = {
                    file.localPath?.let { viewModel.deleteDownload(file.id, it) }
                    pendingDeleteFile = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFile = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    pendingLargeFile?.let { (file, bundleName, productName) ->
        LargeFileConfirmDialog(
            filename = file.filename,
            fileSize = formatSize(file.fileSize),
            onConfirm = {
                viewModel.downloadFile(file, bundleName, productName)
                pendingLargeFile = null
            },
            onDismiss = { pendingLargeFile = null },
        )
    }

    // Tag management dialog
    tagManageNode?.let { node ->
        val nodePath = node.virtualPath
        val nodeName = when (node) {
            is TreeNode.GroupNode -> node.displayName
            is TreeNode.ProductNode -> node.product.humanName
            is TreeNode.FileNode -> node.file.filename
        }
        val currentTags = when (node) {
            is TreeNode.GroupNode -> node.tags
            is TreeNode.ProductNode -> node.tags
            is TreeNode.FileNode -> node.tags
        }
        TagManageDialog(
            itemName = nodeName,
            currentTags = currentTags,
            onAddTag = { tag -> viewModel.addTagToPath(nodePath, tag) },
            onRemoveTag = { tag -> viewModel.removeTagFromPath(nodePath, tag) },
            onDismiss = { tagManageNode = null },
        )
    }
}

@Composable
private fun LibraryInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}
