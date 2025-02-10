package com.kieronquinn.app.utag.utils.extensions

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 *  Holds the current thread until the lock becomes available
 */
suspend fun Mutex.await() = withLock {}