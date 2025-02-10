package com.kieronquinn.app.utag.xposed.extensions

import android.content.Context
import com.kieronquinn.app.utag.xposed.Xposed.Companion.APPLICATION_ID
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_RESULT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.METHOD_IS_SMARTTHINGS_MODDED
import com.kieronquinn.app.utag.xposed.Xposed.Companion.METHOD_SHOW_SETUP_TOAST

fun UTagXposedProvider_isSmartThingsModded(context: Context): Boolean {
    val authority = "${APPLICATION_ID}.xposed"
    return callProvider(context, authority) {
        call(METHOD_IS_SMARTTHINGS_MODDED, null, null)?.getBoolean(EXTRA_RESULT)
            ?: false
    } ?: false
}

fun UTagXposedProvider_showSetupToast(context: Context) {
    val authority = "${APPLICATION_ID}.xposed"
    callProvider(context, authority) {
        call(METHOD_SHOW_SETUP_TOAST, null, null)
    }
}