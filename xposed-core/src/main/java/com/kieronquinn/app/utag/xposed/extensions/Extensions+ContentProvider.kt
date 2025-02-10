package com.kieronquinn.app.utag.xposed.extensions

import android.content.ContentProvider
import android.content.ContentProviderClient
import android.content.Context

fun ContentProvider.provideContext(): Context {
    return context!!
}

fun <T> ContentProvider.runWithClearedIdentity(block: () -> T): T {
    val identity = clearCallingIdentity()
    return block().also {
        restoreCallingIdentity(identity)
    }
}

fun <T> callProvider(
    context: Context,
    authority: String,
    block: ContentProviderClient.() -> T
): T? {
    val provider = context.contentResolver.acquireUnstableContentProviderClient(authority)
    return try {
        provider?.let { block(it) }
    } catch (e: Exception){
        null
    } finally {
        provider?.close()
    }
}