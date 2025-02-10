package com.kieronquinn.app.utag.xposed.extensions

import android.os.Build

fun <T> T.takeIfAtLeastS(): T? {
    return takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
}

fun <T> T.takeIfAtLeastT(): T? {
    return takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
}

fun <T> T.takeIfAtMostR(): T? {
    return takeIf { Build.VERSION.SDK_INT <= Build.VERSION_CODES.R }
}
