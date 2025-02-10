package com.kieronquinn.app.utag.xposed.extensions

import android.content.ContentProviderClient
import android.content.Context
import com.kieronquinn.app.utag.xposed.Xposed.Companion.APPLICATION_ID
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_RESULT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.METHOD_IS_IN_PASSIVE_MODE

fun UTagPassiveModeProvider_isInPassiveMode(context: Context, deviceId: String): Boolean {
    val authority = "${APPLICATION_ID}.passivemode"
    return callProvider(context, authority) {
        call(METHOD_IS_IN_PASSIVE_MODE, deviceId, null)?.getBoolean(EXTRA_RESULT)
            ?: false
    } ?: false
}