package com.kieronquinn.app.utag.utils.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun <T> Flow<T?>.firstNotNull(): T {
    return first { it != null }!!
}

/**
 *  Returns a [MutableStateFlow] which takes all of the incoming [Flow]'s emissions, but can be
 *  overridden locally. The incoming flow's emissions take priority if they are emitted after a
 *  modification.
 */
fun <T> Flow<T>.mutable(scope: CoroutineScope, initialValue: T): MutableStateFlow<T> {
    val outFlow = MutableStateFlow(initialValue)
    scope.launch {
        collect {
            outFlow.emit(it)
        }
    }
    return outFlow
}

fun <T> Flow<T>.autoClearAfter(
    delayTime: Long,
    invokeOnClear: suspend () -> Unit = {}
): Flow<T?> = channelFlow {
    val original = this@autoClearAfter
    original.collectLatest { newValue ->
        send(newValue)
        delay(delayTime)
        send(null)
        invokeOnClear()
    }
}

/**
 *  Waits for the first emission matching the predicate, but discards it.
 */
suspend fun <T> Flow<T>.await(predicate: T.() -> Boolean) {
    first { predicate(it) }
}

fun <T> Flow<T>.delayBy(period: Long) = onEach {
    delay(period)
}

/**
 *  Returns first flow result from two flows, using a given scope
 */
suspend fun <T1, T2> first(
    scope: CoroutineScope,
    one: Flow<T1>,
    two: Flow<T2>
): FirstResult<T1, T2> = suspendCancellableCoroutine {
    var job1: Job? = null
    var job2: Job? = null
    job1 = scope.launch {
        it.resume(FirstResult.One(one.first()))
        job1?.cancel()
        job2?.cancel()
    }
    job2 = scope.launch {
        it.resume(FirstResult.Two(two.first()))
        job1.cancel()
        job2?.cancel()
    }
    it.invokeOnCancellation {
        job1.cancel()
        job2.cancel()
    }
}

sealed class FirstResult<T1, T2> {
    data class One<T1, T2>(val value: T1): FirstResult<T1, T2>()
    data class Two<T1, T2>(val value: T2): FirstResult<T1, T2>()
}