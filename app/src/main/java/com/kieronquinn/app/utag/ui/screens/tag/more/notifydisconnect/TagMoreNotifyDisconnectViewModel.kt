package com.kieronquinn.app.utag.ui.screens.tag.more.notifydisconnect

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.SafeAreaRepository
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea.Location
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea.WiFi
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.WarningAction
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.WarningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TagMoreNotifyDisconnectViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onWarningActionClicked(action: WarningAction)
    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onShowImageChanged(enabled: Boolean)
    abstract fun onSafeAreaChanged(safeArea: SafeArea, enabled: Boolean)
    abstract fun onSafeAreaClicked(safeArea: SafeArea)
    abstract fun onAddSafeAreaClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val warning: WarningState?,
            val locationSafeAreas: List<Location>,
            val wifiSafeAreas: List<WiFi>,
            val showImage: Boolean,
            val areBiometricsEnabled: Boolean
        ): State()
    }

}

class TagMoreNotifyDisconnectViewModelImpl(
    private val safeAreaRepository: SafeAreaRepository,
    private val navigation: TagMoreNavigation,
    private val deviceId: String,
    settings: EncryptedSettingsRepository
): TagMoreNotifyDisconnectViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val enabled = safeAreaRepository.isNotifyDisconnectEnabled(deviceId)
    private val showImage = safeAreaRepository.isShowImageEnabled(deviceId)
    private val biometrics = settings.biometricPromptEnabled.asFlow()
    private val safeAreas = safeAreaRepository.getSafeAreas()

    private val warning = resumeBus.mapLatest {
        safeAreaRepository.getWarningState()
    }

    override val state = combine(
        enabled,
        safeAreas,
        warning,
        showImage,
        biometrics
    ) { enabled, safeAreas, warning, showImage, biometrics ->
        State.Loaded(
            enabled,
            warning,
            safeAreas.filterIsInstance<Location>().sortedBy { it.name.lowercase() },
            safeAreas.filterIsInstance<WiFi>().sortedBy { it.name.lowercase() },
            showImage,
            biometrics
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onWarningActionClicked(action: WarningAction) {
        val intent = when(action) {
            WarningAction.NOTIFICATION_SETTINGS -> {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                }
            }
            WarningAction.NOTIFICATION_CHANNEL_SETTINGS -> {
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                    putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannel.LEFT_BEHIND.id)
                }
            }
        }
        viewModelScope.launch {
            navigation.navigate(intent)
        }
    }

    override fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            safeAreaRepository.setNotifyDisconnectEnabled(deviceId, enabled)
        }
    }

    override fun onShowImageChanged(enabled: Boolean) {
        viewModelScope.launch {
            safeAreaRepository.setShowImageEnabled(deviceId, enabled)
        }
    }

    override fun onSafeAreaChanged(safeArea: SafeArea, enabled: Boolean) {
        viewModelScope.launch {
            val activeDeviceIds = if(enabled) {
                safeArea.activeDeviceIds.plus(deviceId)
            }else{
                safeArea.activeDeviceIds.minus(deviceId)
            }
            safeAreaRepository.updateSafeArea(safeArea.copyWithDeviceIds(activeDeviceIds))
        }
    }

    override fun onSafeAreaClicked(safeArea: SafeArea) {
        viewModelScope.launch {
            when(safeArea) {
                is Location -> {
                    navigation.navigate(
                        TagMoreNotifyDisconnectFragmentDirections
                            .actionTagMoreNotifyDisconnectFragmentToSafeAreaLocationFragment2(
                                safeArea.id, deviceId, false
                            )
                    )
                }
                is WiFi -> {
                    navigation.navigate(
                        TagMoreNotifyDisconnectFragmentDirections
                            .actionTagMoreNotifyDisconnectFragmentToSafeAreaWiFiFragment2(
                                safeArea.id, deviceId, false
                            )
                    )
                }
            }
        }
    }

    override fun onAddSafeAreaClicked() {
        viewModelScope.launch {
            navigation.navigate(
                TagMoreNotifyDisconnectFragmentDirections
                    .actionTagMoreNotifyDisconnectFragmentToSafeAreaTypeFragment2(deviceId)
            )
        }
    }

}