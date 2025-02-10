package com.kieronquinn.app.utag.utils.extensions

import android.widget.ViewFlipper

fun ViewFlipper.setDisplayedChildIfNeeded(child: Int) {
    if(displayedChild == child) return
    displayedChild = child
}