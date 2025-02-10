package com.kieronquinn.app.utag.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.repositories.AnalyticsRepository
import com.kieronquinn.app.utag.xposed.extensions.EXTRA_THROWABLE
import com.kieronquinn.app.utag.xposed.extensions.METHOD_REPORT_NON_FATAL
import com.kieronquinn.app.utag.xposed.extensions.getSerializableCompat
import com.kieronquinn.app.utag.xposed.extensions.provideContext
import org.koin.android.ext.android.inject

class UTagXposedCrashReportProvider: ContentProvider() {

    private val analyticsRepository by inject<AnalyticsRepository>()

    override fun onCreate(): Boolean {
        FirebaseApp.initializeApp(provideContext())
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if(callingPackage != PACKAGE_NAME_ONECONNECT) {
            throw SecurityException("Not allowed")
        }
        when(method) {
            METHOD_REPORT_NON_FATAL -> {
                val throwable = extras?.getSerializableCompat(EXTRA_THROWABLE, Throwable::class.java)
                    ?: return null
                analyticsRepository.recordNonFatal(throwable)
            }
        }
        return null
    }

    //Default ContentProvider methods are all no-op

    override fun getType(uri: Uri): String? = null

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

}