package com.kieronquinn.app.utag.utils.extensions

import android.app.Activity
import androidx.core.app.ActivityCompat

fun Activity.showPermissionRationale(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}