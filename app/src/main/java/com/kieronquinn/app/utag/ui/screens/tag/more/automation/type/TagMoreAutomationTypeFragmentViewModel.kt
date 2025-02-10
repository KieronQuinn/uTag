package com.kieronquinn.app.utag.ui.screens.tag.more.automation.type

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import kotlinx.coroutines.launch

abstract class TagMoreAutomationTypeFragmentViewModel: ViewModel() {

    abstract fun onAppClicked(label: String, action: TagButtonAction)
    abstract fun onShortcutClicked(label: String, action: TagButtonAction)
    abstract fun onTaskerClicked(label: String, action: TagButtonAction)
    abstract fun back()

}

class TagMoreAutomationTypeFragmentViewModelImpl(
    private val navigation: TagMoreNavigation
): TagMoreAutomationTypeFragmentViewModel() {


    override fun onAppClicked(label: String, action: TagButtonAction) {
        viewModelScope.launch {
            navigation.navigate(TagMoreAutomationTypeFragmentDirections
                .actionTagMoreAutomationTypeFragmentToTagMoreAutomationAppPickerFragment(label, action))
        }
    }

    override fun onShortcutClicked(label: String, action: TagButtonAction) {
        viewModelScope.launch {
            navigation.navigate(TagMoreAutomationTypeFragmentDirections
                .actionTagMoreAutomationTypeFragmentToTagMoreAutomationShortcutPickerFragment(label, action))
        }
    }

    override fun onTaskerClicked(label: String, action: TagButtonAction) {
        viewModelScope.launch {
            navigation.navigate(TagMoreAutomationTypeFragmentDirections
                .actionTagMoreAutomationTypeFragmentToTagMoreAutomationTaskerFragment(label, action))
        }
    }

    override fun back() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}