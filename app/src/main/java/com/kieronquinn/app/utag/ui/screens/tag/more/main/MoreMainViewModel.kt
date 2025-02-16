package com.kieronquinn.app.utag.ui.screens.tag.more.main

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.utag.components.navigation.TagContainerNavigation
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.model.DeviceInfo
import com.kieronquinn.app.utag.model.EncryptionKey
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptionRepository
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository
import com.kieronquinn.app.utag.repositories.PassiveModeRepository
import com.kieronquinn.app.utag.repositories.SafeAreaRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState.Loaded.LocationState
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.ui.screens.tag.more.container.MoreContainerFragmentDirections
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.PinEntryResult
import com.kieronquinn.app.utag.utils.extensions.isLoadingDelayed
import com.kieronquinn.app.utag.utils.extensions.toLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class MoreMainViewModel: ViewModel() {

    abstract val events: Flow<Event>
    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onBackPressed()
    abstract fun onNearbyClicked()
    abstract fun onLocationHistoryClicked()
    abstract fun onShareableChanged(enabled: Boolean)
    abstract fun onNotifyChanged(enabled: Boolean)
    abstract fun onNotifyDisconnectChanged(enabled: Boolean)
    abstract fun onNotifyDisconnectClicked()
    abstract fun onShareableUserChanged(enabled: Boolean)
    abstract fun onLostModeClicked()
    abstract fun one2eChanged(enabled: Boolean)
    abstract fun one2eWikiClicked()
    abstract fun onBatteryClicked()
    abstract fun showPinEntry()
    abstract fun onPinCancelled()
    abstract fun onPinEntered(pinEntryResult: PinEntryResult.Success)
    abstract fun onFindDeviceClicked()
    abstract fun onFindDeviceChanged(enabled: Boolean)
    abstract fun onAutomationsClicked()
    abstract fun onPassiveModeClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val navigationLocation: LatLng?,
            val lostModeState: LostModeRequestResponse?,
            val notifyDisconnectEnabled: Boolean,
            val notifyDisconnectHasWarning: Boolean,
            val safeAreaCount: Int,
            val deviceInfo: DeviceInfo,
            val isSending: Boolean,
            val isConnected: Boolean,
            val isScannedOrConnected: Boolean,
            val e2eAvailable: Boolean?,
            val e2eEnabled: Boolean?,
            val isCloseBy: Boolean,
            val findDeviceEnabled: Boolean,
            val findDeviceHasWarning: Boolean,
            val passiveModeEnabled: Boolean,
            val requiresAgreement: Boolean,
            val offline: Boolean,
            val region: String?,
            val swapLocationHistory: Boolean
        ): State()
        data class PINRequired(val deviceInfo: DeviceInfo): State()
        data object Error: State()
    }

    enum class Event {
        ERROR, SHAREABLE_ENABLED, NOTIFY_ENABLED
    }

}

class MoreMainViewModelImpl(
    private val containerNavigation: TagContainerNavigation,
    private val navigation: TagMoreNavigation,
    private val apiRepository: ApiRepository,
    private val encryptionRepository: EncryptionRepository,
    private val deviceId: String,
    private val smartTagRepository: SmartTagRepository,
    private val findDeviceRepository: FindMyDeviceRepository,
    private val safeAreaRepository: SafeAreaRepository,
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    settingsRepository: SettingsRepository,
    smartThingsRepository: SmartThingsRepository,
    passiveModeRepository: PassiveModeRepository,
    authRepository: AuthRepository
): MoreMainViewModel() {

    companion object {
        private const val MINIMUM_DISTANCE_FOR_NAVIGATE = 250f //250m
        private const val LINK_WIKI = "https://kieronquinn.co.uk/redirect/uTag/wiki/encryption"
    }

    private val bleRefreshBus = MutableStateFlow(System.currentTimeMillis())
    private val refreshBus = MutableStateFlow(System.currentTimeMillis())
    private val tagConnection = smartTagRepository.getTagConnection(deviceId)
    private val isSending = MutableStateFlow(false)
    private val isBleSending = MutableStateFlow(false)
    private val isLoading = isLoadingDelayed()

    private var pinState = PinState.IDLE
    private var isPinError = false

    private val tagState = smartTagRepository.getTagState(deviceId).onEach {
        isLoading.emit(false)
    }

    private val lostMode = refreshBus.mapLatest {
        apiRepository.getLostMode(deviceId)
    }

    private val location = refreshBus.mapLatest {
        smartThingsRepository.getLocation()
    }

    private val consentInfo = flow {
        emit(authRepository.getConsentDetails())
    }

    private val isConnected = tagConnection?.getIsConnected() ?: flowOf(false)
    private val isScanned = tagConnection?.getIsScannedOrConnected() ?: flowOf(false)

    private val e2eKey = refreshBus.mapLatest {
        apiRepository.getEncryptionKey()
    }

    private val e2eEnabled = combine(
        isConnected,
        bleRefreshBus
    ) { isConnected, _ ->
        if(isConnected) {
            tagConnection?.isE2EEnabled()
        }else null
    }.onEach {
        isBleSending.emit(false)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val findDeviceEnabled = findDeviceRepository.getConfig(deviceId).mapLatest {
        it.enabled
    }

    private val findDeviceHasError = refreshBus.mapLatest {
        val errorState = findDeviceRepository.getErrorState(deviceId)
        errorState != null && errorState.isCritical
    }

    private val safeAreaCount = safeAreaRepository.getSafeAreas().map { areas ->
        areas.count { it.activeDeviceIds.contains(deviceId) }
    }

    private val notifyDisconnectEnabled = safeAreaRepository.isNotifyDisconnectEnabled(deviceId)

    private val notifyDisconnectError = refreshBus.mapLatest {
        safeAreaRepository.getWarningState()
    }

    private val notifyDisconnectAndSafeArea = combine(
        safeAreaCount,
        notifyDisconnectEnabled,
        notifyDisconnectError
    ) { safeAreaCount, notifyDisconnectEnabled, notifyDisconnectError ->
        val hasWarning = notifyDisconnectError != null && notifyDisconnectError.isCritical
        Triple(safeAreaCount, notifyDisconnectEnabled, hasWarning)
    }

    private val findOptions = combine(
        findDeviceHasError,
        findDeviceEnabled,
        settingsRepository.mapSwapLocationHistory.asFlow()
    ) { error, enabled, swapLocationHistory ->
        Triple(error, enabled, swapLocationHistory)
    }

    private val e2eAndRegion = combine(
        e2eKey,
        e2eEnabled,
        consentInfo
    ) { key, enabled, consentInfo ->
        val region = consentInfo?.region
        Triple(key, enabled, region)
    }

    private val options = combine(
        lostMode,
        e2eAndRegion,
        findOptions,
        notifyDisconnectAndSafeArea,
        passiveModeRepository.isInPassiveModeAsFlow(deviceId),
    ) { lost, e2eAndRegion, findDevice, notifyDisconnectAndSafeArea, passiveMode ->
        val e2eKey = e2eAndRegion.first
        val e2eEnabled = e2eAndRegion.second
        val region = e2eAndRegion.third
        val findAvailable = findDevice.first
        val findEnabled = findDevice.second
        val swapLocationHistory = findDevice.third
        Options(
            lost,
            e2eEnabled,
            e2eKey,
            findAvailable,
            findEnabled,
            notifyDisconnectAndSafeArea.second,
            notifyDisconnectAndSafeArea.first,
            notifyDisconnectAndSafeArea.third,
            passiveMode,
            region,
            swapLocationHistory
        )
    }

    override val events = MutableSharedFlow<Event>()

    private val loading = combine(isSending, isLoading, isBleSending) { options ->
        options.any { it == true }
    }

    private val scannedAndConnected = combine(
        isScanned,
        isConnected
    ) { scanned, connected ->
        Pair(scanned, connected)
    }

    override val state = combine(
        tagState,
        options,
        loading,
        scannedAndConnected,
        location
    ) { tagState, options, sending, scannedAndConnected, location ->
        val scannedOrConnected = scannedAndConnected.first
        val connected = scannedAndConnected.second
        val state = when(tagState) {
            is TagState.Loaded -> tagState
            is TagState.Error -> return@combine State.Error
        }
        val navigationLocation = when(state.locationState) {
            is LocationState.Location -> {
                //Reset PIN error if needed
                isPinError = false
                state.locationState.latLng
            }
            is LocationState.PINRequired -> return@combine State.PINRequired(state.device)
            else -> {
                //Reset PIN error if needed since the UI will be shown
                isPinError = false
                null //No Navigate option will be shown
            }
        }
        val isCloseBy = when(state.locationState) {
            is LocationState.Location -> state.locationState.isCloseBy(location)
            else -> false
        }
        val e2eAvailable = when(options.e2eKey) {
            is EncryptionKey.Set -> true
            is EncryptionKey.Unset -> false
            else -> null
        }
        State.Loaded(
            navigationLocation,
            options.lostModeState,
            options.notifyDisconnectEnabled,
            options.notifyDisconnectHasWarning,
            options.safeAreaCount,
            state.device,
            sending,
            connected,
            scannedOrConnected,
            e2eAvailable,
            options.e2eEnabled,
            isCloseBy,
            options.findDeviceEnabled,
            options.findDeviceHasWarning,
            options.isInPassiveMode,
            state.requiresAgreement(),
            state.locationState?.cached == true,
            options.region,
            options.swapLocationHistory
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            containerNavigation.navigateBack()
        }
    }

    private fun LocationState.Location.isCloseBy(to: Location?): Boolean {
        //If we don't have a GPS location, always assume we can navigate
        if(to == null) return true
        return latLng.toLocation().distanceTo(to) < MINIMUM_DISTANCE_FOR_NAVIGATE
    }

    override fun onNearbyClicked() {
        viewModelScope.launch {
            val deviceInfo = (state.value as? State.Loaded)?.deviceInfo ?: return@launch
            navigation.navigate(MoreMainFragmentDirections
                .actionMoreMainFragmentToTagMoreNearbyFragment(
                    deviceId, deviceInfo.label, deviceInfo.supportsUwb, deviceInfo.icon
                ))
        }
    }

    override fun onLocationHistoryClicked() {
        viewModelScope.launch {
            val deviceInfo = (state.value as? State.Loaded)?.deviceInfo ?: return@launch
            containerNavigation.navigate(
                MoreContainerFragmentDirections.actionMoreContainerFragmentToTagLocationHistoryFragment(
                    deviceInfo.deviceId, deviceInfo.label, deviceInfo.isOwner
                )
            )
        }
    }

    override fun onShareableChanged(enabled: Boolean) {
        viewModelScope.launch {
            isSending.emit(true)
            if(apiRepository.setShareable(deviceId, enabled)) {
                if(enabled) {
                    events.emit(Event.SHAREABLE_ENABLED)
                }
                smartTagRepository.refreshTagStates()
                isLoading.emit(true)
            }else{
                events.emit(Event.ERROR)
            }
            isSending.emit(false)
        }
    }

    override fun onNotifyChanged(enabled: Boolean) {
        viewModelScope.launch {
            isSending.emit(true)
            if(apiRepository.setSearching(deviceId, enabled)) {
                if(enabled) {
                    events.emit(Event.NOTIFY_ENABLED)
                }
                smartTagRepository.refreshTagStates()
                isLoading.emit(true)
            }else{
                events.emit(Event.ERROR)
            }
            isSending.emit(false)
        }
    }

    override fun onNotifyDisconnectChanged(enabled: Boolean) {
        viewModelScope.launch {
            safeAreaRepository.setNotifyDisconnectEnabled(deviceId, enabled)
        }
    }

    override fun onNotifyDisconnectClicked() {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            navigation.navigate(
                MoreMainFragmentDirections.actionMoreMainFragmentToTagMoreNotifyDisconnectFragment(
                    state.deviceInfo.deviceId, state.deviceInfo.label
                )
            )
        }
    }

    override fun onShareableUserChanged(enabled: Boolean) {
        viewModelScope.launch {
            isSending.emit(true)
            val userId = encryptedSettingsRepository.userId.get()
            val result = if(enabled) {
                apiRepository.setShareableMembers(deviceId, userId)
            }else{
                apiRepository.stopSharing(deviceId)
            }
            if(result) {
                smartTagRepository.refreshTagStates()
                isLoading.emit(true)
            }else{
                events.emit(Event.ERROR)
            }
            isSending.emit(false)
        }
    }

    override fun onLostModeClicked() {
        val state = state.value as? State.Loaded ?: return
        val lostModeEnabled = state.lostModeState?.enabled ?: return
        val directions = if(lostModeEnabled) {
            MoreMainFragmentDirections.actionMoreMainFragmentToLostModeSettingsFragment(
                state.deviceInfo.deviceId,
                state.deviceInfo.label
            )
        }else{
            MoreMainFragmentDirections.actionMoreMainFragmentToLostModeGuideFragment(
                state.isScannedOrConnected,
                state.deviceInfo.deviceId,
                state.deviceInfo.label
            )
        }
        viewModelScope.launch {
            navigation.navigate(directions)
        }
    }

    override fun one2eChanged(enabled: Boolean) {
        viewModelScope.launch {
            if((state.value as? State.Loaded)?.offline == true) {
                //Don't even try to update since this can cause out-of-sync values
                events.emit(Event.ERROR)
                return@launch
            }
            isBleSending.emit(true)
            if(tagConnection?.setE2EEnabled(enabled) != true) {
                //Failed to set flag, abort
                events.emit(Event.ERROR)
                isBleSending.emit(false)
                return@launch
            }
            isSending.emit(true)
            if(apiRepository.setE2eEnabled(deviceId, enabled)) {
                smartTagRepository.refreshTagStates()
                isLoading.emit(true)
            }else{
                //Try to reset the value back on the tag
                tagConnection.setE2EEnabled(!enabled)
                events.emit(Event.ERROR)
            }
            bleRefreshBus.emit(System.currentTimeMillis())
            isSending.emit(false)
        }
    }

    override fun one2eWikiClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_WIKI)
        }
    }

    override fun onBatteryClicked() {
        viewModelScope.launch {
            val level = (state.value as? State.Loaded)?.deviceInfo?.batteryLevel ?: return@launch
            navigation.navigate(
                MoreMainFragmentDirections.actionMoreMainFragmentToTagMoreBatteryDialogFragment(level)
            )
        }
    }

    override fun showPinEntry() {
        if(pinState != PinState.IDLE) return
        pinState = PinState.SHOWING
        val deviceInfo = (state.value as? State.PINRequired)?.deviceInfo ?: return
        viewModelScope.launch {
            navigation.navigate(MoreMainFragmentDirections.actionMoreMainFragmentToTagPinEntryDialogFragment2(
                deviceInfo.label, isPinError, false
            ))
        }
    }

    override fun onPinCancelled() {
        viewModelScope.launch {
            smartTagRepository.setPINSuppressed(true)
        }
    }

    override fun onPinEntered(pinEntryResult: PinEntryResult.Success) {
        viewModelScope.launch {
            encryptionRepository.setPIN(pinEntryResult.pin, pinEntryResult.save)
            //If PIN is shown again, show the error message
            isPinError = true
            pinState = PinState.WAITING
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    override fun onFindDeviceClicked() {
        val deviceInfo = (state.value as? State.Loaded)?.deviceInfo ?: return
        viewModelScope.launch {
            navigation.navigate(MoreMainFragmentDirections
                .actionMoreMainFragmentToTagMoreFindDeviceFragment(
                    deviceId, deviceInfo.label, deviceInfo.shareable
                ))
        }
    }

    override fun onFindDeviceChanged(enabled: Boolean) {
        viewModelScope.launch {
            findDeviceRepository.updateConfig(deviceId) {
                copy(enabled = enabled)
            }
        }
    }

    override fun onAutomationsClicked() {
        val deviceInfo = (state.value as? State.Loaded)?.deviceInfo ?: return
        viewModelScope.launch {
            navigation.navigate(
                MoreMainFragmentDirections.actionMoreMainFragmentToTagMoreAutomationFragment(
                    deviceId, deviceInfo.label, deviceInfo.shareable
                )
            )
        }
    }

    override fun onPassiveModeClicked() {
        val deviceInfo = (state.value as? State.Loaded)?.deviceInfo ?: return
        viewModelScope.launch {
            navigation.navigate(
                MoreMainFragmentDirections.actionMoreMainFragmentToTagMorePassiveModeFragment(
                    deviceId, deviceInfo.label
                )
            )
        }
    }

    enum class PinState {
        IDLE, SHOWING, WAITING
    }

    private data class Options(
        val lostModeState: LostModeRequestResponse?,
        val e2eEnabled: Boolean?,
        val e2eKey: EncryptionKey?,
        val findDeviceHasWarning: Boolean,
        val findDeviceEnabled: Boolean,
        val notifyDisconnectEnabled: Boolean,
        val safeAreaCount: Int,
        val notifyDisconnectHasWarning: Boolean,
        val isInPassiveMode: Boolean,
        val region: String?,
        val swapLocationHistory: Boolean
    )

}