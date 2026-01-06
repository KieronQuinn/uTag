package com.kieronquinn.app.utag.utils.extensions

import android.content.Intent
import android.content.pm.PackageManager
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.kieronquinn.app.utag.xposed.extensions.isPackageInstalled

private const val PACKAGE_CHROME = "com.android.chrome"

private val CHROME_PACKAGES = setOf(
    PACKAGE_CHROME,
    "com.chrome.beta",
    "com.chrome.dev",
    "com.chrome.canary"
)

fun getAuthIntent(url: String, forceChrome: Boolean = false): Intent {
    return CustomTabsIntent.Builder().apply {
        setShowTitle(true)
        setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        setBookmarksButtonEnabled(false)
        setDownloadButtonEnabled(false)
    }.build().intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        data = url.toUri()
        if(forceChrome) {
            setPackage(PACKAGE_CHROME)
        }
    }
}

fun Intent.isIntentChrome(packageManager: PackageManager): Boolean {
    // Edge case: If Chrome is not available (ie. they disabled it), don't prompt the user to use it
    if(!packageManager.isPackageInstalled(PACKAGE_CHROME)) return true
    val resolvedPackage = packageManager.resolveActivity(
        this,
        PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo?.packageName ?: return false
    return CHROME_PACKAGES.contains(resolvedPackage)
}