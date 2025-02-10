package com.kieronquinn.app.utag.ui.screens.settings.location.widgetfrequency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import kotlinx.coroutines.launch

abstract class WidgetFrequencyViewModel: ViewModel() {

    abstract fun back()

}

class WidgetFrequencyViewModelImpl(
    private val navigation: SettingsNavigation
): WidgetFrequencyViewModel() {

    override fun back() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}