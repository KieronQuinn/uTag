package com.kieronquinn.app.utag.ui.screens.settings.encryption.set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import kotlinx.coroutines.launch

abstract class SettingsEncryptionSetPINViewModel: ViewModel() {

    abstract var pin: String

    abstract fun onPinChanged(pin: String)
    abstract fun onPinComplete(pin: String)

}

class SettingsEncryptionSetPINViewModelImpl(
    private val navigation: SettingsNavigation
): SettingsEncryptionSetPINViewModel() {

    override var pin = ""

    override fun onPinChanged(pin: String) {
        this.pin = pin
    }

    override fun onPinComplete(pin: String) {
        viewModelScope.launch {
            navigation.navigate(
                SettingsEncryptionSetPINFragmentDirections
                    .actionSettingsEncryptionSetPINFragmentToSettingsEncryptionConfirmPINFragment(pin)
            )
        }
    }

}