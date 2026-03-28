package com.dologan.humblebrowser.data.api

import com.dologan.humblebrowser.data.api.models.OrderResponse
import com.dologan.humblebrowser.data.api.models.SignResponse
import com.dologan.humblebrowser.data.api.models.TroveProduct
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface HumbleBundleApi {

    @GET("home/library")
    suspend fun getLibraryPage(): String

    @GET("api/v1/order/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: String,
        @Query("all_tpkds") allTpkds: Boolean = true,
    ): OrderResponse

    @FormUrlEncoded
    @POST("api/v1/user/download/sign")
    suspend fun signDownload(
        @Field("machine_name") machineName: String,
        @Field("filename") filename: String,
    ): SignResponse

    @GET("client/catalog")
    suspend fun getTroveCatalog(
        @Query("index") index: Int,
    ): List<TroveProduct>
}
