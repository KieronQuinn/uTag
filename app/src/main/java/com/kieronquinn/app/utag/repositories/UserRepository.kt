package com.kieronquinn.app.utag.repositories

import android.content.Context
import com.kieronquinn.app.utag.model.database.cache.CacheItem.CacheType
import com.kieronquinn.app.utag.networking.model.smartthings.UserInfoResponse
import com.kieronquinn.app.utag.networking.services.UserService
import com.kieronquinn.app.utag.utils.extensions.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit

interface UserRepository {

    suspend fun getUserInfo(): UserInfoResponse?

}

class UserRepositoryImpl(
    private val settings: EncryptedSettingsRepository,
    context: Context,
    retrofit: Retrofit
): UserRepository {

    private val userService = UserService.createService(context, retrofit)

    private val userInfoLock = Mutex()
    private var cachedUserInfo: UserInfoResponse? = null

    override suspend fun getUserInfo(): UserInfoResponse? {
        return userInfoLock.withLock {
            cachedUserInfo?.let { return it }
            userService.getUserInfo().get(CacheType.USER_INFO, name = "userInfo")?.also {
                cachedUserInfo = it
                settings.authCountryCode.set(it.countryCode)
            }
        }
    }

}