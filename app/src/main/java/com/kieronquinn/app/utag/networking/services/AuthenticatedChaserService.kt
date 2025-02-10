package com.kieronquinn.app.utag.networking.services

import android.content.Context
import com.kieronquinn.app.utag.networking.model.chaser.ChaserTagDataRequest
import com.kieronquinn.app.utag.networking.model.chaser.ChaserTagDataResponse
import com.kieronquinn.app.utag.utils.extensions.smartThingsClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthenticatedChaserService {

    companion object {
        private const val BASE_URL = "https://client.smartthings.com/chaser/"

        fun createService(context: Context, retrofit: Retrofit): AuthenticatedChaserService {
            return retrofit.newBuilder()
                .client(smartThingsClient(context))
                .baseUrl(BASE_URL)
                .build()
                .create(AuthenticatedChaserService::class.java)
        }
    }

    @POST("images/vendor/retrieves/global?ostype=android")
    fun getTagData(@Body body: ChaserTagDataRequest): Call<ChaserTagDataResponse>

}