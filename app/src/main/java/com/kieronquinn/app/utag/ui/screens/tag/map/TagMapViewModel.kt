package com.kieronquinn.app.utag.ui.screens.tag.map

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.location.Location
import android.provider.Settings
import androidx.core.graphics.Insets
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.bluetooth.BaseTagConnection.SyncResult
import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection
import com.kieronquinn.app.utag.components.navigation.TagContainerNavigation
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.ContentCreatorRepository
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptionRepository
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository
import com.kieronquinn.app.utag.repositories.LocationRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.repositories.UTagServiceRepository
import com.kieronquinn.app.utag.ui.screens.tag.map.TagMapViewModel.State.Error.ErrorType
import com.kieronquinn.app.utag.ui.screens.tag.map.TagMapViewModelImpl.Options.Requirements
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.PinEntryResult
import com.kieronquinn.app.utag.utils.extensions.FirstResult
import com.kieronquinn.app.utag.utils.extensions.bluetoothEnabledAsFlow
import com.kieronquinn.app.utag.utils.extensions.first
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import com.kieronquinn.app.utag.utils.extensions.onConnectResult
import com.samsung.android.oneconnect.base.device.tag.TagConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

abstract class TagMapViewModel: ViewModel() {

    abstract val events: Flow<Event>
    abstract val state: StateFlow<State>

    abstract fun setSelectedDeviceId(deviceId: String)
    abstract fun onMapMoved()
    abstract fun onPickerClicked()
    abstract fun onCenterClicked()
    abstract fun onBluetoothEnable()
    abstract fun onResume()
    abstract fun onPause()
    abstract fun setInsets(insets: Insets)
    abstract fun onRefreshClicked()
    abstract fun setSearchingMode()
    abstract fun onRingClicked()
    abstract fun onLocationHistoryClicked()
    abstract fun onMoreClicked()
    abstract fun showPinEntry()
    abstract fun onPinCancelled()
    abstract fun onPinEntered(pinEntryResult: PinEntryResult.Success)
    abstract fun onAllowAccessClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val deviceId: String,
            val tagState: TagState.Loaded,
            val tagConnection: RemoteTagConnection,
            val isConnected: Boolean,
            val isScannedOrConnected: Boolean,
            val isRefreshing: Boolean,
            val showPicker: Boolean,
            val insets: Insets?,
            val mapOptions: MapOptions,
            val showAddToHome: Boolean,
            val otherTags: List<TagState.Loaded>,
            val knownTagIds: List<String>,
            val knownTagMembers: Map<String, String>?,
            val requiresMutualAgreement: Boolean,
            val region: String,
            val enableBluetoothPendingIntent: PendingIntent?
        ): State() {
            override fun isBusy() = isRefreshing
        }
        data class Error(val type: ErrorType): State() {
            sealed class ErrorType {
                data class Generic(val code: Int): ErrorType()
                data object NoTags: ErrorType()
                data object SmartThingsNotInstalled: ErrorType()
                data class ModuleNotActivated(val isUTagMod: Boolean): ErrorType()
                data class ModuleOutdated(val isUTagMod: Boolean): ErrorType()
                data class ModuleNewer(val isUTagMod: Boolean): ErrorType()
                data object Permissions: ErrorType()
            }
        }

        open fun isBusy() = true
    }

    sealed class Event {
        data class FailedToRefresh(val deviceLabel: String) : Event()
        data object NetworkError : Event()
        data object LocationError : Event()
        data class FailedToConnect(
            val deviceId: String,
            val deviceLabel: String,
            val reason: TagConnectionState
        ) : Event()
    }

    data class MapOptions(
        val shouldCenter: Boolean,
        val location: Location?,
        val style: MapStyle,
        val theme: MapTheme,
        val showBuildings: Boolean,
        val favouriteIds: List<String>?,
        val bluetoothEnabled: Boolean
    )

}

class TagMapViewModelImpl(
    private val smartTagRepository: SmartTagRepository,
    private val historyWidgetRepository: HistoryWidgetRepository,
    private val smartThingsRepository: SmartThingsRepository,
    private val navigation: TagContainerNavigation,
    private val encryptionRepository: EncryptionRepository,
    private val uTagServiceRepository: UTagServiceRepository,
    private val apiRepository: ApiRepository,
    private val authRepository: AuthRepository,
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    locationRepository: LocationRepository,
    settingsRepository: SettingsRepository,
    contentCreatorRepository: ContentCreatorRepository,
    deviceRepository: DeviceRepository,
    context: Context,
    initialDeviceId: String
): TagMapViewModel() {

    companion object {
        private const val REFRESH_TIME = 60_000L //60 seconds
        private const val REFRESH_SCAN_TIME = 30_000L //30 seconds
    }

    private val shouldCenter = MutableStateFlow(true)
    private val insets = MutableStateFlow<Insets?>(null)
    private val isResumed = MutableStateFlow(false)
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val refreshLock = Mutex()
    private var refreshJob: Job? = null

    private val shortcutManager =
        context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager

    private var pinState = PinState.IDLE
    private var isPinError = false

    private val isRefreshing = MutableStateFlow(false)
    private val lastSelectedDeviceId = encryptedSettingsRepository.lastSelectedDeviceIdOnMap
    private val deviceId = MutableStateFlow(initialDeviceId.takeIf { it.isNotBlank() })

    private val hasLocationPermissions = resumeBus.mapLatest {
        context.hasLocationPermissions()
    }

    private val fmmDevices = resumeBus.mapLatest {
        apiRepository.getDevices()
    }.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val favouriteIds = fmmDevices.mapLatest {
        it?.favourites?.mapNotNull { f -> f.stDid }
    }

    private val allTagIds = flow {
        emit(deviceRepository.getDeviceIds())
    }

    private val myTagIds = fmmDevices.mapLatest {
        val ownerId = it?.ownerId ?: return@mapLatest emptyList()
        it.devices.filter { d -> d.isTag() && d.stOwnerId == ownerId }.mapNotNull { d -> d.stDid }
    }

    private val users = flow {
        emit(locationRepository.getAllUsers())
    }

    private val enableBluetoothPendingIntent = flow {
        emit(smartThingsRepository.getEnableBluetoothIntent())
    }

    private val myLocation = combine(
        hasLocationPermissions,
        isResumed,
        settingsRepository.isMyLocationEnabled.asFlow()
    ) { permissions, resumed, setting ->
        if(permissions && resumed && setting) {
            contentCreatorRepository.wrapLocationAsFlow()
        }else{
            flowOf(null)
        }
    }.flattenConcat().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val bluetoothEnabled = context.bluetoothEnabledAsFlow(viewModelScope)

    private val mapStyle = combine(
        settingsRepository.mapStyle.asFlow(),
        settingsRepository.mapTheme.asFlow(),
        settingsRepository.mapShowBuildings.asFlow()
    ) { style, theme, buildings ->
        Triple(style, theme, buildings)
    }

    private val mapOptions = combine(
        shouldCenter,
        myLocation,
        mapStyle,
        favouriteIds,
        bluetoothEnabled
    ) { shouldCenter, location, mapStyle, favouriteIds, bluetooth ->
        val style = mapStyle.first
        val theme = mapStyle.second
        val buildings = mapStyle.third
        MapOptions(shouldCenter, location, style, theme, buildings, favouriteIds, bluetooth.enabled)
    }

    private val showAddToHome = resumeBus.mapLatest {
        shortcutManager.isRequestPinShortcutSupported && shortcutManager.pinnedShortcuts.none {
            it.id == "utag"
        }
    }

    private val tagConnections = flow {
        emit(smartTagRepository.createTagConnections())
    }.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val tagConnection = combine(
        tagConnections,
        deviceId,
        myTagIds,
        allTagIds
    ) { all, selected, myTagIds, allTagIds ->
        if(allTagIds == null) return@combine TagConnectionState.Error
        val selectedDeviceId = selected ?: lastSelectedDeviceId.get()
            .takeIf { allTagIds.contains(it) } ?: all?.firstOwnedOrFirstOrNull(myTagIds)
            ?: return@combine TagConnectionState.Error
        all?.get(selectedDeviceId)?.let { TagConnectionState.Loaded(selectedDeviceId, it) }
            ?: TagConnectionState.Error
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TagConnectionState.Loading)

    private fun Map<String, RemoteTagConnection>.firstOwnedOrFirstOrNull(
        myTagIds: List<String>
    ): String? {
        return keys.firstOrNull { myTagIds.contains(it) } ?: keys.firstOrNull()
    }

    private val isConnected = tagConnection.flatMapLatest {
        if(it is TagConnectionState.Loaded) {
            it.tagConnection.getIsConnected()
        } else flowOf(false)
    }

    private val isScannedOrConnected = tagConnection.flatMapLatest {
        if(it is TagConnectionState.Loaded) {
            it.tagConnection.getIsScannedOrConnected()
        } else flowOf(false)
    }

    private val tagConnectResult = uTagServiceRepository.service.flatMapLatest {
        it?.onConnectResult()?.filter { result -> result.second.isError() }?.mapNotNull { result ->
            val label = smartTagRepository.getKnownTagNames().first()[result.first.hashCode()]
                ?: return@mapNotNull null
            Triple(result.first, label, result.second)
        } ?: emptyFlow()
    }.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val userOptions = resumeBus.mapLatest {
        val options = apiRepository.getUserOptions() ?: return@mapLatest null
        val agreedTypes = options.fmmAgreementUrls.filter { it.agreed }.map { it.type }
        val hasAgreed = options.fmmAgreement &&
                agreedTypes.contains("service.pp") &&
                agreedTypes.contains("service.location")
        if(!hasAgreed) {
            if(apiRepository.setTermsAgreed()) {
                //Call a second time to keep updated
                apiRepository.getUserOptions()
            }else null
        }else{
            options
        }
    }

    private val consentInfo = flow {
        emit(authRepository.getConsentDetails())
    }

    private val requirements = flow {
        emit(
            Requirements(
                smartThingsRepository.getModuleState(),
                smartThingsRepository.hasRequiredPermissions()
            )
        )
    }

    //Enforce result of agreeing to terms first to prevent race conditions
    private val tagStates = combine(
        tagConnections,
        userOptions
    ) { connections, userOptions ->
        //null = failed to load
        if(connections == null) return@combine flowOf(TagStates.Error(2001))
        //Empty = no tags on account
        if(connections.isEmpty()) return@combine flowOf(TagStates.Loaded(emptyMap()))
        if(userOptions == null) return@combine flowOf(TagStates.Error(2002))
        val connectionFlows = connections.map {
            val deviceId = it.key
            smartTagRepository.getTagState(deviceId).onEach {
                if(pinState == PinState.WAITING) {
                    pinState = PinState.IDLE
                }
            }.map { state ->
                Pair(deviceId, state)
            }
        }.toTypedArray()
        combine(*connectionFlows) {
            TagStates.Loaded(it.toMap())
        }
    }.flattenConcat()

    private val isSyncing = tagConnection.flatMapLatest {
        (it as? TagConnectionState.Loaded)?.tagConnection?.getIsAutoSyncing() ?: flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val isRefreshingOrSyncing = combine(
        isRefreshing,
        isSyncing
    ) { refreshing, syncing ->
        refreshing || syncing
    }

    private val options = combine(
        insets,
        requirements,
        isRefreshingOrSyncing,
        showAddToHome,
        consentInfo
    ) { insets, requirements, isRefreshingOrSyncing, showAddToHome, consentInfo ->
        val region = consentInfo?.region ?: return@combine null
        Options(insets, requirements, isRefreshingOrSyncing, showAddToHome, region)
    }

    private val device = combine(
        isConnected,
        isScannedOrConnected,
        users,
        enableBluetoothPendingIntent
    ) { connected, scannedOrConnected, users, enableBluetooth ->
        Device(connected, scannedOrConnected, users, enableBluetooth)
    }

    private data class Device(
        val connected: Boolean,
        val scannedOrConnected: Boolean,
        val users: Map<String, String>?,
        val enableBlueooth: PendingIntent?
    )

    override val events = MutableSharedFlow<Event>()

    override val state = combine(
        tagStates,
        tagConnection,
        options,
        device,
        mapOptions
    ) { tagStates, tagConnection, options, device, mapOptions ->
        val states = when(tagStates) {
            is TagStates.Loaded -> tagStates.states
            is TagStates.Error -> return@combine State.Error(ErrorType.Generic(tagStates.code))
        }
        if(states?.isEmpty() == true) return@combine State.Error(ErrorType.NoTags)
        if(options == null) return@combine State.Error(ErrorType.Generic(104))
        val connection = when(tagConnection) {
            is TagConnectionState.Loaded -> tagConnection
            is TagConnectionState.Loading -> return@combine State.Loading
            is TagConnectionState.Error -> return@combine State.Error(ErrorType.Generic(100))
        }
        val deviceId = connection.deviceId
        val isConnected = device.connected
        val isScannedOrConnected = device.scannedOrConnected
        val users = device.users
        lastSelectedDeviceId.set(deviceId)
        val tagState = states?.get(deviceId)
        val requirements = options.requirements
        val favouriteIds = mapOptions.favouriteIds ?: emptyList()
        val otherTags = states?.filterNot {
            it.key == deviceId
        }?.filter { favouriteIds.contains(it.key) }?.map {
            it.value
        }?.filterIsInstance<TagState.Loaded>() ?: emptyList()
        val knownTagIds = states?.keys?.toList() ?: emptyList()
        val showPicker = states?.filterNot {
            it.key == deviceId
        }?.isNotEmpty() == true
        when {
            requirements.moduleState == null -> {
                State.Error(ErrorType.SmartThingsNotInstalled)
            }
            requirements.moduleState is ModuleState.NotModded -> {
                State.Error(ErrorType.ModuleNotActivated(requirements.moduleState.isUTagBuild))
            }
            requirements.moduleState is ModuleState.Outdated -> {
                State.Error(ErrorType.ModuleOutdated(requirements.moduleState.isUTagBuild))
            }
            requirements.moduleState is ModuleState.Newer -> {
                State.Error(ErrorType.ModuleNewer(requirements.moduleState.isUTagBuild))
            }
            !requirements.hasRequiredPermissions -> State.Error(ErrorType.Permissions)
            tagState is TagState.Loaded -> {
                if(tagState.getLocation() != null) {
                    //Clear PIN error if it's set
                    isPinError = false
                }
                State.Loaded(
                    deviceId,
                    tagState,
                    connection.tagConnection,
                    isConnected,
                    isScannedOrConnected,
                    options.isRefreshingOrSyncing,
                    showPicker,
                    options.insets,
                    mapOptions,
                    options.showAddToHome,
                    otherTags,
                    knownTagIds,
                    users,
                    tagState.requiresAgreement(),
                    options.region,
                    device.enableBlueooth
                )
            }
            tagState is TagState.Error -> State.Error(ErrorType.Generic(tagState.code))
            else -> State.Error(ErrorType.Generic(102))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        scheduleRefreshTimer(REFRESH_TIME)
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
            isResumed.emit(true)
            smartTagRepository.refreshTagStates()
        }
    }

    override fun onPause() {
        cancelRefreshTimer()
        viewModelScope.launch {
            isResumed.emit(false)
        }
    }

    override fun setInsets(insets: Insets) {
        viewModelScope.launch {
            this@TagMapViewModelImpl.insets.emit(insets)
        }
    }

    override fun onRefreshClicked() {
        viewModelScope.launch {
            refresh(notifyIfFailed = true)
        }
    }

    override fun setSearchingMode() {
        viewModelScope.launch {
            val tagConnection = (state.value as? State.Loaded)?.tagConnection ?: return@launch
            if(!tagConnection.setSearchingMode()) {
                events.emit(Event.NetworkError)
            }
        }
    }

    override fun onRingClicked() {
        val device = (state.value as? State.Loaded)?.tagState?.device ?: return
        viewModelScope.launch {
            navigation.navigate(
                TagMapFragmentDirections.actionTagMapFragmentToTagRingDialogFragment(
                    device.deviceId, device.label
                )
            )
        }
    }

    override fun onLocationHistoryClicked() {
        val device = (state.value as? State.Loaded)?.tagState?.device ?: return
        viewModelScope.launch {
            navigation.navigate(
                TagMapFragmentDirections.actionTagMapFragmentToTagLocationHistoryFragment(
                    device.deviceId, device.label, device.isOwner
                )
            )
        }
    }

    override fun onMoreClicked() {
        viewModelScope.launch {
            val deviceInfo = (state.value as? State.Loaded)?.tagState?.device ?: return@launch
            navigation.navigate(
                TagMapFragmentDirections.actionTagMapFragmentToMoreContainerFragment(
                    deviceInfo.deviceId, deviceInfo.label
                )
            )
        }
    }

    override fun showPinEntry() {
        if(pinState != PinState.IDLE) return
        pinState = PinState.SHOWING
        viewModelScope.launch {
            if(encryptionRepository.hasPin()) {
                pinState = PinState.IDLE
                return@launch
            }
            //Device has to have loaded for PIN entry to be shown
            val deviceName = (state.value as? State.Loaded)?.tagState?.device?.label
                ?: return@launch
            navigation.navigate(TagMapFragmentDirections.actionTagMapFragmentToTagPinEntryDialogFragment(
                deviceName, isPinError, false
            ))
        }
    }

    override fun onPinCancelled() {
        viewModelScope.launch {
            smartTagRepository.setPINSuppressed(true)
            pinState = PinState.IDLE
        }
    }

    override fun onPinEntered(pinEntryResult: PinEntryResult.Success) {
        viewModelScope.launch {
            encryptionRepository.setPIN(pinEntryResult.pin, pinEntryResult.save)
            //If PIN is shown again, show the error message
            isPinError = true
            pinState = PinState.WAITING
            smartTagRepository.refreshTagStates()
        }
    }

    override fun onAllowAccessClicked() {
        viewModelScope.launch {
            val deviceId = (state.value as? State.Loaded)?.deviceId ?: return@launch
            val userId = encryptedSettingsRepository.userId.get()
            if(apiRepository.setShareableMembers(deviceId, userId)) {
                smartTagRepository.refreshTagStates()
            }else{
                events.emit(Event.NetworkError)
            }
        }
    }

    override fun onPickerClicked() {
        viewModelScope.launch {
            val state = state.value as? State.Loaded ?: return@launch
            navigation.navigate(
                TagMapFragmentDirections.actionTagMapFragmentToPickerContainerFragment(
                    state.knownTagIds.toTypedArray(), state.tagState.device.deviceId
                )
            )
        }
    }

    override fun onCenterClicked() {
        viewModelScope.launch {
            shouldCenter.emit(true)
        }
    }

    override fun onBluetoothEnable() {
        viewModelScope.launch {
            val pendingIntent = (state.value as? State.Loaded)?.enableBluetoothPendingIntent
            if(pendingIntent != null) {
                navigation.navigate(pendingIntent)
            }else{
                navigation.navigate(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            }
        }
    }

    override fun setSelectedDeviceId(deviceId: String) {
        viewModelScope.launch {
            this@TagMapViewModelImpl.deviceId.emit(deviceId)
            shouldCenter.emit(true)
        }
    }

    override fun onMapMoved() {
        viewModelScope.launch {
            shouldCenter.emit(false)
        }
    }

    private fun scheduleRefreshTimer(refreshTime: Long) {
        cancelRefreshTimer()
        refreshJob = viewModelScope.launch {
            delay(refreshTime)
            smartTagRepository.refreshTagStates()
            scheduleRefreshTimer(refreshTime)
        }
    }

    private fun cancelRefreshTimer() {
        refreshJob?.cancel()
    }

    private suspend fun refresh(notifyIfFailed: Boolean) = refreshLock.withLock {
        val state = state.value as? State.Loaded ?: return@withLock
        val deviceId = state.deviceId
        val deviceLabel = state.tagState.device.label
        val tagConnection = state.tagConnection
        //Ignore if already refreshing
        if(isRefreshing.value) return
        isRefreshing.emit(true)
        val isConnected = tagConnection.getIsScannedOrConnected()
        val result = if (isConnected.first()) {
            tagConnection.syncLocation(true)
        } else {
            //Start a scan to see if we can connect now
            uTagServiceRepository.runWithService {
                it.startTagScanNow(REFRESH_SCAN_TIME)
            }
            //Wait for a connection with the timeout
            val connectResult = withTimeoutOrNull(REFRESH_SCAN_TIME) {
                first(
                    viewModelScope,
                    isConnected.filter { it },
                    tagConnectResult.filter { it.first == deviceId }
                ).let {
                    when(it) {
                        is FirstResult.One -> ConnectResult.SUCCESS
                        is FirstResult.Two -> ConnectResult.FAILED_HANDLED_ELSEWHERE
                    }
                }
            } ?: ConnectResult.FAILED
            //If we're now connected, start a sync. If not, reject
            when (connectResult) {
                ConnectResult.SUCCESS -> {
                    tagConnection.syncLocation(true)
                }
                ConnectResult.FAILED -> {
                    SyncResult.FAILED_TO_CONNECT
                }
                ConnectResult.FAILED_HANDLED_ELSEWHERE -> {
                    SyncResult.FAILED_ALREADY_SYNCING
                }
            }
        }
        when (result) {
            SyncResult.SUCCESS -> {
                smartTagRepository.refreshTagStates()
                historyWidgetRepository.updateWidgets()
            }
            SyncResult.FAILED_TO_CONNECT, SyncResult.FAILED_DISCONNECTED -> {
                if(notifyIfFailed) {
                    events.emit(Event.FailedToRefresh(deviceLabel))
                }
                //Run a scan to see if we can get reconnected to the tag
                uTagServiceRepository.runWithService {
                    //0 = default timeout will be used
                    it.startTagScanNow(0)
                }
            }
            SyncResult.FAILED_TO_SEND -> {
                events.emit(Event.NetworkError)
            }
            SyncResult.FAILED_TO_GET_LOCATION -> {
                events.emit(Event.LocationError)
            }
            SyncResult.FAILED_ALREADY_SYNCING, SyncResult.FAILED_OTHER, SyncResult.FAILED_AUTO_SYNC_NOT_REQUIRED -> {
                //Auto sync is happening, does not need to happen or a race condition, do nothing
            }
        }
        isRefreshing.emit(false)
    }

    private fun setupConnectResult() = viewModelScope.launch {
        tagConnectResult.collect {
            events.emit(Event.FailedToConnect(it.first, it.second, it.third))
        }
    }

    init {
        setupConnectResult()
    }

    override fun onCleared() {
        super.onCleared()
        smartTagRepository.destroyTagConnections()
    }

    enum class PinState {
        IDLE, SHOWING, WAITING
    }

    private enum class ConnectResult {
        SUCCESS, FAILED, FAILED_HANDLED_ELSEWHERE
    }

    sealed class TagConnectionState {
        data object Loading: TagConnectionState()
        data class Loaded(
            val deviceId: String,
            val tagConnection: RemoteTagConnection
        ): TagConnectionState()
        data object Error: TagConnectionState()
    }

    data class Options(
        val insets: Insets?,
        val requirements: Requirements,
        val isRefreshingOrSyncing: Boolean,
        val showAddToHome: Boolean,
        val region: String
    ) {
        data class Requirements(
            val moduleState: ModuleState?,
            val hasRequiredPermissions: Boolean
        )
    }

    private sealed class TagStates {
        data class Loaded(val states: Map<String, TagState>?): TagStates()
        data class Error(val code: Int): TagStates()
    }

}