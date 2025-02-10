package com.kieronquinn.app.utag.ui.screens.settings.backuprestore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import kotlinx.coroutines.launch

abstract class BackupRestoreViewModel: ViewModel() {

    abstract fun onBackupClicked(uri: Uri)
    abstract fun onRestoreClicked(uri: Uri)

}

class BackupRestoreViewModelImpl(
    private val navigation: SettingsNavigation
): BackupRestoreViewModel() {

    override fun onBackupClicked(uri: Uri) {
        viewModelScope.launch {
            navigation.navigate(BackupRestoreFragmentDirections
                .actionBackupRestoreFragmentToBackupFragment(uri))
        }
    }

    override fun onRestoreClicked(uri: Uri) {
        viewModelScope.launch {
            navigation.navigate(BackupRestoreFragmentDirections
                .actionBackupRestoreFragmentToRestoreConfigFragment(uri))
        }
    }

}