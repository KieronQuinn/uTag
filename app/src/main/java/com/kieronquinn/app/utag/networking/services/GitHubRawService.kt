package com.kieronquinn.app.utag.networking.services

import android.content.Context
import com.kieronquinn.app.utag.networking.model.github.ModRelease
import com.kieronquinn.app.utag.utils.extensions.withCache
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

interface GitHubRawService {

    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/KieronQuinn/uTag/refs/heads/main/"

        fun createService(retrofit: Retrofit, context: Context): GitHubRawService {
            return retrofit.newBuilder()
                .baseUrl(BASE_URL)
                .withCache(context)
                .build()
                .create(GitHubRawService::class.java)
        }
    }

    @GET("SmartThings.json")
    fun getRelease(): Call<ModRelease>

}