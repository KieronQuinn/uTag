package com.kieronquinn.app.utag.utils.extensions

import android.content.res.Resources

fun Resources.dip(value: Int): Int = (value * displayMetrics.density).toInt()

val Int.dp
    get() = Resources.getSystem().dip(this)