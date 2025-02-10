package com.kieronquinn.app.utag.utils.extensions

import android.view.View
import com.kieronquinn.app.utag.ui.views.HeaderView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun HeaderView.onNavigationIconClicked() = callbackFlow<View> {
    setNavigationButtonOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)