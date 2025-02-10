package com.kieronquinn.app.utag.utils.extensions

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings


@SuppressLint("BatteryLife")
fun getIgnoreBatteryOptimisationsIntent(packageName: String): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        setData(Uri.parse("package:$packageName"))
    }
}

private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

fun getUWBSettingsIntent(): Intent {
    return Intent("com.android.settings.ADVANCED_CONNECTED_DEVICE_SETTINGS").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val bundle = Bundle()
        bundle.putString(EXTRA_FRAGMENT_ARG_KEY, "uwb_settings")
        putExtra(EXTRA_FRAGMENT_ARG_KEY, "uwb_settings")
        putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
    }
}