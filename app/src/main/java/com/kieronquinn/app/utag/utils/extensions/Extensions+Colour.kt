package com.kieronquinn.app.utag.utils.extensions

import android.graphics.Color

fun Int.toHexString(): String {
    return "#" + Integer.toHexString(this)
}

fun String.parseHexColour(): Int? {
    return try {
        Color.parseColor(this)
    }catch (e: IllegalAccessException){
        null
    }
}