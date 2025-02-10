package com.kieronquinn.app.utag.utils.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.lifecycle.withResumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Lifecycle.runOnDestroy(block: () -> Unit) {
    addObserver(object: LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            block()
        }
    })
}

fun LifecycleOwner.whenResumed(block: suspend CoroutineScope.() -> Unit): Job {
    return lifecycleScope.launch {
        lifecycle.withResumed {
            launch {
                block()
            }
        }
    }
}

fun LifecycleOwner.whenCreated(block: suspend CoroutineScope.() -> Unit): Job {
    return lifecycleScope.launch {
        lifecycle.withCreated {
            launch {
                block()
            }
        }
    }
}

fun LifecycleRegistry.handleLifecycleEventSafely(event: Lifecycle.Event) {
    try {
        handleLifecycleEvent(event)
    }catch (e: IllegalStateException) {
        //Already at this event
    }
}