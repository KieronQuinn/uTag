package com.kieronquinn.app.utag.utils.extensions

import android.animation.ValueAnimator
import android.view.View
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val TAP_DEBOUNCE = 250L

suspend fun View.awaitPost() = suspendCancellableCoroutine {
    post {
        if(isAttachedToWindow){
            it.resume(this)
        }else{
            it.cancel()
        }
    }
}

fun View.onClicked() = callbackFlow {
    setOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)

suspend fun View.onClicked(block: View.() -> Unit) {
    onClicked().collect {
        block()
    }
}

fun View.onLongClicked(vibrate: Boolean = true) = callbackFlow<View> {
    setOnLongClickListener {
        trySend(it)
        vibrate
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)

fun View.animateElevationChange(afterElevation: Float): ValueAnimator {
    return ValueAnimator.ofFloat(elevation, afterElevation).apply {
        duration = 250
        addUpdateListener {
            this@animateElevationChange.elevation = it.animatedValue as Float
        }
        start()
    }
}

val View.widthWithMargins: Int
    get() = width + marginLeft + marginRight

val View.heightWithMargins: Int
    get() = height + marginTop + marginBottom