package com.kieronquinn.app.utag.utils.extensions

import android.os.DeadObjectException
import com.kieronquinn.app.utag.service.IUTagService
import com.kieronquinn.app.utag.service.callback.ITagConnectResultCallback
import com.samsung.android.oneconnect.base.device.tag.TagConnectionState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun IUTagService.onConnectResult() = callbackFlow {
    val callback = object: ITagConnectResultCallback.Stub() {
        override fun onTagConnectResult(
            deviceId: String,
            state: TagConnectionState
        ) {
            trySend(Pair(deviceId, state))
        }
    }
    val id = addTagConnectResultCallback(callback)
    awaitClose {
        try {
            removeTagConnectResultCallback(id ?: return@awaitClose)
        }catch (e: DeadObjectException) {
            //Disconnected, service will handle it
        }
    }
}