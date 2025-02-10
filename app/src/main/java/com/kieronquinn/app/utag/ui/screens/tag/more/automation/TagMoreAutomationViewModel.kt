package com.kieronquinn.app.utag.ui.screens.tag.more.automation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.model.ButtonVolumeLevel
import com.kieronquinn.app.utag.model.database.AutomationConfig
import com.kieronquinn.app.utag.repositories.AutomationRepository
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.utils.extensions.isLoadingDelayed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class TagMoreAutomationViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onResume()
    abstract fun onBackPressed()
    abstract fun onPressStateChanged(enabled: Boolean)
    abstract fun onPressClicked()
    abstract fun onPressChanged(intent: Intent)
    abstract fun onHoldStateChanged(enabled: Boolean)
    abstract fun onHoldClicked()
    abstract fun onHoldChanged(intent: Intent)
    abstract fun onWarningDismissed()
    abstract fun onButtonVolumeChanged(volumeLevel: ButtonVolumeLevel)

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val deviceId: String,
            val deviceLabel: String,
            val config: AutomationConfig,
            val hasOverlayPermission: Boolean,
            val maybeRequiresRestrictedSettingsPrompt: Boolean,
            val pressTitle: CharSequence?,
            val holdTitle: CharSequence?,
            val showSharedWarningDialog: Boolean,
            val buttonVolumeLevel: ButtonVolumeLevel,
            val isSending: Boolean
        ): State()
        data object Error: State()
    }

    enum class Event {
        ERROR
    }

}

class TagMoreAutomationViewModelImpl(
    private val automationRepository: AutomationRepository,
    private val navigation: TagMoreNavigation,
    settingsRepository: SettingsRepository,
    smartTagRepository: SmartTagRepository,
    deviceId: String,
    deviceLabel: String,
    sharedTag: Boolean
): TagMoreAutomationViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val refreshBus = MutableStateFlow(System.currentTimeMillis())
    private val isSending = MutableStateFlow(false)
    private val isLoading = isLoadingDelayed()
    private val sendLock = Mutex()
    private val config = automationRepository.getAutomationConfigAsFlow(deviceId)
    private val hasSeenAutomationWarning = settingsRepository.hasSeenAutomationWarning
    private val tagConnection = smartTagRepository.getTagConnection(deviceId)
    private var hasSynced = false

    private val ruleSyncResult = resumeBus.mapLatest {
        automationRepository.syncRemoteRuleStates()
    }

    private val hasOverlayPermission = resumeBus.mapLatest {
        automationRepository.hasOverlayPermission()
    }

    private val maybeRequiresRestrictedSettingsPrompt = resumeBus.mapLatest {
        automationRepository.maybeRequiresRestrictedSettingsPrompt()
    }

    private val options = combine(
        hasOverlayPermission,
        maybeRequiresRestrictedSettingsPrompt,
        hasSeenAutomationWarning.asFlow()
    ) { overlay, prompt, hasSeenAutomationWarning ->
        Triple(overlay, prompt, hasSeenAutomationWarning)
    }

    private val buttonVolume = refreshBus.mapLatest {
        tagConnection?.getButtonVolume()
    }.onEach {
        isLoading.emit(false)
    }

    private val loading = combine(
        isLoading,
        isSending
    ) { loading, sending ->
        loading == true || sending
    }

    override val state = combine(
        config,
        ruleSyncResult,
        options,
        buttonVolume,
        loading
    ) { config, syncSuccess, options, buttonVolume, sending ->
        val hasOverlayPermission = options.first
        val prompt = options.second
        val hasSeenWarning = options.third
        if(syncSuccess && buttonVolume != null) {
            val pressTitle = automationRepository.resolveIntentTitle(config, TagButtonAction.PRESS)
            val holdTitle = automationRepository.resolveIntentTitle(config, TagButtonAction.HOLD)
            State.Loaded(
                deviceId,
                deviceLabel,
                config,
                hasOverlayPermission,
                prompt,
                pressTitle,
                holdTitle,
                sharedTag && !hasSeenWarning,
                buttonVolume,
                sending
            )
        }else State.Error
    }.onEach {
        //Force a sync on first load
        if(!hasSynced && it is State.Loaded) {
            hasSynced = true
            updateConfig(it.deviceId, it.config)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val events = MutableSharedFlow<Event>()

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onPressClicked() {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            when {
                state.hasOverlayPermission -> {
                    navigation.navigate(TagMoreAutomationFragmentDirections
                        .actionTagMoreAutomationFragmentToTagMoreAutomationTypeFragment(
                            state.deviceLabel, TagButtonAction.PRESS
                        ))
                }
                state.maybeRequiresRestrictedSettingsPrompt -> {
                    navigation.navigate(TagMoreAutomationFragmentDirections
                        .actionTagMoreAutomationFragmentToTagMoreAutomationPermissionFragment())
                }
                else -> {
                    navigation.navigate(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                }
            }
        }
    }

    override fun onPressStateChanged(enabled: Boolean) {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            val newConfig = state.config.copy(pressEnabled = enabled)
            updateConfig(state.deviceId, newConfig)
        }
    }

    override fun onPressChanged(intent: Intent) {
        val state = state.value as? State.Loaded ?: return
        val intentUri = intent.toUri(0)
        viewModelScope.launch {
            val newConfig = state.config.copy(pressEnabled = true, pressIntent = intentUri)
            updateConfig(state.deviceId, newConfig)
        }
    }

    override fun onHoldClicked() {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            when {
                state.hasOverlayPermission -> {
                    navigation.navigate(TagMoreAutomationFragmentDirections
                        .actionTagMoreAutomationFragmentToTagMoreAutomationTypeFragment(
                            state.deviceLabel, TagButtonAction.HOLD
                        ))
                }
                state.maybeRequiresRestrictedSettingsPrompt -> {
                    navigation.navigate(TagMoreAutomationFragmentDirections
                        .actionTagMoreAutomationFragmentToTagMoreAutomationPermissionFragment())
                }
                else -> {
                    navigation.navigate(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                }
            }
        }
    }

    override fun onHoldStateChanged(enabled: Boolean) {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            val newConfig = state.config.copy(holdEnabled = enabled)
            updateConfig(state.deviceId, newConfig)
        }
    }

    override fun onHoldChanged(intent: Intent) {
        val state = state.value as? State.Loaded ?: return
        val intentUri = intent.toUri(0)
        viewModelScope.launch {
            val newConfig = state.config.copy(holdEnabled = true, holdIntent = intentUri)
            updateConfig(state.deviceId, newConfig)
        }
    }

    override fun onWarningDismissed() {
        viewModelScope.launch {
            hasSeenAutomationWarning.set(true)
        }
    }

    override fun onButtonVolumeChanged(volumeLevel: ButtonVolumeLevel) {
        viewModelScope.launch {
            isSending.emit(true)
            if(tagConnection?.setButtonVolume(volumeLevel) != true) {
                events.emit(Event.ERROR)
            }else{
                isLoading.emit(true)
            }
            isSending.emit(false)
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    private suspend fun updateConfig(
        deviceId: String,
        newConfig: AutomationConfig
    ) = sendLock.withLock {
        isSending.emit(true)
        //Send the config to the Tag first to make sure it works
        if(syncWithTag(newConfig)) {
            //Now update the database
            automationRepository.updateAutomationConfig(deviceId) {
                newConfig
            }
        }else{
            //Failed to send to Tag, don't update and show error Toast
            events.emit(Event.ERROR)
        }
        isSending.emit(false)
    }

    /**
     *  Sends button config based on **either** the custom press or remote press being enabled
     */
    private suspend fun syncWithTag(newConfig: AutomationConfig): Boolean {
        return tagConnection?.setButtonConfig(
            newConfig.pressEnabled || newConfig.pressRemoteEnabled,
            newConfig.holdEnabled || newConfig.holdRemoteEnabled
        ) ?: false
    }

}