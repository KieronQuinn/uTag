package com.kieronquinn.app.utag.xposed.extensions

import android.content.Context
import androidx.core.os.bundleOf
import com.kieronquinn.app.utag.xposed.Xposed.Companion.APPLICATION_ID

const val METHOD_REPORT_NON_FATAL = "report_non_fatal"
const val EXTRA_THROWABLE = "throwable"

fun XposedCrashReportProvider_reportNonFatal(context: Context, throwable: Throwable) {
    val authority = "${APPLICATION_ID}.crashreport"
    callProvider(context, authority) {
        val extras = bundleOf(EXTRA_THROWABLE to throwable)
        call(METHOD_REPORT_NON_FATAL, null, extras)
    }
}