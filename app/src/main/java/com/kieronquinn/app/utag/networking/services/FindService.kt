package com.kieronquinn.app.utag.networking.services

import android.content.Context
import com.kieronquinn.app.utag.networking.model.find.EncryptionKeyRequest
import com.kieronquinn.app.utag.networking.model.find.EncryptionKeyResponse
import com.kieronquinn.app.utag.networking.model.find.FindItemRequest
import com.kieronquinn.app.utag.networking.model.find.FindNullableItemResponse
import com.kieronquinn.app.utag.utils.extensions.findClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Path

interface FindService {

    companion object {
        private const val BASE_URL = "https://api.samsungfind.com/"

        fun createService(context: Context, retrofit: Retrofit): FindService {
            return retrofit.newBuilder()
                .client(findClient(context))
                .baseUrl(BASE_URL)
                .build()
                .create(FindService::class.java)
        }

        private const val HEADER_TAB_NAME = "X-Sec-Tab-Name: "
        private const val TAB_PEOPLE = "${HEADER_TAB_NAME}PEOPLE"
        private const val TAB_DEVICES = "${HEADER_TAB_NAME}DEVICES"
        private const val TAB_ITEMS = "${HEADER_TAB_NAME}ITEMS"
    }

    @GET("/users/{id}/key")
    @Headers(TAB_DEVICES)
    fun getEncryptionKey(@Path("id") userId: String): Call<FindNullableItemResponse<EncryptionKeyResponse>>

    @PUT("/users/{id}/key")
    @Headers(TAB_DEVICES)
    fun setEncryptionKey(@Path("id") userId: String, @Body body: FindItemRequest<EncryptionKeyRequest>): Call<Unit>

}