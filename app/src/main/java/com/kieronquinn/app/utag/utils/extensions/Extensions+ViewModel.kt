package com.kieronquinn.app.utag.utils.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.flowOf

/**
 *  A Flow which can be set to `true` and will clear back to `false` after a given delay. This
 *  is used on some screens where the state of a toggle is changed by a sync which happens after
 *  it has been unlocked from a network request.
 */
fun ViewModel.isLoadingDelayed(delay: Long = 2500L) = flowOf(false)
    .autoClearAfter(delay)
    .mutable(viewModelScope, false)