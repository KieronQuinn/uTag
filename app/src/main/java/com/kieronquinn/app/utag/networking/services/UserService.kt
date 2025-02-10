package com.kieronquinn.app.utag.networking.services

import android.content.Context
import com.kieronquinn.app.utag.networking.model.smartthings.UserInfoResponse
import com.kieronquinn.app.utag.utils.extensions.smartThingsClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

interface UserService {

    companion object {
        private const val BASE_URL = "https://auth.api.smartthings.com/"

        fun createService(context: Context, retrofit: Retrofit): UserService {
            return retrofit.newBuilder()
                .client(smartThingsClient(context))
                .baseUrl(BASE_URL)
                .build()
                .create(UserService::class.java)
        }
    }

    @GET("users/me")
    fun getUserInfo(): Call<UserInfoResponse>

}