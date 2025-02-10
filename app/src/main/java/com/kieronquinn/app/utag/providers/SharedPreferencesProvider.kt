package com.kieronquinn.app.utag.providers

import android.content.ComponentName
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.gson.Gson
import com.kieronquinn.app.utag.utils.extensions.getTyped
import com.kieronquinn.app.utag.utils.extensions.javaPrimitiveTypeOrClass
import com.kieronquinn.app.utag.xposed.extensions.provideContext
import org.koin.android.ext.android.inject

abstract class SharedPreferencesProvider: BaseProvider() {

    companion object {
        const val KEY_KEY = "key"
        const val KEY_VALUE = "value"
        const val KEY_DEFAULT = "default"
        const val KEY_RESULT = "result"

        enum class Method {
            CONTAINS,
            GET_ALL,
            GET_BOOLEAN,
            GET_FLOAT,
            GET_INT,
            GET_LONG,
            GET_STRING,
            GET_STRING_SET,
            PUT_BOOLEAN,
            PUT_FLOAT,
            PUT_INT,
            PUT_LONG,
            PUT_STRING,
            PUT_STRING_SET,
            REMOVE,
            CLEAR,
            ON_ENCRYPTION_FAILED;

            companion object {
                fun get(method: String): Method? {
                    return entries.firstOrNull { it.name == method }
                }
            }
        }
    }

    abstract val sharedPreferences: SharedPreferences

    private val gson by inject<Gson>()

    private val authority by lazy {
        provideContext().packageManager
            .getProviderInfo(ComponentName(provideContext(), this::class.java), 0).authority
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when(Method.get(method)) {
            Method.CONTAINS -> {
                val key = extras?.key() ?: return null
                bundleOf(KEY_RESULT to sharedPreferences.contains(key))
            }
            Method.GET_ALL -> {
                bundleOf(KEY_RESULT to gson.toJson(sharedPreferences.all))
            }
            Method.GET_BOOLEAN -> {
                bundleOf(KEY_RESULT to sharedPreferences.get<Boolean>(extras))
            }
            Method.GET_FLOAT -> {
                bundleOf(KEY_RESULT to sharedPreferences.get<Float>(extras))
            }
            Method.GET_INT -> {
                bundleOf(KEY_RESULT to sharedPreferences.get<Int>(extras))
            }
            Method.GET_LONG -> {
                bundleOf(KEY_RESULT to sharedPreferences.get<Long>(extras))
            }
            Method.GET_STRING -> {
                bundleOf(KEY_RESULT to sharedPreferences.get<String>(extras))
            }
            Method.GET_STRING_SET -> {
                bundleOf(
                    KEY_RESULT to sharedPreferences.get<Set<String>>(extras)?.toTypedArray()
                )
            }
            Method.PUT_BOOLEAN -> {
                bundleOf(KEY_RESULT to sharedPreferences.put<Boolean>(extras))
            }
            Method.PUT_FLOAT -> {
                bundleOf(KEY_RESULT to sharedPreferences.put<Float>(extras))
            }
            Method.PUT_INT -> {
                bundleOf(KEY_RESULT to sharedPreferences.put<Int>(extras))
            }
            Method.PUT_LONG -> {
                bundleOf(KEY_RESULT to sharedPreferences.put<Long>(extras))
            }
            Method.PUT_STRING -> {
                bundleOf(KEY_RESULT to sharedPreferences.put<String>(extras))
            }
            Method.PUT_STRING_SET -> {
                bundleOf(KEY_RESULT to sharedPreferences.put<Set<String>>(extras))
            }
            Method.REMOVE -> {
                val key = extras?.key() ?: return null
                bundleOf(KEY_RESULT to sharedPreferences.edit().remove(key).commit()).also {
                    notifyChange(key)
                }
            }
            Method.CLEAR -> {
                bundleOf(KEY_RESULT to sharedPreferences.edit().clear().commit()).also {
                    notifyChange(null)
                }
            }
            Method.ON_ENCRYPTION_FAILED -> {
                onEncryptionFailed()
                bundleOf(KEY_RESULT to true)
            }
            else -> null
        }
    }

    open fun onEncryptionFailed() {
        //No-op by default
    }

    private fun Bundle.key(): String? {
        return getString(KEY_KEY)
    }

    private inline fun <reified T : Any> SharedPreferences.get(extras: Bundle?): T? {
        val key = extras?.key() ?: return null
        val default = extras.getTyped<T>(KEY_DEFAULT)
        return when(T::class.javaPrimitiveTypeOrClass) {
            Boolean::class.javaPrimitiveTypeOrClass -> getBoolean(key, default as Boolean) as? T
            Float::class.javaPrimitiveTypeOrClass -> getFloat(key, default as Float) as? T
            Int::class.javaPrimitiveTypeOrClass -> getInt(key, default as Int) as? T
            Long::class.javaPrimitiveTypeOrClass -> getLong(key, default as Long) as? T
            String::class.javaPrimitiveTypeOrClass -> getString(key, default as? String) as? T
            Set::class.javaPrimitiveTypeOrClass -> getStringSet(key, default as Set<String>) as? T
            else -> return null
        }
    }

    private inline fun <reified T : Any> SharedPreferences.put(extras: Bundle?): Boolean {
        val key = extras?.key() ?: return false
        val value = extras.getTyped<T>(KEY_VALUE) ?: return false
        return edit().apply {
            when(T::class.javaPrimitiveTypeOrClass) {
                Boolean::class.javaPrimitiveTypeOrClass -> putBoolean(key, value as Boolean) as? T
                Float::class.javaPrimitiveTypeOrClass -> putFloat(key, value as Float) as? T
                Int::class.javaPrimitiveTypeOrClass -> putInt(key, value as Int) as? T
                Long::class.javaPrimitiveTypeOrClass -> putLong(key, value as Long) as? T
                String::class.javaPrimitiveTypeOrClass -> putString(key, value as String) as? T
                Set::class.javaPrimitiveTypeOrClass -> putStringSet(key, value as Set<String>) as? T
                else -> return false
            }
        }.commit().also {
            notifyChange(key)
        }
    }

    private fun notifyChange(key: String?) {
        val uri = Uri.Builder()
            .scheme("content")
            .authority(authority)
            .run {
                if(key != null) {
                    appendPath(key)
                }else this
            }
            .build()
        provideContext().contentResolver.notifyChange(uri, null)
    }


}