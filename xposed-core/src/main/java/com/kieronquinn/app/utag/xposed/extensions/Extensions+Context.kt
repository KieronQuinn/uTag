package com.kieronquinn.app.utag.xposed.extensions

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.pm.PackageManager

fun Context.hasPermission(vararg permission: String): Boolean {
    return permission.all { checkCallingOrSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
}

fun Context.isInForeground(): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    //Hidden field for whether the task is open and focused by the user
    val isFocused = try {
        RunningAppProcessInfo::class.java.getField("isFocused")
    }catch (e: Throwable) {
        //Not supported on this device, despite it being in AOSP. Use the fallback instead.
        return isInForegroundFallback()
    }
    return activityManager.runningAppProcesses.any {
        isFocused.getBoolean(it)
    }
}

/**
 *  Less reliable than [isInForeground], occasional false positives. Used only when the main
 *  method doesn't work.
 */
private fun Context.isInForegroundFallback(): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.runningAppProcesses.any {
        it.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
}