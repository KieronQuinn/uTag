package com.kieronquinn.app.utag.utils.extensions

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import com.kieronquinn.app.utag.xposed.Xposed

fun PackageManager.getInstalledApps(): List<ActivityInfo> {
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    return queryIntentActivities(launcherIntent, 0).map {
        it.activityInfo
    }
}

fun PackageManager.getInstalledShortcuts(): List<ActivityInfo> {
    val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
    return queryIntentActivities(shortcutIntent, 0).map {
        it.activityInfo
    }
}

fun PackageManager.wasSmartThingsInstalledByPlay(): Boolean {
    return getInstallSource(Xposed.PACKAGE_NAME_ONECONNECT) == "com.android.vending"
}

@Suppress("DEPRECATION")
private fun PackageManager.getInstallSource(packageName: String): String? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getInstallSourceInfo(packageName).installingPackageName
        } else {
            getInstallerPackageName(packageName)
        }
    }catch (e: NameNotFoundException) {
        null
    }
}