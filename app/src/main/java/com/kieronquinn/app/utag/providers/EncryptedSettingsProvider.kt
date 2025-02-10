package com.kieronquinn.app.utag.providers

import android.content.Context
import android.content.SharedPreferences
import com.kieronquinn.app.utag.utils.extensions.createEncryptedSharedPrefDestructively
import com.kieronquinn.app.utag.utils.extensions.wrapAsApplicationContext
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper.RoomEncryptionFailedCallback
import com.kieronquinn.app.utag.xposed.extensions.provideContext
import org.koin.android.ext.android.inject

class EncryptedSettingsProvider: SharedPreferencesProvider(), RoomEncryptionFailedCallback {

    companion object {
        private const val SHARED_PREFS_NAME = "encrypted_shared_prefs"

        @Synchronized
        private fun getSharedPreferences(
            context: Context,
            failedCallback: RoomEncryptionFailedCallback
        ): SharedPreferences {
            return context.wrapAsApplicationContext().createEncryptedSharedPrefDestructively(
                "${context.packageName}_$SHARED_PREFS_NAME"
            ) {
                failedCallback.onEncryptionFailed()
            }
        }
    }

    private val roomEncryptionHelper by inject<RoomEncryptionHelper>()

    override val sharedPreferences by lazy {
        getSharedPreferences(provideContext(), this)
    }

    override fun onEncryptionFailed() {
        roomEncryptionHelper.onEncryptionFailed()
    }

}