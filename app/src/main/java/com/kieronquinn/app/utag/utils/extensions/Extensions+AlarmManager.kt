package com.kieronquinn.app.utag.utils.extensions

import android.app.AlarmManager
import android.os.Build

fun AlarmManager.canScheduleExactAlarmsCompat(): Boolean {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        canScheduleExactAlarms()
    }else true
}