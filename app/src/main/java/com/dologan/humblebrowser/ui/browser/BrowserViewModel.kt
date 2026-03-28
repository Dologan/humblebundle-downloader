package com.dologan.humblebrowser.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dologan.humblebrowser.data.db.dao.BundleDao
import com.dologan.humblebrowser.data.db.dao.ExclusionRuleDao
import com.dologan.humblebrowser.data.db.dao.FileDao
import com.dologan.humblebrowser.data.db.dao.HiddenPathDao
import com.dologan.humblebrowser.data.db.dao.ProductDao
import com.dologan.humblebrowser.data.db.entities.BundleEntity
import com.dologan.humblebrowser.data.db.entities.FileEntity
import com.dologan.humblebrowser.data.db.entities.HiddenPathEntity
import com.dologan.humblebrowser.data.db.entities.ProductEntity
import com.dologan.humblebrowser.data.repository.LibraryRepository
import com.dologan.humblebrowser.download.DownloadManager
import com.dologan.humblebrowser.domain.model.TreeNode
import com.dologan.humblebrowser.domain.model.ViewMode
import com.dologan.humblebrowser.util.ExclusionMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserUiState(
    val tree: List<TreeNode> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val searchQuery: String = "",
    val viewMode: ViewMode = ViewMode.BY_BUNDLE,
    val activePlatformFilters: Set<String> = emptySet(),
    val activeExtensionFilters: Set<String> = emptySet(),
    val showHidden: Boolean = false,
    val autoHideEmpty: Boolean = false,
    val availablePlatforms: List<String> = emptyList(),
    val availableExtensions: List<String> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val bundleDao: BundleDao,
    private val productDao: ProductDao,
    private val fileDao: FileDao,
    private val hiddenPathDao: HiddenPathDao,
    private val exclusionRuleDao: ExclusionRuleDao,
    val downloadManager: DownloadManager,
) : ViewModel() {

    private val _expandedNodes = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _viewMode = MutableStateFlow(ViewMode.BY_BUNDLE)
    private val _platformFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _extensionFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _showHidden = MutableStateFlow(false)
    private val _autoHideEmpty = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BrowserUiState> = combine(
        bundleDao.observeAll(),
        productDao.observeAll(),
        fileDao.observeAll(),
        hiddenPathDao.observeAll(),
        exclusionRuleDao.observeEnabled(),
    ) { bundles, products, files, hiddenPaths, exclusionRules ->
        buildUiState(bundles, products, files, hiddenPaths.map { it.path }.toSet(), exclusionRules)
    }.combine(
        combine(
            _expandedNodes,
            _searchQuery,
            _viewMode,
            _platformFilters,
            _extensionFilters,
        ) { expanded, search, viewMode, platforms, extensions ->
            FilterState(expanded, search, viewMode, platforms, extensions)
        }
    ) { dbState, filterState ->
        dbState.copy(
            searchQuery = filterState.search,
            viewMode = filterState.viewMode,
            activePlatformFilters = filterState.platforms,
            activeExtensionFilters = filterState.extensions,
        ).let { state ->
            state.copy(tree = buildTree(state, filterState))
        }
    }.combine(
        combine(_isSyncing, _showHidden, _autoHideEmpty, _errorMessage) { syncing, showHidden, autoHide, error ->
            Triple(syncing, showHidden to autoHide, error)
        }
    ) { state, (syncing, hideSettings, error) ->
        state.copy(
            isSyncing = syncing,
            showHidden = hideSettings.first,
            autoHideEmpty = hideSettings.second,
            errorMessage = error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BrowserUiState(isLoading = true),
    )

    private data class FilterState(
        val expanded: Set<String>,
        val search: String,
        val viewMode: ViewMode,
        val platforms: Set<String>,
        val extensions: Set<String>,
    )

    // Cached data references for tree building
    private var cachedBundles: List<BundleEntity> = emptyList()
    private var cachedProducts: List<ProductEntity> = emptyList()
    private var cachedFiles: List<FileEntity> = emptyList()
    private var cachedHiddenPaths: Set<String> = emptySet()
    private var cachedExclusionMatcher: ExclusionMatcher = ExclusionMatcher(emptyList())

    private fun buildUiState(
        bundles: List<BundleEntity>,
        products: List<ProductEntity>,
        files: List<FileEntity>,
        hiddenPaths: Set<String>,
        exclusionRules: List<com.dologan.humblebrowser.data.db.entities.ExclusionRuleEntity>,
    ): BrowserUiState {
        cachedBundles = bundles
        cachedProducts = products
        cachedFiles = files
        cachedHiddenPaths = hiddenPaths
        cachedExclusionMatcher = ExclusionMatcher(exclusionRules)

        return BrowserUiState(
            availablePlatforms = files.map { it.platform }.distinct().sorted(),
            availableExtensions = files.mapNotNull { extractExtension(it.filename) }.distinct().sorted(),
        )
    }

    private fun buildTree(state: BrowserUiState, filterState: FilterState): List<TreeNode> {
        val filteredFiles = cachedFiles.filter { file ->
            val platformOk = filterState.platforms.isEmpty() || file.platform in filterState.platforms
            val extOk = filterState.extensions.isEmpty() ||
                extractExtension(file.filename) in filterState.extensions
            val searchOk = filterState.search.isBlank() ||
                file.filename.contains(filterState.search, ignoreCase = true)

            platformOk && extOk && searchOk
        }

        val productFileMap = filteredFiles.groupBy { it.productId }
        val productMap = cachedProducts.associateBy { it.id }
        val bundleMap = cachedBundles.associateBy { it.orderId }

        return when (filterState.viewMode) {
            ViewMode.BY_BUNDLE -> buildByBundleTree(
                bundleMap, productMap, productFileMap, filterState.expanded,
            )
            ViewMode.BY_TYPE -> buildByTypeTree(
                bundleMap, productMap, productFileMap, filteredFiles, filterState.expanded,
            )
            ViewMode.ALPHABETICAL -> buildAlphabeticalTree(
                bundleMap, productMap, productFileMap, filterState.expanded,
            )
        }
    }

    private fun buildByBundleTree(
        bundleMap: Map<String, BundleEntity>,
        productMap: Map<String, ProductEntity>,
        productFileMap: Map<String, List<FileEntity>>,
        expanded: Set<String>,
    ): List<TreeNode> {
        val nodes = mutableListOf<TreeNode>()
        val bundleProducts = cachedProducts.groupBy { it.orderId }

        for (bundle in cachedBundles) {
            val bundlePath = bundle.bundleName
            val isHidden = bundlePath in cachedHiddenPaths
            if (isHidden && !_showHidden.value) continue
            if (cachedExclusionMatcher.isExcluded(bundlePath)) continue

            val products = bundleProducts[bundle.orderId] ?: emptyList()
            val visibleProducts = products.filter { p ->
                val pPath = "$bundlePath/${p.humanName}"
                val pHidden = pPath in cachedHiddenPaths
                if (pHidden && !_showHidden.value) return@filter false
                if (cachedExclusionMatcher.isExcluded(pPath)) return@filter false
                if (_autoHideEmpty.value && (productFileMap[p.id]?.isEmpty() != false)) return@filter false
                true
            }

            if (_autoHideEmpty.value && visibleProducts.isEmpty()) continue

            nodes.add(
                TreeNode.GroupNode(
                    key = bundle.orderId,
                    depth = 0,
                    virtualPath = bundlePath,
                    displayName = bundle.bundleName,
                    expanded = bundle.orderId in expanded,
                    childCount = visibleProducts.size,
                    isHidden = isHidden,
                )
            )

            if (bundle.orderId in expanded) {
                for (product in visibleProducts) {
                    val productPath = "$bundlePath/${product.humanName}"
                    val pHidden = productPath in cachedHiddenPaths
                    val files = productFileMap[product.id] ?: emptyList()
                    val visibleFiles = files.filter { f ->
                        !cachedExclusionMatcher.isExcluded("$productPath/${f.filename}")
                    }

                    nodes.add(
                        TreeNode.ProductNode(
                            product = product,
                            bundleName = bundle.bundleName,
                            expanded = product.id in expanded,
                            childCount = visibleFiles.size,
                            isHidden = pHidden,
                        )
                    )

                    if (product.id in expanded) {
                        for (file in visibleFiles) {
                            nodes.add(
                                TreeNode.FileNode(
                                    file = file,
                                    bundleName = bundle.bundleName,
                                    productName = product.humanName,
                                )
                            )
                        }
                    }
                }
            }
        }
        return nodes
    }

    private fun buildByTypeTree(
        bundleMap: Map<String, BundleEntity>,
        productMap: Map<String, ProductEntity>,
        productFileMap: Map<String, List<FileEntity>>,
        filteredFiles: List<FileEntity>,
        expanded: Set<String>,
    ): List<TreeNode> {
        val nodes = mutableListOf<TreeNode>()
        val filesByPlatform = filteredFiles.groupBy { it.platform }

        for ((platform, files) in filesByPlatform.toSortedMap()) {
            val typeKey = "type:$platform"
            val productIds = files.map { it.productId }.distinct()

            nodes.add(
                TreeNode.GroupNode(
                    key = typeKey,
                    depth = 0,
                    virtualPath = platform,
                    displayName = platform.replaceFirstChar { it.uppercase() },
                    expanded = typeKey in expanded,
                    childCount = productIds.size,
                )
            )

            if (typeKey in expanded) {
                for (productId in productIds) {
                    val product = productMap[productId] ?: continue
                    val bundle = bundleMap[product.orderId]
                    val bundleName = bundle?.bundleName ?: "Unknown"
                    val productFiles = files.filter { it.productId == productId }

                    nodes.add(
                        TreeNode.ProductNode(
                            product = product,
                            bundleName = bundleName,
                            expanded = product.id in expanded,
                            childCount = productFiles.size,
                        )
                    )

                    if (product.id in expanded) {
                        for (file in productFiles) {
                            nodes.add(
                                TreeNode.FileNode(
                                    file = file,
                                    bundleName = bundleName,
                                    productName = product.humanName,
                                )
                            )
                        }
                    }
                }
            }
        }
        return nodes
    }

    private fun buildAlphabeticalTree(
        bundleMap: Map<String, BundleEntity>,
        productMap: Map<String, ProductEntity>,
        productFileMap: Map<String, List<FileEntity>>,
        expanded: Set<String>,
    ): List<TreeNode> {
        val nodes = mutableListOf<TreeNode>()
        val sortedProducts = cachedProducts
            .filter { productFileMap.containsKey(it.id) }
            .sortedBy { it.humanName.lowercase() }

        for (product in sortedProducts) {
            val bundle = bundleMap[product.orderId]
            val bundleName = bundle?.bundleName ?: "Unknown"
            val productPath = "$bundleName/${product.humanName}"
            val isHidden = productPath in cachedHiddenPaths
            if (isHidden && !_showHidden.value) continue

            val files = productFileMap[product.id] ?: emptyList()

            nodes.add(
                TreeNode.ProductNode(
                    product = product,
                    bundleName = bundleName,
                    expanded = product.id in expanded,
                    childCount = files.size,
                    isHidden = isHidden,
                )
            )

            if (product.id in expanded) {
                for (file in files) {
                    nodes.add(
                        TreeNode.FileNode(
                            file = file,
                            bundleName = bundleName,
                            productName = product.humanName,
                        )
                    )
                }
            }
        }
        return nodes
    }

    fun toggleExpanded(key: String) {
        _expandedNodes.value = _expandedNodes.value.let {
            if (key in it) it - key else it + key
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        _expandedNodes.value = emptySet() // collapse all on mode switch
    }

    fun togglePlatformFilter(platform: String) {
        _platformFilters.value = _platformFilters.value.let {
            if (platform in it) it - platform else it + platform
        }
    }

    fun toggleExtensionFilter(extension: String) {
        _extensionFilters.value = _extensionFilters.value.let {
            if (extension in it) it - extension else it + extension
        }
    }

    fun toggleShowHidden() {
        _showHidden.value = !_showHidden.value
    }

    fun toggleAutoHideEmpty() {
        _autoHideEmpty.value = !_autoHideEmpty.value
    }

    fun hidePath(path: String) {
        viewModelScope.launch {
            hiddenPathDao.hide(HiddenPathEntity(path = path))
        }
    }

    fun unhidePath(path: String) {
        viewModelScope.launch {
            hiddenPathDao.unhide(path)
        }
    }

    fun sync(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            val result = libraryRepository.syncLibrary(forceRefresh)
            result.onFailure { e ->
                _errorMessage.value = e.message ?: "Sync failed"
            }
            _isSyncing.value = false
        }
    }

    fun downloadFile(file: FileEntity, bundleName: String, productName: String) {
        downloadManager.enqueueDownload(file, bundleName, productName)
    }

    fun cancelDownload(fileId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(fileId)
        }
    }

    fun isLargeFile(file: FileEntity): Boolean = downloadManager.isLargeFile(file)

    fun dismissError() {
        _errorMessage.value = null
    }

    private fun extractExtension(filename: String): String? {
        val dot = filename.lastIndexOf('.')
        return if (dot > 0 && dot < filename.length - 1) {
            filename.substring(dot + 1).lowercase()
        } else null
    }
}
