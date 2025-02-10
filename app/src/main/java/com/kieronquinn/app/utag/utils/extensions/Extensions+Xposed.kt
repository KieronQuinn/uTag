package com.kieronquinn.app.utag.utils.extensions

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CAPSULE_PROVIDER_RESULT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CapsuleProviderMethod
import com.kieronquinn.app.utag.xposed.extensions.getParcelableCompat

fun Context.callCapsuleProviderBooleanMethod(
    method: CapsuleProviderMethod,
    extras: Bundle? = null
): Boolean {
    return callCapsuleProviderMethod(method, extras) {
        getBoolean(it, false)
    } ?: false
}

fun Context.callCapsuleProviderIntMethod(
    method: CapsuleProviderMethod,
    extras: Bundle? = null
): Int? {
    return callCapsuleProviderMethod(method, extras) {
        getInt(it, -1)
    }?.takeIf { it >= 0 }
}

fun Context.callCapsuleProviderStringMethod(
    method: CapsuleProviderMethod,
    extras: Bundle? = null
): String? {
    return callCapsuleProviderMethod(method, extras) {
        getString(it)
    }
}

fun Context.callCapsuleProviderPendingIntentMethod(
    method: CapsuleProviderMethod,
    extras: Bundle? = null
): PendingIntent? {
    return callCapsuleProviderMethod(method, extras) {
        getParcelableCompat(it, PendingIntent::class.java)
    }
}

private fun <T> Context.callCapsuleProviderMethod(
    method: CapsuleProviderMethod,
    extras: Bundle? = null,
    getter: Bundle.(key: String) -> T
): T? {
    val provider = contentResolver.acquireUnstableContentProviderClient(
        "com.samsung.android.oneconnect.CapsuleProvider"
    ) ?: return null
    return try {
        provider.call(method.name, null, extras)?.let {
            getter(it, CAPSULE_PROVIDER_RESULT)
        }
    } catch (e: Throwable) {
        null
    } finally {
        provider.close()
    }
}