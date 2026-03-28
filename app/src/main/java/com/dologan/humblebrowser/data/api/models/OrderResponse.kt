package com.dologan.humblebrowser.data.api.models

import com.google.gson.annotations.SerializedName

data class OrderResponse(
    @SerializedName("gamekey") val gameKey: String,
    @SerializedName("product") val product: OrderProduct,
    @SerializedName("subproducts") val subproducts: List<SubProduct>,
    @SerializedName("created") val created: String? = null,
)

data class OrderProduct(
    @SerializedName("human_name") val humanName: String,
    @SerializedName("machine_name") val machineName: String? = null,
)

data class SubProduct(
    @SerializedName("human_name") val humanName: String,
    @SerializedName("machine_name") val machineName: String,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("downloads") val downloads: List<Download> = emptyList(),
)

data class Download(
    @SerializedName("platform") val platform: String,
    @SerializedName("download_struct") val downloadStruct: List<DownloadStruct> = emptyList(),
    @SerializedName("machine_name") val machineName: String? = null,
)

data class DownloadStruct(
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: DownloadUrl? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("md5") val md5: String? = null,
    @SerializedName("uploaded_at") val uploadedAt: String? = null,
    @SerializedName("human_size") val humanSize: String? = null,
)

data class DownloadUrl(
    @SerializedName("web") val web: String? = null,
    @SerializedName("bittorrent") val bittorrent: String? = null,
)
