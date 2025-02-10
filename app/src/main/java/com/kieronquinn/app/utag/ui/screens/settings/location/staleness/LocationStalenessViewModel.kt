package com.kieronquinn.app.utag.ui.screens.settings.location.staleness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import kotlinx.coroutines.launch

abstract class LocationStalenessViewModel: ViewModel() {

    abstract fun back()

}

class LocationStalenessViewModelImpl(
    private val navigation: SettingsNavigation
): LocationStalenessViewModel() {

    override fun back() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}