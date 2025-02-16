package com.kieronquinn.app.utag.ui.screens.setup.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import kotlinx.coroutines.launch

abstract class SetupLandingViewModel: ViewModel() {

    abstract fun onGetStartedClicked()
    abstract fun onDebugLongClicked()

}

class SetupLandingViewModelImpl(
    private val navigation: SetupNavigation,
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    settingsRepository: SettingsRepository
): SetupLandingViewModel() {

    private val hasAgreedToPolicies = settingsRepository.hasAgreedToPolicies

    override fun onGetStartedClicked() {
        viewModelScope.launch {
            if(hasAgreedToPolicies.get()) {
                navigation.navigate(
                    SetupLandingFragmentDirections
                        .actionLandingFragmentToSetupModFragment()
                )
            }else{
                navigation.navigate(
                    SetupLandingFragmentDirections
                        .actionLandingFragmentToSetupPrivacyFragment()
                )
            }
        }
    }

    override fun onDebugLongClicked() {
        viewModelScope.launch {
            encryptedSettingsRepository.debugModeVisible.set(true)
            encryptedSettingsRepository.debugModeEnabled.set(true)
        }
    }

}