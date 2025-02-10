package com.kieronquinn.app.utag.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.bundle.bundleOf
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.repositories.PassiveModeRepository
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_RESULT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.METHOD_IS_IN_PASSIVE_MODE
import org.koin.android.ext.android.inject

class UTagPassiveModeProvider: ContentProvider() {

    private val passiveModeRepository by inject<PassiveModeRepository>()

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if(callingPackage != PACKAGE_NAME_ONECONNECT) {
            throw SecurityException("Not allowed")
        }
        val result = when(method) {
            METHOD_IS_IN_PASSIVE_MODE -> {
                passiveModeRepository.isInPassiveMode(arg ?: return null)
            }
            else -> return null
        }
        return bundleOf(EXTRA_RESULT to result)
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