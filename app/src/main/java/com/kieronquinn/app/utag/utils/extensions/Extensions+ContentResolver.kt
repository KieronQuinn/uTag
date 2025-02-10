package com.kieronquinn.app.utag.utils.extensions

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

fun ContentResolver.safeQuery(
    uri: Uri,
    projection: Array<String?>?,
    selection: String?,
    selectionArgs: Array<String?>?,
    sortOrder: String?
): Cursor? {
    return try {
        query(uri, projection, selection, selectionArgs, sortOrder)
    }catch (e: SecurityException){
        //Provider not found
        null
    }
}

fun ContentResolver.registerContentObserverSafely(
    uri: Uri,
    notifyForDescendants: Boolean,
    observer: ContentObserver
) {
    try{
        registerContentObserver(uri, notifyForDescendants, observer)
    }catch (e: Exception){
        //Does not exist
        e.printStackTrace()
    }
}

fun ContentResolver.unregisterContentObserverSafely(observer: ContentObserver) {
    try{
        unregisterContentObserver(observer)
    }catch (e: Exception){
        //Does not exist
        e.printStackTrace()
    }
}

fun ContentResolver.observerAsFlow(uri: Uri) = callbackFlow {
    val observer = object: ContentObserver(android.os.Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            trySend(uri)
        }
    }
    trySend(null)
    registerContentObserverSafely(uri, true, observer)
    awaitClose {
        unregisterContentObserverSafely(observer)
    }
}.flowOn(Dispatchers.IO)