package com.kieronquinn.app.utag.ui.screens.tag.lostmode.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import kotlinx.coroutines.launch

abstract class LostModeGuideViewModel: ViewModel() {

    abstract fun onCancelClicked()
    abstract fun onNextClicked()

}

class LostModeGuideViewModelImpl(
    private val navigation: TagMoreNavigation,
    private val deviceId: String,
    private val deviceName: String
): LostModeGuideViewModel() {

    override fun onCancelClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onNextClicked() {
        viewModelScope.launch {
            navigation.navigate(LostModeGuideFragmentDirections
                .actionLostModeGuideFragmentToLostModeSettingsFragment(deviceId, deviceName))
        }
    }

}