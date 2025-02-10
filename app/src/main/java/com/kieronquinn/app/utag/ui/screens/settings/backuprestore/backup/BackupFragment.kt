package com.kieronquinn.app.utag.ui.screens.settings.backuprestore.backup

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagBackupProgress
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagBackupProgress.ErrorReason
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class BackupFragment: BaseSettingsFragment(), BackAvailable {

    private val navArgs by navArgs<BackupFragmentArgs>()

    private val viewModel by viewModel<BackupViewModel> {
        parametersOf(navArgs.uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: UTagBackupProgress) {
        when(state){
            is UTagBackupProgress.CreatingBackup -> {
                setLoading(true, getString(R.string.backup_creating_backup))
            }
            is UTagBackupProgress.CreatingAutomationConfigsBackup -> {
                setLoading(true, getString(R.string.backup_creating_automation_configs_backup))
            }
            is UTagBackupProgress.CreatingFindMyDeviceConfigsBackup -> {
                setLoading(true, getString(R.string.backup_creating_find_my_device_configs_backup))
            }
            is UTagBackupProgress.CreatingLocationSafeAreasBackup -> {
                setLoading(true, getString(R.string.backup_creating_location_safe_areas_backup))
            }
            is UTagBackupProgress.CreatingNotifyDisconnectConfigsBackup -> {
                setLoading(true, getString(R.string.backup_creating_notify_disconnect_configs_backup))
            }
            is UTagBackupProgress.CreatingPassiveModeConfigsBackup -> {
                setLoading(true, getString(R.string.backup_creating_passive_mode_configs_backup))
            }
            is UTagBackupProgress.CreatingWiFiSafeAreasBackup -> {
                setLoading(true, getString(R.string.backup_creating_wifi_safe_areas_backup))
            }
            is UTagBackupProgress.CreatingSettingsBackup -> {
                setLoading(true, getString(R.string.backup_creating_settings_backup))
            }
            is UTagBackupProgress.CreatingUnknownTagsBackup -> {
                setLoading(true, getString(R.string.backup_creating_unknown_tags_backup))
            }
            is UTagBackupProgress.WritingFile -> {
                setLoading(true, getString(R.string.backup_creating_backup))
            }
            is UTagBackupProgress.Finished -> setPreferences {
                actionCardPreference {
                    title = getString(R.string.backup_created)
                    summary = state.filename
                    addButton(getString(R.string.backup_restore_close)) {
                        viewModel.onCloseClicked()
                    }
                }
            }
            is UTagBackupProgress.Error -> showErrorDialog(state.reason)
        }
    }

    private fun showErrorDialog(reason: ErrorReason) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.backup_error)
            setMessage(reason.description)
            setCancelable(false)
            setPositiveButton(R.string.backup_restore_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.onCloseClicked()
            }
        }.show()
    }

}