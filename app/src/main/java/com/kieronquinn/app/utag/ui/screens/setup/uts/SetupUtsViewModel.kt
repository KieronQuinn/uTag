package com.kieronquinn.app.utag.ui.screens.setup.uts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.ui.screens.setup.chaser.SetupUtsFragmentDirections
import kotlinx.coroutines.launch

abstract class SetupUtsViewModel: ViewModel() {

    abstract fun onAgreeClicked()
    abstract fun onDisagreeClicked()

}

class SetupUtsViewModelImpl(
    private val navigation: SetupNavigation,
    private val encryptedSettingsRepository: EncryptedSettingsRepository
): SetupUtsViewModel() {

    override fun onAgreeClicked() {
        viewModelScope.launch {
            encryptedSettingsRepository.utsScanEnabled.set(true)
            navigateToNext()
        }
    }

    override fun onDisagreeClicked() {
        viewModelScope.launch {
            encryptedSettingsRepository.utsScanEnabled.set(false)
            navigateToNext()
        }
    }

    private suspend fun navigateToNext() {
        navigation.navigate(SetupUtsFragmentDirections.actionSetupUtsFragmentToSetupAccountFragment())
    }

}