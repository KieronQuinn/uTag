package com.kieronquinn.app.utag.xposed.extensions

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

@Suppress("DEPRECATION")
fun <T: Parcelable> Bundle.getParcelableCompat(key: String, type: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, type)
    } else {
        getParcelable(key)
    }
}

@Suppress("DEPRECATION")
fun <T: Serializable> Bundle.getSerializableCompat(key: String, type: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, type)
    } else {
        getSerializable(key) as? T
    }
}