package com.kieronquinn.app.utag.ui.screens.settings.backuprestore.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.BackupRepository
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagBackupProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BackupViewModel: ViewModel() {

    abstract val state: StateFlow<UTagBackupProgress>

    abstract fun onCloseClicked()

}

class BackupViewModelImpl(
    private val navigation: SettingsNavigation,
    backupRepository: BackupRepository,
    uri: Uri
): BackupViewModel() {

    override val state = backupRepository.createBackup(uri)
        .stateIn(viewModelScope, SharingStarted.Eagerly, UTagBackupProgress.CreatingBackup)

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}