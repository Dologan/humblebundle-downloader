package com.dologan.humblebrowser.data.api.models

import com.google.gson.annotations.SerializedName

data class TroveProduct(
    @SerializedName("human-name") val humanName: String,
    @SerializedName("machine_name") val machineName: String,
    @SerializedName("downloads") val downloads: Map<String, TroveDownload>? = null,
    @SerializedName("icon-path") val iconPath: String? = null,
)

data class TroveDownload(
    @SerializedName("url") val url: TroveDownloadUrl? = null,
    @SerializedName("machine_name") val machineName: String? = null,
    @SerializedName("md5") val md5: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("uploaded_at") val uploadedAt: String? = null,
    @SerializedName("timestamp") val timestamp: Long? = null,
)

data class TroveDownloadUrl(
    @SerializedName("web") val web: String? = null,
)

data class SignResponse(
    @SerializedName("signed_url") val signedUrl: String,
)
