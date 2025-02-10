package com.kieronquinn.app.utag.utils.extensions

import com.kieronquinn.app.utag.model.EncryptedValue

fun Any.toEncryptedValue(): EncryptedValue {
    val value = when(this) {
        is Collection<*> -> joinToString(",")
        is Enum<*> -> name
        is String -> this
        else -> toString()
    }
    return EncryptedValue(value.toByteArray())
}

fun EncryptedValue.toDouble(): Double {
    return String(bytes).toDouble()
}

fun EncryptedValue.toFloat(): Float {
    return String(bytes).toFloat()
}

fun EncryptedValue.toInt(): Int {
    return String(bytes).toInt()
}

fun EncryptedValue.toLong(): Long {
    return String(bytes).toLong()
}

fun EncryptedValue.toBoolean(): Boolean {
    return String(bytes).toBooleanStrict()
}

fun EncryptedValue.toStringSet(): Set<String> {
    return String(bytes).split(",").toSet()
}

inline fun <reified T : Enum<T>> EncryptedValue.toEnum(): T {
    return enumValueOf<T>(String(bytes))
}

inline fun <reified T : Enum<T>> EncryptedValue.toEnumOrNull(): T? {
    return try {
        enumValueOf<T>(String(bytes))
    } catch (e: IllegalArgumentException) {
        null //No longer valid
    }
}