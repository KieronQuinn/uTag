package com.kieronquinn.app.utag.providers

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.repositories.AuthRepository
import org.koin.android.ext.android.inject

/**
 *  Provides access for API calls to various auth and encryption related calls. API tokens and
 *  refresh calls from interceptors run via this provider due to the risk of race conditions and
 *  expired refresh tokens if the UI and service try to refresh at the same time.
 */
class AuthProvider: BaseProvider() {
    
    companion object {
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.auth"
        private const val KEY_RESULT = "result"
        
        fun getUserId(context: Context): String? {
            return context.contentResolver.getString(Method.GET_USER_ID)
        }

        fun getAuthServerUrl(context: Context): String? {
            return context.contentResolver.getString(Method.GET_AUTH_SERVER_URL)
        }
        
        fun getSmartThingsToken(context: Context): String? {
            return context.contentResolver.getString(Method.GET_SMARTTHINGS_TOKEN)
        }
        
        fun refreshSmartThingsToken(context: Context): Boolean {
            return context.contentResolver.getBoolean(Method.REFRESH_SMARTTHINGS_TOKEN)
        }
        
        fun getFindToken(context: Context): String? {
            return context.contentResolver.getString(Method.GET_FIND_TOKEN)
        }
        
        fun refreshFindToken(context: Context): Boolean {
            return context.contentResolver.getBoolean(Method.REFRESH_FIND_TOKEN)
        }
        
        fun clearCredentials(context: Context): Boolean {
            return context.contentResolver.getBoolean(Method.CLEAR_CREDENTIALS)
        }
        
        private fun ContentResolver.getString(method: Method): String? {
            return call(AUTHORITY, method.name, null, null)?.getString(KEY_RESULT)
        }
        
        private fun ContentResolver.getBoolean(method: Method): Boolean {
            return call(AUTHORITY, method.name, null, null)?.getBoolean(KEY_RESULT)
                ?: false
        }
        
        private enum class Method {
            GET_USER_ID,
            GET_AUTH_SERVER_URL,
            GET_SMARTTHINGS_TOKEN,
            REFRESH_SMARTTHINGS_TOKEN,
            GET_FIND_TOKEN,
            REFRESH_FIND_TOKEN,
            CLEAR_CREDENTIALS;

            companion object {
                fun get(method: String): Method? {
                    return entries.firstOrNull { it.name == method }
                }
            }
        }
    }
    
    private val authRepository by inject<AuthRepository>()

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when(Method.get(method)) {
            Method.GET_USER_ID -> {
                bundleOf(KEY_RESULT to authRepository.getUserId())
            }
            Method.GET_AUTH_SERVER_URL -> {
                bundleOf(KEY_RESULT to authRepository.getAuthServerUrl())
            }
            Method.GET_SMARTTHINGS_TOKEN -> {
                bundleOf(KEY_RESULT to authRepository.getSmartThingsAuthToken())
            }
            Method.REFRESH_SMARTTHINGS_TOKEN -> {
                bundleOf(KEY_RESULT to authRepository.refreshSmartThingsToken())
            }
            Method.GET_FIND_TOKEN -> {
                bundleOf(KEY_RESULT to authRepository.getFindAuthToken())
            }
            Method.REFRESH_FIND_TOKEN -> {
                bundleOf(KEY_RESULT to authRepository.refreshFindToken())
            }
            Method.CLEAR_CREDENTIALS -> {
                bundleOf(KEY_RESULT to authRepository.clearCredentials())
            }
            null -> null
        }
    }
    
}