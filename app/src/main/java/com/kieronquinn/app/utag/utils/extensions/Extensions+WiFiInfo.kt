package com.kieronquinn.app.utag.utils.extensions

import android.annotation.SuppressLint
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

fun WifiInfo.isInvalid(): Boolean {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ssid == WifiManager.UNKNOWN_SSID || bssid == "02:00:00:00:00:00"
    }else false
}

fun WifiInfo.getSSIDOrNull(): String? {
    return ssid.takeUnless { isInvalid() }?.removePrefix("\"")?.removeSuffix("\"")
}

@SuppressLint("HardwareIds")
fun WifiInfo.getMacAddressOrNull(): String? {
    return bssid.takeUnless { isInvalid() }?.uppercase()
}