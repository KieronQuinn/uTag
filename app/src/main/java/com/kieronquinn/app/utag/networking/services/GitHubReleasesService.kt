package com.kieronquinn.app.utag.networking.services

import android.content.Context
import com.kieronquinn.app.utag.networking.model.github.GitHubRelease
import com.kieronquinn.app.utag.utils.extensions.withCache
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

interface GitHubReleasesService {

    companion object {
        private const val BASE_URL = "https://api.github.com/repos/KieronQuinn/uTag/"

        fun createService(retrofit: Retrofit, context: Context): GitHubReleasesService {
            return retrofit.newBuilder()
                .baseUrl(BASE_URL)
                .withCache(context)
                .build()
                .create(GitHubReleasesService::class.java)
        }
    }

    @GET("releases")
    fun getReleases(): Call<Array<GitHubRelease>>

}