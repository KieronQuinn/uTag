package com.kieronquinn.app.utag.utils.extensions

import android.os.Bundle

inline fun <reified T: Any> Bundle.getTyped(key: String): T? {
    return when(T::class.javaPrimitiveTypeOrClass) {
        Boolean::class.javaPrimitiveTypeOrClass -> getBoolean(key) as? T
        Float::class.javaPrimitiveTypeOrClass -> getFloat(key) as? T
        Int::class.javaPrimitiveTypeOrClass -> getInt(key) as? T
        Long::class.javaPrimitiveTypeOrClass -> getLong(key) as? T
        String::class.javaPrimitiveTypeOrClass -> getString(key) as? T
        Set::class.javaPrimitiveTypeOrClass -> getStringArray(key)?.toSet() as? T
        else -> return null
    }
}

inline fun <reified T: Any> Bundle.putTyped(key: String, value: T) {
    when(T::class.javaPrimitiveTypeOrClass) {
        Boolean::class.javaPrimitiveTypeOrClass -> putBoolean(key, value as Boolean)
        Float::class.javaPrimitiveTypeOrClass -> putFloat(key, value as Float)
        Int::class.javaPrimitiveTypeOrClass -> putInt(key, value as Int)
        Long::class.javaPrimitiveTypeOrClass -> putLong(key, value as Long)
        String::class.javaPrimitiveTypeOrClass -> putString(key, value as String)
        Set::class.javaPrimitiveTypeOrClass -> putStringArray(key, (value as Set<String>).toTypedArray())
    }
}