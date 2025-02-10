package com.kieronquinn.app.utag.ui.screens.setup.chaser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import kotlinx.coroutines.launch

abstract class SetupChaserViewModel: ViewModel() {

    abstract fun onAgreeClicked()
    abstract fun onDisagreeClicked()

}

class SetupChaserViewModelImpl(
    private val navigation: SetupNavigation,
    private val encryptedSettingsRepository: EncryptedSettingsRepository
): SetupChaserViewModel() {

    override fun onAgreeClicked() {
        viewModelScope.launch {
            encryptedSettingsRepository.networkContributionsEnabled.set(true)
            navigateToNext()
        }
    }

    override fun onDisagreeClicked() {
        viewModelScope.launch {
            encryptedSettingsRepository.networkContributionsEnabled.set(false)
            navigateToNext()
        }
    }

    private suspend fun navigateToNext() {
        if(encryptedSettingsRepository.utsScanEnabled.exists()) {
            navigation.navigate(SetupChaserFragmentDirections
                .actionSetupChaserFragmentToSetupAccountFragment())
        }else{
            navigation.navigate(SetupChaserFragmentDirections
                .actionSetupChaserFragmentToSetupUtsFragment())
        }
    }

}