package com.dologan.humblebrowser.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dologan.humblebrowser.data.db.entities.DownloadState
import com.dologan.humblebrowser.data.db.entities.FileEntity
import com.dologan.humblebrowser.domain.model.TreeNode
import com.dologan.humblebrowser.domain.model.ViewMode
import com.dologan.humblebrowser.ui.components.FilterBar
import com.dologan.humblebrowser.ui.components.LargeFileConfirmDialog
import com.dologan.humblebrowser.ui.components.SearchBar
import com.dologan.humblebrowser.ui.components.ViewModeSelector
import com.dologan.humblebrowser.util.FileActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var pendingLargeFile by remember { mutableStateOf<Triple<FileEntity, String, String>?>(null) }

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
                    IconButton(onClick = { viewModel.sync(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
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

            PullToRefreshBox(
                isRefreshing = state.isSyncing,
                onRefresh = { viewModel.sync(forceRefresh = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.tree.isEmpty() && !state.isSyncing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotBlank()) "No results found"
                            else "No items to display.\nPull to refresh.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                            )
                        }
                    }
                }
            }
        }
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
