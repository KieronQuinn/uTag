package com.kieronquinn.app.utag.xposed.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable

const val INTENT_KEY_SECURITY_TAG = "security_tag"
const val PENDING_INTENT_REQUEST_CODE = 999

fun Intent.applySecurity(context: Context) {
    val securityTag = PendingIntent.getActivity(
        context,
        PENDING_INTENT_REQUEST_CODE,
        Intent(),
        PendingIntent.FLAG_IMMUTABLE
    )
    putExtra(INTENT_KEY_SECURITY_TAG, securityTag)
}

fun Intent.verifySecurity(requiredPackage: String) {
    getParcelableExtraCompat(INTENT_KEY_SECURITY_TAG, PendingIntent::class.java)?.let {
        if(it.creatorPackage == requiredPackage) return
    }
    throw SecurityException("Unauthorised access")
}

@Suppress("DEPRECATION")
fun <T: Parcelable> Intent.getParcelableExtraCompat(key: String, type: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, type)
    } else {
        getParcelableExtra(key)
    }
}