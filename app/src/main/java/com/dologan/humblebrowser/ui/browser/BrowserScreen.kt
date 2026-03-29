package com.dologan.humblebrowser.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var pendingLargeFile by remember { mutableStateOf<Triple<FileEntity, String, String>?>(null) }
    var pendingDeleteFile by remember { mutableStateOf<FileEntity?>(null) }
    var showFullReloadConfirm by remember { mutableStateOf(false) }

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
                            text = { Text(if (state.autoHideEmpty) "Show Empty Folders" else "Auto-hide Empty Folders") },
                            onClick = {
                                viewModel.toggleAutoHideEmpty()
                                showMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.downloadedOnly) "Show All Items" else "Downloaded Only") },
                            onClick = {
                                viewModel.toggleDownloadedOnly()
                                showMenu = false
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
                    if (state.searchQuery.isNotBlank()) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.bodyLarge,
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
}

private fun formatSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}
