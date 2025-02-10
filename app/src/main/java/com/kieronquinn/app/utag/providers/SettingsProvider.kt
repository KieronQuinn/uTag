package com.kieronquinn.app.utag.providers

import android.content.Context
import android.content.SharedPreferences
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.xposed.extensions.provideContext

class SettingsProvider: SharedPreferencesProvider() {

    companion object {
        private const val SHARED_PREFS_NAME = "${BuildConfig.APPLICATION_ID}_prefs"
    }

    override val sharedPreferences: SharedPreferences by lazy {
        provideContext().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

}