package com.kieronquinn.app.utag.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.AnalyticsRepository
import com.kieronquinn.app.utag.xposed.extensions.EXTRA_SOURCE
import org.koin.android.ext.android.inject

class UnsupportedIntentActivity: BaseActivity() {

    companion object {
        private enum class Source(
            val source: String?,
            @StringRes val message: Int,
            val githubUrl: String? = null
        ) {
            PET(
                "petcareplugin",
                R.string.unsupported_intent_dialog_content_pet,
                "https://kieronquinn.co.uk/redirect/uTag/github/pet"
            ),
            UNKNOWN(null, R.string.unsupported_intent_dialog_content_generic)
        }
    }

    private val analyticsRepository by inject<AnalyticsRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rawSource = intent.getStringExtra(EXTRA_SOURCE)
        val source = Source.entries.firstOrNull { it.source == rawSource } ?: Source.UNKNOWN
        analyticsRepository.logEvent(
            "unsupported_intent",
            bundleOf("source" to rawSource)
        )
        AlertDialog.Builder(this).apply {
            setTitle(R.string.unsupported_intent_dialog_title)
            setMessage(getString(source.message, rawSource))
            setPositiveButton(android.R.string.ok) { _, _ ->
                finishAndRemoveTask()
            }
            setNegativeButton(R.string.unsupported_intent_dialog_close) { _, _ ->
                finishAndRemoveTask()
            }
            setOnDismissListener {
                finishAndRemoveTask()
            }
            if(source.githubUrl != null) {
                setPositiveButton(R.string.unsupported_intent_dialog_github) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(source.githubUrl)
                    })
                    finishAndRemoveTask()
                }
            }
        }.show()
    }

}