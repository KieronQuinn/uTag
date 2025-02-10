package com.kieronquinn.app.utag.utils.extensions

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavArgs
import androidx.navigation.NavArgsLazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun Fragment.childBackStackTopFragment() = callbackFlow {
    val listener = FragmentManager.OnBackStackChangedListener {
        trySend(getTopFragment())
    }
    childFragmentManager.addOnBackStackChangedListener(listener)
    trySend(getTopFragment())
    awaitClose {
        childFragmentManager.removeOnBackStackChangedListener(listener)
    }
}

fun Fragment.getTopFragment(): Fragment? {
    if(!isAdded) return null
    return childFragmentManager.fragments.firstOrNull()
}

fun Fragment.getBackFragment(): Fragment? {
    if(!isAdded) return null
    return childFragmentManager.fragments.getOrNull(1)
}

/**
 *  Helper for [LifecycleOwner].[whenResumed]
 */
fun Fragment.whenResumed(block: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.whenResumed(block)
}

/**
 *  Helper for [LifecycleOwner].[whenCreated]
 */
fun Fragment.whenCreated(block: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.whenCreated(block)
}

val Fragment.containerParent
    get() = parentFragment?.parentFragment

@MainThread
inline fun <reified Args : NavArgs> Fragment.containerParentNavArgs(): NavArgsLazy<Args> =
    NavArgsLazy(Args::class) {
        containerParent?.arguments
            ?: throw IllegalStateException("Fragment $this has null arguments")
    }