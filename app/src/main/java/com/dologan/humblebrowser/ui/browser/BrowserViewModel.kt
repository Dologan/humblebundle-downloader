package com.dologan.humblebrowser.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dologan.humblebrowser.data.db.dao.BundleDao
import com.dologan.humblebrowser.data.db.dao.ExclusionRuleDao
import com.dologan.humblebrowser.data.db.dao.FileDao
import com.dologan.humblebrowser.data.db.dao.HiddenPathDao
import com.dologan.humblebrowser.data.db.dao.ItemTagDao
import com.dologan.humblebrowser.data.db.dao.ProductDao
import com.dologan.humblebrowser.data.db.entities.BundleEntity
import com.dologan.humblebrowser.data.db.entities.DownloadState
import com.dologan.humblebrowser.data.db.entities.ExclusionRuleEntity
import com.dologan.humblebrowser.data.db.entities.FileEntity
import com.dologan.humblebrowser.data.db.entities.HiddenPathEntity
import com.dologan.humblebrowser.data.db.entities.ItemTagEntity
import com.dologan.humblebrowser.data.db.entities.ProductEntity
import com.dologan.humblebrowser.data.prefs.AuthPreferences
import com.dologan.humblebrowser.data.repository.LibraryRepository
import com.dologan.humblebrowser.download.DownloadManager
import com.dologan.humblebrowser.domain.model.TreeNode
import com.dologan.humblebrowser.domain.model.ViewMode
import com.dologan.humblebrowser.util.ExclusionMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryStats(
    val totalBundles: Int = 0,
    val totalFiles: Int = 0,
    val totalLibrarySize: Long = 0L,
    val downloadedFiles: Int = 0,
    val downloadedSize: Long = 0L,
)

data class BrowserUiState(
    val tree: List<TreeNode> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val searchQuery: String = "",
    val viewMode: ViewMode = ViewMode.BY_BUNDLE,
    val activePlatformFilters: Set<String> = emptySet(),
    val activeExtensionFilters: Set<String> = emptySet(),
    val activeTagFilters: Set<String> = emptySet(),
    val showHidden: Boolean = false,
    val autoHideEmpty: Boolean = false,
    val downloadedOnly: Boolean = false,
    val hasLibraryData: Boolean = false,
    val availablePlatforms: List<String> = emptyList(),
    val availableExtensions: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val libraryMaxFileSize: Long = 0L,
    val sizeFilterMin: Long = 0L,
    val sizeFilterMax: Long = Long.MAX_VALUE,
    val errorMessage: String? = null,
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val authPreferences: AuthPreferences,
    private val bundleDao: BundleDao,
    private val productDao: ProductDao,
    private val fileDao: FileDao,
    private val hiddenPathDao: HiddenPathDao,
    private val exclusionRuleDao: ExclusionRuleDao,
    private val itemTagDao: ItemTagDao,
    val downloadManager: DownloadManager,
) : ViewModel() {

    private val _expandedNodes = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _viewMode = MutableStateFlow(ViewMode.BY_BUNDLE)
    private val _platformFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _extensionFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _tagFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _showHidden = MutableStateFlow(false)
    private val _autoHideEmpty = MutableStateFlow(true)
    private val _downloadedOnly = MutableStateFlow(false)
    private val _sizeFilterMin = MutableStateFlow(0L)
    private val _sizeFilterMax = MutableStateFlow(Long.MAX_VALUE)
    private val _isSyncing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // DB combine block: core library + user config + tags
    private val dbStateFlow = combine(
        combine(bundleDao.observeAll(), productDao.observeAll(), fileDao.observeAll()) { b, p, f ->
            Triple(b, p, f)
        },
        combine(hiddenPathDao.observeAll(), exclusionRuleDao.observeEnabled()) { h, e -> h to e },
        itemTagDao.observeAll(),
    ) { (bundles, products, files), (hiddenPaths, exclusionRules), itemTags ->
        buildDbState(bundles, products, files, hiddenPaths.map { it.path }.toSet(), exclusionRules, itemTags)
    }

    // Filter toggles: extension + boolean flags + tag filters + size range
    private val filterStateFlow = combine(
        _expandedNodes,
        _searchQuery,
        _viewMode,
        _platformFilters,
        combine(
            combine(_extensionFilters, _showHidden, _autoHideEmpty, _downloadedOnly, _tagFilters) { ext, show, autoHide, dlOnly, tags ->
                FilterTogglesPartial(ext, show, autoHide, dlOnly, tags)
            },
            combine(_sizeFilterMin, _sizeFilterMax) { minSize, maxSize -> minSize to maxSize },
        ) { partial, (minSize, maxSize) ->
            FilterToggles(partial.extensions, partial.showHidden, partial.autoHideEmpty, partial.downloadedOnly, partial.tags, minSize, maxSize)
        },
    ) { expanded, search, viewMode, platforms, toggles ->
        FilterState(expanded, search, viewMode, platforms, toggles)
    }

    val uiState: StateFlow<BrowserUiState> = combine(
        dbStateFlow,
        filterStateFlow,
    ) { dbState, filterState ->
        dbState.copy(
            searchQuery = filterState.search,
            viewMode = filterState.viewMode,
            activePlatformFilters = filterState.platforms,
            activeExtensionFilters = filterState.toggles.extensions,
            activeTagFilters = filterState.toggles.tags,
            showHidden = filterState.toggles.showHidden,
            autoHideEmpty = filterState.toggles.autoHideEmpty,
            downloadedOnly = filterState.toggles.downloadedOnly,
            sizeFilterMin = filterState.toggles.sizeMin,
            sizeFilterMax = filterState.toggles.sizeMax,
        ).let { state -> state.copy(tree = buildTree(state, filterState)) }
    }.combine(
        combine(_isSyncing, _errorMessage) { syncing, error -> syncing to error }
    ) { state, (syncing, error) ->
        state.copy(isSyncing = syncing, errorMessage = error)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BrowserUiState(isLoading = true),
    )

    val libraryStats: StateFlow<LibraryStats> = combine(
        bundleDao.observeAll(),
        fileDao.observeAll(),
    ) { bundles, files ->
        LibraryStats(
            totalBundles = bundles.size,
            totalFiles = files.size,
            totalLibrarySize = files.sumOf { it.fileSize },
            downloadedFiles = files.count { it.downloadState == DownloadState.COMPLETE },
            downloadedSize = files.filter { it.downloadState == DownloadState.COMPLETE }.sumOf { it.fileSize },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryStats())

    private data class FilterTogglesPartial(
        val extensions: Set<String>,
        val showHidden: Boolean,
        val autoHideEmpty: Boolean,
        val downloadedOnly: Boolean,
        val tags: Set<String>,
    )

    private data class FilterToggles(
        val extensions: Set<String>,
        val showHidden: Boolean,
        val autoHideEmpty: Boolean,
        val downloadedOnly: Boolean,
        val tags: Set<String>,
        val sizeMin: Long,
        val sizeMax: Long,
    )

    private data class FilterState(
        val expanded: Set<String>,
        val search: String,
        val viewMode: ViewMode,
        val platforms: Set<String>,
        val toggles: FilterToggles,
    )

    // Cached data references for tree building
    private var cachedBundles: List<BundleEntity> = emptyList()
    private var cachedProducts: List<ProductEntity> = emptyList()
    private var cachedFiles: List<FileEntity> = emptyList()
    private var cachedHiddenPaths: Set<String> = emptySet()
    private var cachedExclusionMatcher: ExclusionMatcher = ExclusionMatcher(emptyList())
    // path -> set of tags on that path
    private var cachedTagMap: Map<String, Set<String>> = emptyMap()

    private fun buildDbState(
        bundles: List<BundleEntity>,
        products: List<ProductEntity>,
        files: List<FileEntity>,
        hiddenPaths: Set<String>,
        exclusionRules: List<ExclusionRuleEntity>,
        itemTags: List<ItemTagEntity>,
    ): BrowserUiState {
        cachedBundles = bundles
        cachedProducts = products
        cachedFiles = files
        cachedHiddenPaths = hiddenPaths
        cachedExclusionMatcher = ExclusionMatcher(exclusionRules)
        cachedTagMap = itemTags.groupBy({ it.path }, { it.tag })
            .mapValues { (_, tags) -> tags.toSet() }

        val allTags = itemTags.map { it.tag }.distinct().sortedWith(
            compareBy { if (it == FAVES_TAG) "" else it }
        )

        // Prune active tag filters that no longer exist in the library
        val allTagSet = allTags.toSet()
        val staleFilters = _tagFilters.value - allTagSet
        if (staleFilters.isNotEmpty()) {
            _tagFilters.value = _tagFilters.value - staleFilters
        }

        return BrowserUiState(
            hasLibraryData = bundles.isNotEmpty(),
            availablePlatforms = files.map { it.platform }.distinct().sorted(),
            availableExtensions = files.mapNotNull { extractExtension(it.filename) }.distinct().sorted(),
            availableTags = allTags,
            libraryMaxFileSize = files.maxOfOrNull { it.fileSize } ?: 0L,
        )
    }

    private fun buildTree(state: BrowserUiState, filterState: FilterState): List<TreeNode> {
        val searchActive = filterState.search.isNotBlank()
        val t = filterState.toggles
        val hasActiveFilters = searchActive || t.downloadedOnly || t.tags.isNotEmpty() ||
            t.sizeMin > 0L || t.sizeMax < Long.MAX_VALUE

        val filteredFiles = cachedFiles.filter { file ->
            val platformOk = filterState.platforms.isEmpty() || file.platform in filterState.platforms
            val extOk = t.extensions.isEmpty() || extractExtension(file.filename) in t.extensions
            val searchOk = !searchActive || file.filename.contains(filterState.search, ignoreCase = true)
            val downloadedOk = !t.downloadedOnly || file.downloadState == DownloadState.COMPLETE
            val sizeOk = file.fileSize in t.sizeMin..t.sizeMax
            val tagOk = t.tags.isEmpty() || fileMatchesTags(file, t.tags)
            platformOk && extOk && searchOk && downloadedOk && sizeOk && tagOk
        }

        val productFileMap = filteredFiles.groupBy { it.productId }
        val productMap = cachedProducts.associateBy { it.id }
        val bundleMap = cachedBundles.associateBy { it.orderId }

        return when (filterState.viewMode) {
            ViewMode.BY_BUNDLE -> buildByBundleTree(bundleMap, productMap, productFileMap, filterState.expanded, filterState.toggles, hasActiveFilters)
            ViewMode.BY_TYPE -> buildByTypeTree(bundleMap, productMap, productFileMap, filteredFiles, filterState.expanded)
            ViewMode.ALPHABETICAL -> buildAlphabeticalTree(bundleMap, productMap, productFileMap, filterState.expanded, filterState.toggles, ascending = true)
            ViewMode.ALPHABETICAL_DESC -> buildAlphabeticalTree(bundleMap, productMap, productFileMap, filterState.expanded, filterState.toggles, ascending = false)
        }
    }

    /** Check if a file (or its parents) has all required tags. */
    private fun fileMatchesTags(file: FileEntity, requiredTags: Set<String>): Boolean {
        val product = cachedProducts.find { it.id == file.productId } ?: return false
        val bundle = cachedBundles.find { it.orderId == product.orderId } ?: return false
        val filePath = "${bundle.bundleName}/${product.humanName}/${file.filename}"
        val productPath = "${bundle.bundleName}/${product.humanName}"
        val bundlePath = bundle.bundleName

        val allTagsForItem = (cachedTagMap[filePath] ?: emptySet()) +
            (cachedTagMap[productPath] ?: emptySet()) +
            (cachedTagMap[bundlePath] ?: emptySet())
        return requiredTags.any { it in allTagsForItem }
    }

    private fun buildByBundleTree(
        bundleMap: Map<String, BundleEntity>,
        productMap: Map<String, ProductEntity>,
        productFileMap: Map<String, List<FileEntity>>,
        expanded: Set<String>,
        toggles: FilterToggles,
        hasActiveFilters: Boolean,
    ): List<TreeNode> {
        val nodes = mutableListOf<TreeNode>()
        val bundleProducts = cachedProducts.groupBy { it.orderId }

        for (bundle in cachedBundles) {
            val bundlePath = bundle.bundleName
            val isHidden = bundlePath in cachedHiddenPaths
            if (isHidden && !toggles.showHidden) continue
            if (cachedExclusionMatcher.isExcluded(bundlePath)) continue

            val products = bundleProducts[bundle.orderId] ?: emptyList()
            val visibleProducts = products.filter { p ->
                val pPath = "$bundlePath/${p.humanName}"
                val pHidden = pPath in cachedHiddenPaths
                if (pHidden && !toggles.showHidden) return@filter false
                if (cachedExclusionMatcher.isExcluded(pPath)) return@filter false
                if (productFileMap[p.id]?.isEmpty() != false) {
                    if (toggles.autoHideEmpty || hasActiveFilters) return@filter false
                }
                true
            }

            if (visibleProducts.isEmpty() && (toggles.autoHideEmpty || hasActiveFilters)) continue

            nodes.add(
                TreeNode.GroupNode(
                    key = bundle.orderId,
                    depth = 0,
                    virtualPath = bundlePath,
                    displayName = bundle.bundleName,
                    expanded = bundle.orderId in expanded,
                    childCount = visibleProducts.size,
                    isHidden = isHidden,
                    tags = cachedTagMap[bundlePath] ?: emptySet(),
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
                            tags = cachedTagMap[productPath] ?: emptySet(),
                        )
                    )

                    if (product.id in expanded) {
                        for (file in visibleFiles) {
                            val filePath = "$productPath/${file.filename}"
                            nodes.add(
                                TreeNode.FileNode(
                                    file = file,
                                    bundleName = bundle.bundleName,
                                    productName = product.humanName,
                                    tags = cachedTagMap[filePath] ?: emptySet(),
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
        toggles: FilterToggles,
        ascending: Boolean,
    ): List<TreeNode> {
        val nodes = mutableListOf<TreeNode>()
        val sorted = cachedProducts
            .filter { productFileMap.containsKey(it.id) }
            .let { list ->
                if (ascending) list.sortedBy { it.humanName.lowercase() }
                else list.sortedByDescending { it.humanName.lowercase() }
            }

        for (product in sorted) {
            val bundle = bundleMap[product.orderId]
            val bundleName = bundle?.bundleName ?: "Unknown"
            val productPath = "$bundleName/${product.humanName}"
            val isHidden = productPath in cachedHiddenPaths
            if (isHidden && !toggles.showHidden) continue

            val files = productFileMap[product.id] ?: emptyList()

            nodes.add(
                TreeNode.ProductNode(
                    product = product,
                    bundleName = bundleName,
                    expanded = product.id in expanded,
                    childCount = files.size,
                    isHidden = isHidden,
                    tags = cachedTagMap[productPath] ?: emptySet(),
                )
            )

            if (product.id in expanded) {
                for (file in files) {
                    val filePath = "$productPath/${file.filename}"
                    nodes.add(
                        TreeNode.FileNode(
                            file = file,
                            bundleName = bundleName,
                            productName = product.humanName,
                            tags = cachedTagMap[filePath] ?: emptySet(),
                        )
                    )
                }
            }
        }
        return nodes
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    fun toggleExpanded(key: String) {
        _expandedNodes.value = _expandedNodes.value.let {
            if (key in it) it - key else it + key
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        _expandedNodes.value = emptySet()
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

    fun toggleTagFilter(tag: String) {
        _tagFilters.value = _tagFilters.value.let {
            if (tag in it) it - tag else it + tag
        }
    }

    fun addTagToPath(path: String, tag: String) {
        viewModelScope.launch {
            itemTagDao.addTag(ItemTagEntity(path = path, tag = tag.trim()))
        }
    }

    fun removeTagFromPath(path: String, tag: String) {
        viewModelScope.launch {
            itemTagDao.removeTag(path, tag)
        }
    }

    fun toggleShowHidden() { _showHidden.value = !_showHidden.value }
    fun toggleAutoHideEmpty() { _autoHideEmpty.value = !_autoHideEmpty.value }
    fun toggleDownloadedOnly() { _downloadedOnly.value = !_downloadedOnly.value }

    fun resetAllFilters() {
        _searchQuery.value = ""
        _platformFilters.value = emptySet()
        _extensionFilters.value = emptySet()
        _tagFilters.value = emptySet()
        _downloadedOnly.value = false
        _sizeFilterMin.value = 0L
        _sizeFilterMax.value = Long.MAX_VALUE
    }

    fun setSizeFilter(min: Long, max: Long) {
        _sizeFilterMin.value = min
        _sizeFilterMax.value = max
    }

    fun resetSizeFilter() {
        _sizeFilterMin.value = 0L
        _sizeFilterMax.value = Long.MAX_VALUE
    }

    fun hidePath(path: String) {
        viewModelScope.launch { hiddenPathDao.hide(HiddenPathEntity(path = path)) }
    }

    fun unhidePath(path: String) {
        viewModelScope.launch { hiddenPathDao.unhide(path) }
    }

    fun sync(forceRefresh: Boolean = false) {
        if (!authPreferences.isLoggedIn()) return
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            libraryRepository.syncLibrary(forceRefresh).onFailure { e ->
                _errorMessage.value = e.message ?: "Sync failed"
            }
            _isSyncing.value = false
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            cachedFiles.filter { it.downloadState == DownloadState.COMPLETE }.forEach { file ->
                file.localPath?.let { downloadManager.deleteDownload(file.id, it) }
            }
        }
    }

    init {
        viewModelScope.launch {
            downloadManager.downloadErrors.collect { error ->
                _errorMessage.value = error
            }
        }
    }

    fun downloadFile(file: FileEntity, bundleName: String, productName: String) {
        viewModelScope.launch {
            try {
                val workId = downloadManager.enqueueDownload(file, bundleName, productName)
                downloadManager.observeDownload(viewModelScope, workId, file.filename)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start download: ${e.message}"
            }
        }
    }

    fun downloadFileTo(file: FileEntity, destUri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val workId = downloadManager.enqueueDownloadToUri(file, destUri)
                downloadManager.observeDownload(viewModelScope, workId, file.filename)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start download: ${e.message}"
            }
        }
    }

    fun cancelDownload(fileId: String) {
        viewModelScope.launch { downloadManager.cancelDownload(fileId) }
    }

    fun deleteDownload(fileId: String, localPath: String) {
        viewModelScope.launch { downloadManager.deleteDownload(fileId, localPath) }
    }

    fun isLargeFile(file: FileEntity): Boolean = downloadManager.isLargeFile(file)

    fun observeTagsForPath(path: String): kotlinx.coroutines.flow.Flow<Set<String>> =
        itemTagDao.observeTagsForPath(path).map { it.toSet() }

    fun dismissError() { _errorMessage.value = null }

    private fun extractExtension(filename: String): String? {
        val dot = filename.lastIndexOf('.')
        return if (dot > 0 && dot < filename.length - 1) filename.substring(dot + 1).lowercase() else null
    }

    companion object {
        const val FAVES_TAG = "Faves"
    }
}
