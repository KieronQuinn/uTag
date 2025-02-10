package com.kieronquinn.app.utag.ui.screens.settings.backuprestore

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.utils.extensions.appendBullet
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupRestoreFragment: BaseSettingsFragment(), BackAvailable {

    companion object {
        private const val UTAG_BACKUP_FILE_TEMPLATE = "backup_%s.utbk"
    }

    private val viewModel by viewModel<BackupRestoreViewModel>()

    private val backupLauncher = registerForActivityResult(CreateDocument()) {
        if(it == null) return@registerForActivityResult
        viewModel.onBackupClicked(it)
    }

    private val restoreLauncher = registerForActivityResult(OpenDocument()) {
        if(it == null) return@registerForActivityResult
        viewModel.onRestoreClicked(it)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPreferences {
            preference {
                title = getString(R.string.backup_restore_backup_title)
                summary = getString(R.string.backup_restore_backup_content)
                onClick {
                    backupLauncher.launch(getFilename())
                }
            }
            preference {
                title = getString(R.string.backup_restore_restore_title)
                summary = getString(R.string.backup_restore_restore_content)
                onClick {
                    restoreLauncher.launch(arrayOf("*/*"))
                }
            }
            preferenceCategory("backup_restore_info") {
                tipsCardPreference {
                    title = getString(R.string.backup_restore_included_title)
                    summary = createInfo(
                        R.array.backup_restore_included_items,
                        R.string.backup_restore_included_footer
                    )
                }
                tipsCardPreference {
                    title = getString(R.string.backup_restore_not_included_title)
                    summary = createInfo(
                        R.array.backup_restore_not_included_items,
                        R.string.backup_restore_not_included_footer
                    )
                }
            }
        }
    }

    private fun getFilename(): String {
        val time = LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return String.format(UTAG_BACKUP_FILE_TEMPLATE, dateTimeFormatter.format(time))
    }

    private fun createInfo(
        @ArrayRes items: Int,
        @StringRes footer: Int
    ) = SpannableStringBuilder().apply {
        val footerItems = resources.getTextArray(items)
        footerItems.forEachIndexed { i, item ->
            appendBullet()
            append(item)
            if(i < footerItems.size - 1) {
                appendLine()
            }
        }
        appendLine()
        appendLine()
        append(getText(footer))
    }

}