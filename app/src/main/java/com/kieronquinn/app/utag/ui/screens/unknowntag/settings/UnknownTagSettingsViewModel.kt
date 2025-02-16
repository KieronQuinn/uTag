package com.kieronquinn.app.utag.ui.screens.unknowntag.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.UtsSensitivity
import com.kieronquinn.app.utag.repositories.NotificationRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class UnknownTagSettingsViewModel: ViewModel() {
    
    abstract val state: StateFlow<State>
    
    abstract fun onResume()
    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onSensitivityChanged(sensitivity: UtsSensitivity)
    abstract fun onViewUnknownTagsClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(val enabled: Boolean, val sensitivity: UtsSensitivity): State()
    }

}

class UnknownTagSettingsViewModelImpl(
    private val navigation: SettingsNavigation,
    private val notificationRepository: NotificationRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository
): UnknownTagSettingsViewModel() {
    
    private val utsScanEnabled = encryptedSettingsRepository.utsScanEnabled
    private val utsSensitivity = encryptedSettingsRepository.utsSensitivity
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    override val state = combine(
        utsScanEnabled.asFlow(),
        utsSensitivity.asFlow()
    ) { enabled, sensitivity ->
        State.Loaded(enabled, sensitivity)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            utsScanEnabled.set(enabled)
            if(!enabled) {
                //Cancel notification if it's showing
                notificationRepository.cancelNotification(NotificationId.UNKNOWN_TAG)
            }
        }
    }

    override fun onSensitivityChanged(sensitivity: UtsSensitivity) {
        viewModelScope.launch {
            utsSensitivity.set(sensitivity)
        }
    }

    override fun onViewUnknownTagsClicked() {
        viewModelScope.launch {
            navigation.navigate(UnknownTagSettingsFragmentDirections
                .actionUnknownTagSettingsFragmentToNavGraphIncludeUnknownTag())
        }
    }
    
}