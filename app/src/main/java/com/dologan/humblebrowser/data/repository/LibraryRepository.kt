package com.dologan.humblebrowser.data.repository

import com.dologan.humblebrowser.data.api.HumbleBundleApi
import com.dologan.humblebrowser.data.db.dao.BundleDao
import com.dologan.humblebrowser.data.db.dao.FileDao
import com.dologan.humblebrowser.data.db.dao.ProductDao
import com.dologan.humblebrowser.data.db.entities.BundleEntity
import com.dologan.humblebrowser.data.db.entities.FileEntity
import com.dologan.humblebrowser.data.db.entities.ProductEntity
import com.dologan.humblebrowser.util.FileNameCleaner
import org.jsoup.Jsoup
import com.google.gson.Gson
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val api: HumbleBundleApi,
    private val bundleDao: BundleDao,
    private val productDao: ProductDao,
    private val fileDao: FileDao,
) {
    suspend fun syncLibrary(forceRefresh: Boolean = false): Result<Int> {
        return try {
            val keys = fetchPurchaseKeys()
            var syncedCount = 0

            for (key in keys) {
                val existing = bundleDao.getById(key)
                if (!forceRefresh && existing != null && existing.lastSyncedAt > 0) {
                    continue
                }
                try {
                    syncOrder(key)
                    syncedCount++
                } catch (e: Exception) {
                    // Log and continue with other orders
                    e.printStackTrace()
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncOrder(orderId: String) {
        val order = api.getOrder(orderId)
        val bundleName = FileNameCleaner.clean(order.product.humanName)

        bundleDao.upsert(
            BundleEntity(
                orderId = orderId,
                bundleName = bundleName,
                lastSyncedAt = System.currentTimeMillis(),
            )
        )

        val products = mutableListOf<ProductEntity>()
        val files = mutableListOf<FileEntity>()

        for (sub in order.subproducts) {
            val productId = "$orderId:${sub.machineName}"
            val productName = FileNameCleaner.clean(sub.humanName)

            products.add(
                ProductEntity(
                    id = productId,
                    orderId = orderId,
                    humanName = productName,
                    machineName = sub.machineName,
                    iconUrl = sub.icon,
                )
            )

            for (download in sub.downloads) {
                for (struct in download.downloadStruct) {
                    val url = struct.url?.web ?: continue
                    val filename = extractFilename(url, struct.name)

                    files.add(
                        FileEntity(
                            id = "$productId:$filename",
                            productId = productId,
                            filename = filename,
                            platform = download.platform,
                            fileSize = struct.fileSize ?: 0L,
                            md5 = struct.md5,
                            downloadUrl = url,
                            uploadedAt = struct.uploadedAt?.toLongOrNull(),
                        )
                    )
                }
            }
        }

        productDao.upsertAll(products)
        fileDao.upsertAll(files)
    }

    suspend fun syncTrove(): Result<Int> {
        return try {
            var index = 0
            var totalProducts = 0
            val troveBundleId = "trove"

            bundleDao.upsert(
                BundleEntity(
                    orderId = troveBundleId,
                    bundleName = "Humble Trove",
                    lastSyncedAt = System.currentTimeMillis(),
                )
            )

            while (true) {
                val troveProducts = api.getTroveCatalog(index)
                if (troveProducts.isEmpty()) break

                val products = mutableListOf<ProductEntity>()
                val files = mutableListOf<FileEntity>()

                for (tp in troveProducts) {
                    val productId = "$troveBundleId:${tp.machineName}"
                    val productName = FileNameCleaner.clean(tp.humanName)

                    products.add(
                        ProductEntity(
                            id = productId,
                            orderId = troveBundleId,
                            humanName = productName,
                            machineName = tp.machineName,
                            iconUrl = tp.iconPath,
                        )
                    )

                    tp.downloads?.forEach { (platform, download) ->
                        val urlPath = download.url?.web ?: return@forEach
                        val filename = urlPath.substringAfterLast("/")

                        files.add(
                            FileEntity(
                                id = "$productId:$filename",
                                productId = productId,
                                filename = filename,
                                platform = platform,
                                fileSize = download.fileSize ?: 0L,
                                md5 = download.md5,
                                downloadUrl = urlPath,
                                uploadedAt = download.timestamp,
                            )
                        )
                    }
                }

                productDao.upsertAll(products)
                fileDao.upsertAll(files)
                totalProducts += troveProducts.size
                index += troveProducts.size
            }

            Result.success(totalProducts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchPurchaseKeys(): List<String> {
        val html = api.getLibraryPage()
        val doc = Jsoup.parse(html)
        val jsonElement = doc.selectFirst("#user-home-json-data")
            ?: throw IllegalStateException("Could not find library data in page")

        val jsonText = jsonElement.text()
        val jsonObj = Gson().fromJson(jsonText, JsonObject::class.java)
        val keysArray = jsonObj.getAsJsonArray("gamekeys")
            ?: throw IllegalStateException("No gamekeys found in library data")

        return keysArray.map { it.asString }
    }

    private fun extractFilename(url: String, name: String?): String {
        val fromUrl = url.substringAfterLast("/").substringBefore("?")
        return if (fromUrl.isNotBlank() && fromUrl.contains(".")) {
            fromUrl
        } else {
            name ?: fromUrl
        }
    }
}
