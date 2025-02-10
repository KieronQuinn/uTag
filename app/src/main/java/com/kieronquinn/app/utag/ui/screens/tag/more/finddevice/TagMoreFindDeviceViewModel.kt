package com.kieronquinn.app.utag.ui.screens.tag.more.finddevice

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.model.database.FindMyDeviceConfig
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TagMoreFindDeviceViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onResume()
    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onVibrateChanged(enabled: Boolean)
    abstract fun onDelayChanged(enabled: Boolean)
    abstract fun onVolumeChanged(volume: Float)
    abstract fun onFindMyDeviceWarningDismissed()
    abstract fun onNotificationSettingsClicked()
    abstract fun onNotificationChannelSettingsClicked()
    abstract fun onFullScreenIntentSettingsClicked()
    abstract fun onDisableSmartThingsActionsClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val errorState: FindMyDeviceRepository.ErrorState?,
            val config: FindMyDeviceConfig,
            val showWarning: Boolean
        ): State()
    }

    enum class Event {
        NETWORK_ERROR
    }

}

class TagMoreFindDeviceViewModelImpl(
    private val findMyDeviceRepository: FindMyDeviceRepository,
    private val apiRepository: ApiRepository,
    private val navigation: TagMoreNavigation,
    private val deviceId: String,
    settingsRepository: SettingsRepository,
    isSharedDevice: Boolean
): TagMoreFindDeviceViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val config = findMyDeviceRepository.getConfig(deviceId)
    private val hasSeenFindMyDeviceWarning = settingsRepository.hasSeenFindMyDeviceWarning

    private val errorState = resumeBus.mapLatest {
        findMyDeviceRepository.getErrorState(deviceId)
    }.flowOn(Dispatchers.IO)

    override val state = combine(
        errorState,
        config,
        hasSeenFindMyDeviceWarning.asFlow()
    ) { errorState, config, hasSeenWarning ->
        State.Loaded(errorState, config, !hasSeenWarning && isSharedDevice)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val events = MutableSharedFlow<Event>()

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            findMyDeviceRepository.updateConfig(deviceId) {
                copy(enabled = enabled)
            }
        }
    }

    override fun onVibrateChanged(enabled: Boolean) {
        viewModelScope.launch {
            findMyDeviceRepository.updateConfig(deviceId) {
                copy(vibrate = enabled)
            }
        }
    }

    override fun onVolumeChanged(volume: Float) {
        viewModelScope.launch {
            findMyDeviceRepository.updateConfig(deviceId) {
                copy(volume = volume)
            }
        }
    }

    override fun onDelayChanged(enabled: Boolean) {
        viewModelScope.launch {
            findMyDeviceRepository.updateConfig(deviceId) {
                copy(delay = enabled)
            }
        }
    }

    override fun onFindMyDeviceWarningDismissed() {
        viewModelScope.launch {
            hasSeenFindMyDeviceWarning.set(true)
        }
    }

    override fun onNotificationSettingsClicked() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
        }
        viewModelScope.launch {
            navigation.navigate(intent)
        }
    }

    override fun onNotificationChannelSettingsClicked() {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannel.FIND_DEVICE.id)
        }
        viewModelScope.launch {
            navigation.navigate(intent)
        }
    }

    @SuppressLint("InlinedApi")
    override fun onFullScreenIntentSettingsClicked() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        }
        viewModelScope.launch {
            navigation.navigate(intent)
        }
    }

    override fun onDisableSmartThingsActionsClicked() {
        viewModelScope.launch {
            if(apiRepository.disableSmartThingsButtonActions(deviceId)) {
                resumeBus.emit(System.currentTimeMillis())
            }else{
                events.emit(Event.NETWORK_ERROR)
            }
        }
    }

}