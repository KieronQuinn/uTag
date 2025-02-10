package com.kieronquinn.app.utag.ui.screens.tag.more.automation.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.repositories.AutomationRepository
import kotlinx.coroutines.launch

abstract class TagMoreAutomationPermissionViewModel: ViewModel() {

    abstract fun onResume()

}

class TagMoreAutomationPermissionViewModelImpl(
    private val navigation: TagMoreNavigation,
    private val automationRepository: AutomationRepository
): TagMoreAutomationPermissionViewModel() {

    override fun onResume() {
        viewModelScope.launch {
            if(automationRepository.hasOverlayPermission()) {
                navigation.navigateBack()
            }
        }
    }

}