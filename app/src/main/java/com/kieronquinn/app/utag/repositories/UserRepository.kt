package com.kieronquinn.app.utag.repositories

import android.content.Context
import com.kieronquinn.app.utag.model.database.cache.CacheItem.CacheType
import com.kieronquinn.app.utag.networking.model.smartthings.UserInfoResponse
import com.kieronquinn.app.utag.networking.services.UserService
import com.kieronquinn.app.utag.utils.extensions.get
import retrofit2.Retrofit

interface UserRepository {

    suspend fun getUserInfo(): UserInfoResponse?

}

class UserRepositoryImpl(context: Context, retrofit: Retrofit): UserRepository {

    private val userService = UserService.createService(context, retrofit)

    override suspend fun getUserInfo(): UserInfoResponse? {
        return userService.getUserInfo().get(CacheType.USER_INFO, name = "userInfo")
    }

}