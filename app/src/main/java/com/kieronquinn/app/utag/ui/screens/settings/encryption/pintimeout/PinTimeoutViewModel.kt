package com.kieronquinn.app.utag.ui.screens.settings.encryption.pintimeout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import kotlinx.coroutines.launch

abstract class PinTimeoutViewModel: ViewModel() {

    abstract fun back()

}

class PinTimeoutViewModelImpl(
    private val navigation: SettingsNavigation
): PinTimeoutViewModel() {

    override fun back() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}