package com.kieronquinn.app.utag.networking.services

import android.content.Context
import com.kieronquinn.app.utag.networking.model.smartthings.GetRulesResponse
import com.kieronquinn.app.utag.utils.extensions.smartThingsClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

interface RulesService {

    companion object {
        private const val BASE_URL = "https://client.smartthings.com/"

        fun createService(context: Context, retrofit: Retrofit): RulesService {
            return retrofit.newBuilder()
                .client(smartThingsClient(context))
                .baseUrl(BASE_URL)
                .build()
                .create(RulesService::class.java)
        }
    }

    @GET("rules")
    fun getRules(): Call<GetRulesResponse>

}