package com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.BackupRepository
import com.kieronquinn.app.utag.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagBackup
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagRestoreProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class RestoreProgressViewModel: ViewModel() {

    abstract val state: StateFlow<UTagRestoreProgress>

    abstract fun onCloseClicked()

}

class RestoreProgressViewModelImpl(
    private val navigation: SettingsNavigation,
    backupRepository: BackupRepository,
    backup: UTagBackup,
    config: RestoreConfig
): RestoreProgressViewModel() {

    override val state = backupRepository.restoreBackup(backup, config)
        .stateIn(viewModelScope, SharingStarted.Eagerly, UTagRestoreProgress.RestoringBackup)

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}