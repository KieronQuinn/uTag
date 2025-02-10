package com.kieronquinn.app.utag.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

class WidgetErrorMessageActivity: BaseActivity() {

    companion object {
        private const val EXTRA_ERROR_MESSAGE = "ERROR_MESSAGE"

        fun getIntent(context: Context, errorMessage: Int): Intent {
            return Intent(context, WidgetErrorMessageActivity::class.java).apply {
                putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val errorMessage = intent.getIntExtra(EXTRA_ERROR_MESSAGE, -1)
            .takeIf { it >= 0 } ?: run {
                finishAndRemoveTask()
                return
            }
        AlertDialog.Builder(this).apply {
            setMessage(getString(errorMessage))
            setPositiveButton(android.R.string.ok) { _, _ ->
                finishAndRemoveTask()
            }
            setOnDismissListener {
                finishAndRemoveTask()
            }
        }.show()
    }

}