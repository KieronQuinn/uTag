package com.kieronquinn.app.utag.ui.screens.settings.location.refreshfrequency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import kotlinx.coroutines.launch

abstract class RefreshFrequencyViewModel: ViewModel() {

    abstract fun back()

}

class RefreshFrequencyViewModelImpl(
    private val navigation: SettingsNavigation
): RefreshFrequencyViewModel() {

    override fun back() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}