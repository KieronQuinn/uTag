package com.kieronquinn.app.utag.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.kieronquinn.app.utag.BuildConfig

class FakeFmmSupportProvider: ContentProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.fakefmm"
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when(uri.lastPathSegment) {
            "fmmsupport" -> {
                return MatrixCursor(arrayOf("fmmsupport")).apply {
                    addRow(arrayOf("true"))
                }
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }

}