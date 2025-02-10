package com.kieronquinn.app.utag.xposed.extensions

import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import com.kieronquinn.app.utag.xposed.Xposed
import java.security.MessageDigest

@OptIn(ExperimentalStdlibApi::class)
private fun PackageManager.getPackageSignatureSHA(packageName: String): String? {
    return try {
        val info = getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
            ?: return null
        val signatureList = if (info.hasMultipleSigners()) {
            info.apkContentsSigners.map {
                val digest = MessageDigest.getInstance("SHA")
                digest.update(it.toByteArray())
                digest.digest().toHexString()
            }
        } else {
            info.signingCertificateHistory.map {
                val digest = MessageDigest.getInstance("SHA")
                digest.update(it.toByteArray())
                digest.digest().toHexString()
            }
        }
        return signatureList.firstOrNull()
    } catch (e: Exception) {
        null
    }
}

//SHA of uTag signature is used to know if SmartThings is pre-modded build only
private const val UTAG_SIGNATURE_SHA = "B63E9D3044F8C57716750BC0AF6006CC7FD72DCF"

fun PackageManager.isSmartThingsModded(): Boolean {
    return getPackageSignatureSHA(Xposed.PACKAGE_NAME_ONECONNECT)
        .equals(UTAG_SIGNATURE_SHA, true)
}

fun PackageManager.isPackageInstalled(packageName: String, flags: Int = 0): Boolean {
    return try {
        getPackageInfo(packageName, flags)
        true
    }catch (e: NameNotFoundException){
        false
    }
}