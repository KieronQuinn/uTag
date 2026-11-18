package com.kieronquinn.app.utag.utils.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/** Prefer the package-install action, falling back to viewing the APK if it is unavailable. */
fun Context.createApkInstallIntent(uri: Uri): Intent? {
    val installPackage = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        setDataAndType(uri, APK_MIME_TYPE)
        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        putExtra(Intent.EXTRA_RETURN_RESULT, false)
        addApkInstallFlags()
    }
    if(packageManager.resolveActivity(installPackage, 0) != null) return installPackage
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, APK_MIME_TYPE)
        addApkInstallFlags()
    }
    return view.takeIf { packageManager.resolveActivity(it, 0) != null }
}

private fun Intent.addApkInstallFlags() {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
