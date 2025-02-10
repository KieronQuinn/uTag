package com.kieronquinn.app.utag.utils.extensions

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun <T> Task<T>.await() = suspendCancellableCoroutine {
    addOnCompleteListener { task ->
        if(task.isSuccessful) {
            it.resume(task.result)
        }else{
            it.resume(null)
        }
    }
}