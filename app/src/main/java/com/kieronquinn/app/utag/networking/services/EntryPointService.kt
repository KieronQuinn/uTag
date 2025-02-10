package com.kieronquinn.app.utag.networking.services

import com.kieronquinn.app.utag.networking.model.smartthings.EntryPointResponse
import com.kieronquinn.app.utag.utils.extensions.ospClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

interface EntryPointService {

    companion object {
        private const val BASE_URL = "https://account.samsung.com/accounts/ANDROIDSDK/"

        fun createService(retrofit: Retrofit): EntryPointService {
            return retrofit.newBuilder()
                .client(ospClient())
                .baseUrl(BASE_URL)
                .build()
                .create(EntryPointService::class.java)
        }
    }

    @GET("getEntryPoint")
    fun getEntryPoint(): Call<EntryPointResponse>

}