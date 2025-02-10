package com.kieronquinn.app.utag.ui.screens.settings.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.GeocoderRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsAdvancedViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onContentCreatorModeClicked()
    abstract fun onAnalyticsChanged(enabled: Boolean)
    abstract fun onAutoDismissNotificationsChanged(enabled: Boolean)
    abstract fun onPreventOvermatureChanged(enabled: Boolean)
    abstract fun onDebugChanged(enabled: Boolean)
    abstract fun onLanguageClicked()
    abstract fun onAddressCacheCleared()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val contentCreatorMode: Boolean,
            val analyticsEnabled: Boolean,
            val autoDismissNotifications: Boolean,
            val preventOvermature: Boolean,
            val debugVisible: Boolean,
            val debugEnabled: Boolean
        ): State()
    }

}

class SettingsAdvancedViewModelImpl(
    private val navigation: SettingsNavigation,
    private val geocoderRepository: GeocoderRepository,
    settingsRepository: SettingsRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository
): SettingsAdvancedViewModel() {

    private val analyticsEnabled = settingsRepository.analyticsEnabled
    private val contentCreatorMode = settingsRepository.contentCreatorModeEnabled
    private val autoDismissNotifications = settingsRepository.autoDismissNotifications
    private val preventOvermature = encryptedSettingsRepository.overmatureOfflinePreventionEnabled
    private val debugVisible = encryptedSettingsRepository.debugModeVisible
    private val debugEnabled = encryptedSettingsRepository.debugModeEnabled

    override val state = combine(
        contentCreatorMode.asFlow(),
        analyticsEnabled.asFlow(),
        autoDismissNotifications.asFlow(),
        preventOvermature.asFlow(),
        debugVisible.asFlow(),
        debugEnabled.asFlow()
    ) {
        State.Loaded(it[0], it[1], it[2], it[3], it[4], it[5])
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onContentCreatorModeClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsAdvancedFragmentDirections
                .actionSettingsAdvancedFragmentToSettingsContentCreatorFragment())
        }
    }

    override fun onAnalyticsChanged(enabled: Boolean) {
        viewModelScope.launch {
            analyticsEnabled.set(enabled)
        }
    }

    override fun onAutoDismissNotificationsChanged(enabled: Boolean) {
        viewModelScope.launch {
            autoDismissNotifications.set(enabled)
        }
    }

    override fun onPreventOvermatureChanged(enabled: Boolean) {
        viewModelScope.launch {
            preventOvermature.set(enabled)
        }
    }

    override fun onDebugChanged(enabled: Boolean) {
        viewModelScope.launch {
            debugEnabled.set(enabled)
        }
    }

    override fun onLanguageClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsAdvancedFragmentDirections
                .actionSettingsAdvancedFragmentToSettingsLanguageFragment()
            )
        }
    }

    override fun onAddressCacheCleared() {
        viewModelScope.launch {
            geocoderRepository.clearCache()
        }
    }

}