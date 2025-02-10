package com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.Application.Companion.LOSTMESSAGE_URL
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse.MessageType
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse.PredefinedMessage
import com.kieronquinn.app.utag.networking.model.smartthings.SetSearchingStatusRequest.SearchingStatus
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.PassiveModeRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class LostModeSettingsViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onResume()
    abstract fun onBackPressed()
    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onEmailChanged(email: String)
    abstract fun onPhoneNumberChanged(phoneNumber: String)
    abstract fun onPredefinedMessageChanged(message: PredefinedMessage)
    abstract fun onCustomUrlClicked()
    abstract fun onNotifyChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val lostModeState: LostModeRequestResponse,
            val email: String,
            val searchingStatus: SearchingStatus,
            val lostModeUrl: String?,
            val isCustom: Boolean,
            val isConnected: Boolean,
            val isScannedOrConnected: Boolean,
            val isSaving: Boolean,
            val contentCreatorModeEnabled: Boolean,
            val passiveModeEnabled: Boolean
        ): State()
        data object Error: State()
    }

    enum class Event {
        ERROR
    }

}

class LostModeSettingsViewModelImpl(
    private val apiRepository: ApiRepository,
    private val navigation: TagMoreNavigation,
    private val deviceId: String,
    private val deviceLabel: String,
    deviceRepository: DeviceRepository,
    userRepository: UserRepository,
    smartTagRepository: SmartTagRepository,
    settingsRepository: SettingsRepository,
    passiveModeRepository: PassiveModeRepository
): LostModeSettingsViewModel() {

    companion object {
        private const val SPACER = " "
    }

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())
    private val saveLock = Mutex()
    private val isSaving = MutableStateFlow(false)
    private val contentCreatorMode = settingsRepository.contentCreatorModeEnabled

    private val tagConnection = smartTagRepository.getTagConnection(deviceId)
    private val isConnected = tagConnection?.getIsConnected() ?: flowOf(false)
    private val isScannedOrConnected = tagConnection?.getIsScannedOrConnected() ?: flowOf(false)

    private val lostModeState = reloadBus.mapLatest {
        apiRepository.getLostMode(deviceId)
    }

    private val passiveModeEnabled = reloadBus.flatMapLatest {
        passiveModeRepository.isInPassiveModeAsFlow(deviceId)
    }

    private val isSearching = reloadBus.mapLatest {
        deviceRepository.getDeviceInfo(deviceId)?.searchingStatus
    }

    private val email = flow {
        emit(userRepository.getUserInfo()?.email)
    }

    private val url = combine(
        isConnected,
        reloadBus
    ) { _, _ ->
        tagConnection?.getLostModeUrl()
    }

    private val options = combine(
        email,
        url,
        contentCreatorMode.asFlow()
    ) { email, url, contentCreatorMode ->
        Triple(email, url, contentCreatorMode)
    }

    private val savingAndSearching = combine(
        isSaving,
        isSearching
    ) { saving, searching ->
        Pair(saving, searching)
    }

    override val state = combine(
        isScannedOrConnected,
        lostModeState,
        options,
        savingAndSearching,
        passiveModeEnabled
    ) { scannedOrConnected, state, options, savingAndSearching, passiveMode ->
        val saving = savingAndSearching.first
        val searching = savingAndSearching.second
        val email = options.first
        val url = options.second
        State.Loaded(
            lostModeState = state ?: return@combine State.Error,
            email = email ?: return@combine State.Error,
            searchingStatus = searching ?: return@combine State.Error,
            lostModeUrl = url,
            isCustom = isCustomUrl(url, state.message),
            isConnected = url != null,
            isScannedOrConnected = scannedOrConnected,
            isSaving = saving,
            contentCreatorModeEnabled = options.third,
            passiveModeEnabled = passiveMode
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val events = MutableSharedFlow<Event>()

    override fun onResume() {
        viewModelScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            updateState {
                copy(enabled = enabled)
            }
        }
    }

    override fun onEmailChanged(email: String) {
        viewModelScope.launch {
            updateState {
                copy(email = email)
            }
        }
    }

    override fun onPhoneNumberChanged(phoneNumber: String) {
        viewModelScope.launch {
            updateState {
                copy(phoneNumber = phoneNumber.takeIf { it.isNotEmpty() })
            }
        }
    }

    override fun onPredefinedMessageChanged(message: PredefinedMessage) {
        viewModelScope.launch {
            updateState {
                copy(
                    messageType = MessageType.PREDEFINED,
                    message = message.name
                )
            }
        }
    }

    override fun onCustomUrlClicked() {
        viewModelScope.launch {
            navigation.navigate(
                LostModeSettingsFragmentDirections
                    .actionLostModeSettingsFragmentToLostModeCustomURLFragment(
                        deviceId, deviceLabel
                    )
            )
        }
    }

    override fun onNotifyChanged(enabled: Boolean) {
        viewModelScope.launch {
            isSaving.emit(true)
            if(apiRepository.setSearching(deviceId, true)) {
                reloadBus.emit(System.currentTimeMillis())
            }else{
                events.emit(Event.ERROR)
            }
            isSaving.emit(false)
        }
    }

    private suspend fun updateState(
        block: LostModeRequestResponse.() -> LostModeRequestResponse
    ) = saveLock.withLock {
        val current = (state.value as? State.Loaded)?.lostModeState ?: return@withLock
        val userEmail = (state.value as? State.Loaded)?.email ?: return@withLock
        isSaving.emit(true)
        val updated = block(current).setDefaultsIfNeeded(userEmail)
        if(apiRepository.setLostMode(deviceId, updated)) {
            reloadBus.emit(System.currentTimeMillis())
        }else{
            events.emit(Event.ERROR)
        }
        isSaving.emit(false)
    }

    private fun LostModeRequestResponse.setDefaultsIfNeeded(
        defaultEmail: String
    ): LostModeRequestResponse {
        if(!enabled) return this
        return copy(
            email = email?.takeIf { it.isNotBlank() } ?: defaultEmail,
            message = when {
                //Set the default predefined message if needed
                messageType == MessageType.PREDEFINED && message.isNullOrBlank() -> {
                    PredefinedMessage.DREAM_SACP_BODY_THIS_SMARTTAG_HAS_BEEN_LOST.name
                }
                else -> message
            }
        )
    }

    /**
     *  If the Tag has a custom URL set (preferred) or if the lost message is set to custom
     *  URL data (saved). This isn't perfect - it's possible to break by modifying on another device,
     *  but only after ignoring warnings.
     */
    private fun isCustomUrl(tagUrl: String?, remoteMessage: String?): Boolean {
        return (tagUrl != null && !tagUrl.startsWith(LOSTMESSAGE_URL)) ||
                (remoteMessage != null && remoteMessage.contains(SPACER))
    }

}