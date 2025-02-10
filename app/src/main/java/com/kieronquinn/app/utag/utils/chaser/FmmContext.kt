package com.kieronquinn.app.utag.utils.chaser

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager

class FmmContext(context: Context) : ContextWrapper(context) {

    override fun getPackageName(): String {
        //Native library checks package name is FMM, so fake that
        return "com.samsung.android.fmm"
    }

    override fun getPackageManager(): PackageManager {
        //Native library does package checks, so we need to inject our own fake PackageManager
        return FmmPackageManager()
    }

}