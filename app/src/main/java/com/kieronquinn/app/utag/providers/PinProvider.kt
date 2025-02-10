package com.kieronquinn.app.utag.providers

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.repositories.EncryptionRepository
import org.koin.android.ext.android.inject

/**
 *  This provider runs in the `:service` process to keep the PIN in memory while the app is closed.
 *  All UI accesses run through here.
 */
class PinProvider: BaseProvider() {

    companion object {
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.pin"
        private const val KEY_RESULT = "result"
        private const val EXTRA_SAVE_PIN = "save_pin"

        fun getPin(context: Context): String? {
            return context.contentResolver.getString(Method.GET_PIN)
        }

        fun clearPin(context: Context) {
            context.contentResolver.getBoolean(Method.CLEAR_PIN)
        }

        fun hasPin(context: Context): Boolean {
            return context.contentResolver.getBoolean(Method.HAS_PIN)
        }

        fun setPin(context: Context, pin: String, savePin: Boolean) {
            context.contentResolver
                .setString(Method.SET_PIN, pin, bundleOf(EXTRA_SAVE_PIN to savePin))
        }

        private fun ContentResolver.getString(method: Method): String? {
            return call(AUTHORITY, method.name, null, null)?.getString(KEY_RESULT)
        }

        private fun ContentResolver.setString(method: Method, pin: String, extras: Bundle?) {
            call(AUTHORITY, method.name, pin, extras)
        }

        private fun ContentResolver.getBoolean(method: Method): Boolean {
            return call(AUTHORITY, method.name, null, null)?.getBoolean(KEY_RESULT)
                ?: false
        }

        private enum class Method {
            HAS_PIN,
            GET_PIN,
            SET_PIN,
            CLEAR_PIN;

            companion object {
                fun get(method: String): Method? {
                    return entries.firstOrNull { it.name == method }
                }
            }
        }
    }

    private val encryptionRepository by inject<EncryptionRepository>()

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when(Method.get(method)) {
            Method.HAS_PIN -> {
                bundleOf(KEY_RESULT to encryptionRepository.hasPin())
            }
            Method.GET_PIN -> {
                bundleOf(KEY_RESULT to encryptionRepository.getPin())
            }
            Method.SET_PIN -> {
                val pin = arg ?: return null
                val savePin = extras?.getBoolean(EXTRA_SAVE_PIN, false) ?: false
                encryptionRepository.setPIN(pin, savePin)
                bundleOf(KEY_RESULT to true)
            }
            Method.CLEAR_PIN -> {
                encryptionRepository.clearPin()
                bundleOf(KEY_RESULT to true)
            }
            null -> null
        }
    }

}