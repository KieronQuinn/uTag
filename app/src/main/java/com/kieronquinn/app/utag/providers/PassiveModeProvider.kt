package com.kieronquinn.app.utag.providers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.repositories.PassiveModeRepository
import com.kieronquinn.app.utag.utils.extensions.observerAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.koin.android.ext.android.inject

class PassiveModeProvider: BaseProvider() {

    companion object {
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.passive"
        private const val KEY_RESULT = "result"
        private const val EXTRA_IGNORE_BYPASS = "ignore_bypass"
        private const val EXTRA_BYPASS_ONLY = "bypass_only"
        
        private val URI = Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .build()

        fun isInPassiveMode(
            context: Context,
            deviceId: String,
            ignoreBypass: Boolean = false,
            bypassOnly: Boolean = false
        ): Boolean {
            return context.contentResolver.getBoolean(
                Method.IS_IN_PASSIVE_MODE,
                deviceId,
                bundleOf(EXTRA_IGNORE_BYPASS to ignoreBypass, EXTRA_BYPASS_ONLY to bypassOnly)
            )
        }
        
        fun notifyChange(context: Context, deviceIdHash: String, enabled: Boolean) {
            val uri = URI.buildUpon().appendPath(deviceIdHash).appendPath(enabled.toString()).build()
            context.contentResolver.notifyChange(uri, null)
        }

        fun onChange(context: Context): Flow<Pair<Int, Boolean>> {
            return context.contentResolver.observerAsFlow(URI).mapNotNull {
                val pathSegments = it?.pathSegments ?: return@mapNotNull null
                val deviceIdHash = pathSegments.firstOrNull()?.toIntOrNull()
                    ?: return@mapNotNull null
                val enabled = pathSegments.lastOrNull()?.toBooleanStrictOrNull()
                    ?: return@mapNotNull null
                Pair(deviceIdHash, enabled)
            }
        }

        private fun ContentResolver.getBoolean(
            method: Method, 
            arg: String? = null, 
            extras: Bundle? = null
        ): Boolean {
            return call(AUTHORITY, method.name, arg, extras)?.getBoolean(KEY_RESULT)
                ?: false
        }

        private enum class Method {
            IS_IN_PASSIVE_MODE;

            companion object {
                fun get(method: String): Method? {
                    return entries.firstOrNull { it.name == method }
                }
            }
        }
    }

    private val passiveModeRepository by inject<PassiveModeRepository>()

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when(Method.get(method)) {
            Method.IS_IN_PASSIVE_MODE -> {
                val deviceId = arg ?: return null
                val ignoreBypass = extras?.getBoolean(EXTRA_IGNORE_BYPASS, false)
                    ?: return null
                val bypassOnly = extras.getBoolean(EXTRA_BYPASS_ONLY, false)
                val result = passiveModeRepository
                    .isInPassiveMode(deviceId, ignoreBypass, bypassOnly)
                bundleOf(KEY_RESULT to result)
            }
            null -> null
        }
    }

}