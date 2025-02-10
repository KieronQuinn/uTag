package com.kieronquinn.app.utag.xposed.extensions

import android.Manifest
import android.annotation.SuppressLint

@SuppressLint("InlinedApi")
fun getRequiredOneConnectPermissions(): Array<String> {
    return listOfNotNull(
        Manifest.permission.BLUETOOTH_SCAN.takeIfAtLeastS(),
        Manifest.permission.NEARBY_WIFI_DEVICES.takeIfAtLeastT(),
        Manifest.permission.BLUETOOTH_ADMIN.takeIfAtMostR(),
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ).toTypedArray()
}