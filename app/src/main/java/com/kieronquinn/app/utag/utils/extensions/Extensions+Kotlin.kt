package com.kieronquinn.app.utag.utils.extensions

import kotlin.reflect.KClass

suspend fun <T> runCatchingOrNull(block: suspend () -> T): T? {
    return runCatching {
        block()
    }.getOrNull()
}

val <T : Any> KClass<T>.javaPrimitiveTypeOrClass: Class<T>
    get() = javaPrimitiveType ?: java