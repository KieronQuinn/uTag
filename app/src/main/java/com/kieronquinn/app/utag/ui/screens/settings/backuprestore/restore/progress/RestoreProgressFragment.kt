package com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.progress

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagRestoreProgress
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RestoreProgressFragment: BaseSettingsFragment(), BackAvailable {

    private val navArgs by navArgs<RestoreProgressFragmentArgs>()

    private val viewModel by viewModel<RestoreProgressViewModel> {
        parametersOf(navArgs.backup, navArgs.config)
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

    private fun handleState(state: UTagRestoreProgress) {
        when(state){
            is UTagRestoreProgress.RestoringBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_backup))
            }
            is UTagRestoreProgress.RestoringAutomationConfigsBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_automation_configs))
            }
            is UTagRestoreProgress.RestoringFindMyDeviceConfigsBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_find_my_device_configs))
            }
            is UTagRestoreProgress.RestoringLocationSafeAreasBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_location_safe_areas))
            }
            is UTagRestoreProgress.RestoringNotifyDisconnectConfigsBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_notify_disconnect_configs))
            }
            is UTagRestoreProgress.RestoringPassiveModeConfigsBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_passive_mode_configs))
            }
            is UTagRestoreProgress.RestoringWiFiSafeAreasBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_wifi_safe_areas))
            }
            is UTagRestoreProgress.RestoringUnknownTagsBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_unknown_tags))
            }
            is UTagRestoreProgress.RestoringSettingsBackup -> {
                setLoading(true, getString(R.string.restore_progress_restoring_settings))
            }
            is UTagRestoreProgress.Finished -> setPreferences {
                actionCardPreference {
                    title = getString(R.string.restore_progress_restoring_complete)
                    summary = getString(R.string.restore_progress_restoring_complete_content)
                    addButton(getString(R.string.backup_restore_close)) {
                        viewModel.onCloseClicked()
                    }
                }
            }
        }
    }

}