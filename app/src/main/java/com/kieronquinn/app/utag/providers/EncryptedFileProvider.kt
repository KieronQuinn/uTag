package com.kieronquinn.app.utag.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository
import com.kieronquinn.app.utag.repositories.WidgetRepository
import com.kieronquinn.app.utag.xposed.extensions.provideContext
import org.koin.android.ext.android.inject
import java.io.File
import java.io.InputStream
import java.io.OutputStream


/**
 *  Opens an encrypted file, decrypts it and serves it to the caller. Permission checks are run
 *  in [openFile], caller must be either SystemUI (for notifications), a bound widget host (for
 *  widgets) or Smartspacer (for the plugin)
 */
class EncryptedFileProvider: ContentProvider() {

    companion object {
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.encryptedprovider"

        private val PACKAGE_ALLOWLIST = setOf(
            BuildConfig.APPLICATION_ID,
            "com.android.systemui",
            "com.kieronquinn.app.smartspacer"
        )

        fun createUri(file: File): Uri {
            return Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(file.name)
                .build()
        }
    }

    private val widgetRepository by inject<WidgetRepository>()
    private val historyWidgetRepository by inject<HistoryWidgetRepository>()

    private val encryptedImagesDirectory by lazy {
        File(provideContext().filesDir, "images")
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val allowedPackages = widgetRepository.getAllowedPackages() +
                historyWidgetRepository.getAllowedPackages() + PACKAGE_ALLOWLIST
        val calling = callingPackage
        if(calling != null && !allowedPackages.contains(calling)) {
            throw SecurityException("$calling is not allowed to access encrypted files")
        }
        val filename = uri.lastPathSegment ?: return null
        val file = File(encryptedImagesDirectory, filename).takeIf {
            it.exists()
        }?.inputStream() ?: return null
        val pipe = ParcelFileDescriptor.createPipe()
        val out = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
        TransferThread(file, out).start()
        return pipe[0]
    }

    override fun getType(uri: Uri): String {
        return "application/octet-stream"
    }

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

    private inner class TransferThread(
        private val input: InputStream,
        private val output: OutputStream
    ): Thread() {
        override fun run() {
            try {
                val bytes = input.readBytes()
                val decrypted = widgetRepository.decryptImage(bytes) ?: byteArrayOf()
                output.write(decrypted)
                output.flush()
                output.close()
                input.close()
            }catch (e: Exception) {
                //Nothing we can do
            }
        }
    }

}