package com.kieronquinn.app.utag.utils.extensions

import android.webkit.CookieManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun CookieManager.clear() = suspendCoroutine { resume ->
    removeAllCookies {
        flush()
        resume.resume(Unit)
    }
}