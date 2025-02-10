package com.kieronquinn.app.utag.utils.preferences

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.bundleOf
import com.google.gson.Gson
import com.kieronquinn.app.utag.providers.SharedPreferencesProvider
import com.kieronquinn.app.utag.providers.SharedPreferencesProvider.Companion.KEY_DEFAULT
import com.kieronquinn.app.utag.providers.SharedPreferencesProvider.Companion.KEY_KEY
import com.kieronquinn.app.utag.providers.SharedPreferencesProvider.Companion.KEY_RESULT
import com.kieronquinn.app.utag.providers.SharedPreferencesProvider.Companion.KEY_VALUE
import com.kieronquinn.app.utag.providers.SharedPreferencesProvider.Companion.Method
import com.kieronquinn.app.utag.utils.extensions.getTyped
import com.kieronquinn.app.utag.utils.extensions.putTyped
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SharedPreferencesResolver(
    context: Context,
    provider: Class<out SharedPreferencesProvider>
): SharedPreferences, SharedPreferences.Editor, KoinComponent {

    private val gson by inject<Gson>()
    private val contentResolver = context.contentResolver
    private val providerComponent = ComponentName(context, provider)
    private val listeners = HashSet<SharedPreferences.OnSharedPreferenceChangeListener>()
    
    private val authority = context.packageManager
        .getProviderInfo(providerComponent, 0).authority

    private val uri = Uri.parse("content://$authority")

    private val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            val key = uri?.lastPathSegment ?: return
            synchronized(listeners) {
                listeners.forEach {
                    it.onSharedPreferenceChanged(this@SharedPreferencesResolver, key)
                }
            }
        }
    }

    override fun getAll(): Map<String, *> {
        return gson.fromJson(
            callProvider<String>(Method.GET_ALL, Bundle.EMPTY), Map::class.java
        ) as Map<String, *>
    }

    override fun getString(key: String, defValue: String?): String? {
        return callProvider(Method.GET_STRING, keyDefault(key, defValue ?: "")) ?: defValue
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return callProvider(Method.GET_STRING_SET, keyDefault(key, defValues ?: emptySet()))
            ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        return callProvider(Method.GET_INT, keyDefault(key, defValue)) ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return callProvider(Method.GET_LONG, keyDefault(key, defValue)) ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return callProvider(Method.GET_FLOAT, keyDefault(key, defValue)) ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return callProvider(Method.GET_BOOLEAN, keyDefault(key, defValue)) ?: defValue
    }

    override fun contains(key: String): Boolean {
        return callProvider(Method.CONTAINS, key(key)) ?: false
    }

    override fun edit(): SharedPreferences.Editor {
        return this
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        synchronized(listeners) {
            if (listeners.contains(listener)) return //Already registered
            if (listeners.isEmpty()) {
                contentResolver.registerContentObserver(uri, true, observer)
            }
            listeners.add(listener)
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        synchronized(listeners) {
            listeners.remove(listener)
            if(listeners.isEmpty()) {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    override fun putString(key: String, value: String?): SharedPreferences.Editor {
        callProvider<String>(Method.PUT_STRING, keyValue(key, value ?: ""))
        return this
    }

    override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
        callProvider<Set<String>>(Method.PUT_STRING_SET, keyValue(key, values ?: emptySet()))
        return this
    }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor {
        callProvider<Int>(Method.PUT_INT, keyValue(key, value))
        return this
    }

    override fun putLong(key: String, value: Long): SharedPreferences.Editor {
        callProvider<Int>(Method.PUT_LONG, keyValue(key, value))
        return this
    }

    override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
        callProvider<Float>(Method.PUT_FLOAT, keyValue(key, value))
        return this
    }

    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
        callProvider<Boolean>(Method.PUT_BOOLEAN, keyValue(key, value))
        return this
    }

    override fun remove(key: String): SharedPreferences.Editor {
        callProvider<Boolean>(Method.REMOVE, key(key))
        return this
    }

    override fun clear(): SharedPreferences.Editor {
        callProvider<Boolean>(Method.CLEAR, Bundle.EMPTY)
        return this
    }

    override fun commit(): Boolean {
        return true //Unused, changes are made immediately
    }

    private fun key(key: String) = bundleOf(KEY_KEY to key)

    private inline fun <reified T : Any> keyValue(key: String, value: T): Bundle {
        return Bundle().apply {
            putString(KEY_KEY, key)
            putTyped(KEY_VALUE, value)
        }
    }

    private inline fun <reified T : Any> keyDefault(key: String, default: T): Bundle {
        return Bundle().apply {
            putString(KEY_KEY, key)
            putTyped(KEY_DEFAULT, default)
        }
    }

    private inline fun <reified T : Any> callProvider(method: Method, bundle: Bundle): T? {
        return contentResolver.call(authority, method.name, null, bundle)
            ?.getTyped<T>(KEY_RESULT)
    }

    override fun apply() {
        //Unused, changes are made immediately
    }
}