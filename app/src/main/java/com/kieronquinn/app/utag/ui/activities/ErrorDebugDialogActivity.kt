package com.kieronquinn.app.utag.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.utils.extensions.copyToClipboard
import com.kieronquinn.app.utag.xposed.extensions.applySecurity
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity

class ErrorDebugDialogActivity: BaseActivity() {

    companion object {
        private const val EXTRA_MESSAGE = "message"

        fun createIntent(context: Context, message: String): Intent {
            return Intent(context, ErrorDebugDialogActivity::class.java).apply {
                applySecurity(context)
                putExtra(EXTRA_MESSAGE, message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.verifySecurity(packageName)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: run {
            finishAndRemoveTask()
            return
        }
        AlertDialog.Builder(this).apply {
            setTitle(R.string.debug_error_dialog_title)
            setMessage(message)
            setPositiveButton(R.string.debug_error_dialog_close) { _, _ ->
                finishAndRemoveTask()
            }
            setNegativeButton(R.string.debug_error_dialog_copy) { _, _ ->
                copyToClipboard(message, "")
            }
            setOnDismissListener {
                finishAndRemoveTask()
            }
        }.show()
    }

}