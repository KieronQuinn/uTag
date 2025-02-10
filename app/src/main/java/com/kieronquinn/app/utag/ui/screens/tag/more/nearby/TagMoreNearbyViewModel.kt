package com.kieronquinn.app.utag.ui.screens.tag.more.nearby

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection
import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection.RingResult
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.model.VolumeLevel
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.Units
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.UTagServiceRepository
import com.kieronquinn.app.utag.repositories.UwbRepository
import com.kieronquinn.app.utag.repositories.UwbRepository.UwbEvent
import com.kieronquinn.app.utag.repositories.UwbRepository.UwbState
import com.kieronquinn.app.utag.service.IUTagService
import com.kieronquinn.app.utag.service.callback.ITagStateCallback
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel.State.RSSI
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel.State.RSSI.Distance
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel.State.RSSI.MoveAroundText
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel.State.UWB
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel.State.UWB.Direction
import com.kieronquinn.app.utag.utils.extensions.BluetoothState
import com.kieronquinn.app.utag.utils.extensions.bluetoothEnabledAsFlow
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.getInclination
import com.kieronquinn.app.utag.utils.extensions.getUWBSettingsIntent
import com.kieronquinn.app.utag.utils.extensions.onConnectResult
import com.kieronquinn.app.utag.xposed.extensions.hasPermission
import com.samsung.android.oneconnect.base.device.tag.TagConnectionState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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
import kotlin.Float
import java.lang.Float as JavaFloat

abstract class TagMoreNearbyViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onResume()
    abstract fun close()
    abstract fun onRingClicked()
    abstract fun onRingVolumeUpClicked()
    abstract fun onRingVolumeDownClicked()
    abstract fun onEnableUWBClicked()
    abstract fun onEnableBluetoothClicked()
    abstract fun onIgnoreUWBClicked()
    abstract fun onUWBPermissionDenied()

    sealed class State {
        data object Loading: State()
        data class EnableBluetooth(val intent: PendingIntent?): State()
        data class Searching(val remainingSeconds: Long, val progress: Float): State()
        data class RSSI(
            val distance: Distance,
            val moveAroundText: MoveAroundText,
            val ringState: RingState = RingState.NotRinging
        ): State() {
            enum class Distance(@StringRes val label: Int, val progress: Float) {
                FAR(R.string.nearby_its_far_away, 0.333f),
                NOT_FAR(R.string.nearby_its_not_far_from_here, 0.666f),
                NEAR(R.string.nearby_its_near_you, 1f)
            }
            enum class MoveAroundText(@StringRes val label: Int) {
                MOVE_AROUND(R.string.nearby_move_around),
                GETTING_WEAKER(R.string.nearby_signal_weaker),
                GETTING_STRONGER(R.string.nearby_signal_stronger)
            }
        }
        data class UWB(
            val isCloseBy: Boolean,
            val isVeryCloseBy: Boolean,
            val isCorrected: Boolean,
            val distance: Double,
            val direction: Direction?,
            val azimuth: Float?,
            val ringState: RingState = RingState.NotRinging,
            val requiresInclination: Boolean = false,
            val units: Units = Units.SYSTEM
        ): State() {
            enum class Direction(@StringRes val label: Int) {
                UP(R.string.nearby_look_up),
                DOWN(R.string.nearby_look_down),
                LEFT(R.string.nearby_turn_left),
                RIGHT(R.string.nearby_turn_right),
                FORWARD(R.string.nearby_go_forward),
                BACKWARD(R.string.nearby_its_behind_you),
                UNSURE(R.string.nearby_move_around)
            }
        }
        data object UWBDisabled: State()
        data class UWBPermissionRequired(val permission: String): State()
        data class Timeout(val wasConnected: Boolean): State()
    }

    sealed class RingState {
        data object NotRinging: RingState()
        data object Sending: RingState()
        data class Ringing(val volume: VolumeLevel, val sending: Boolean = false): RingState()
    }

    sealed class Event {
        data object RingError : Event()
        data class FailedToConnect(
            val deviceId: String,
            val deviceLabel: String,
            val reason: TagConnectionState
        ): Event()
    }

}

class TagMoreNearbyViewModelImpl(
    private val uwbRepository: UwbRepository,
    private val navigation: TagMoreNavigation,
    context: Context,
    smartTagRepository: SmartTagRepository,
    smartThingsRepository: SmartThingsRepository,
    serviceRepository: UTagServiceRepository,
    settingsRepository: SettingsRepository,
    deviceId: String,
    uwbAvailable: Boolean
): TagMoreNearbyViewModel() {

    companion object {
        private const val REFRESH_RATE_RSSI = 200L
        private const val TIMEOUT_SECONDS = 120L
        private const val UWB_MIN_DISTANCE_FOR_UP_DOWN = 1 //1 metre
        private const val UWB_MIN_DISTANCE_FOR_ANIM = 0.25 //0.25 metres
    }

    private val tagConnection = serviceRepository.service.flatMapLatest {
        it?.isConnected(deviceId) ?: flowOf(false)
    }.mapLatest { connected ->
        if(connected) {
            wasConnected = true
            smartTagRepository.getTagConnection(deviceId)
        } else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val ringStopEvent = tagConnection.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(null)
        it.onRingStop()
    }.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val refreshBus = MutableStateFlow(System.currentTimeMillis())
    private val suppressUwb = MutableStateFlow(false)
    private val hasShownUwbPrompt = MutableStateFlow(false)
    private val shouldConnectUwbIfPossible = MutableStateFlow(false)
    private val ringState = MutableStateFlow<RingState>(RingState.NotRinging)
    private val ringLock = Mutex()
    private var wasConnected = false
    private val bluetoothEnabled = context.bluetoothEnabledAsFlow(viewModelScope)

    private val allowLongDistanceUwb = settingsRepository.allowLongDistanceUwb.asFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val uwbPermissionRequired = refreshBus.mapLatest {
        val permission = uwbRepository.permission ?: return@mapLatest null
        permission.takeUnless { context.hasPermission(permission) }
    }

    override val events = MutableSharedFlow<Event>()

    private val uwbState = combine(
        settingsRepository.useUwb.asFlow(),
        refreshBus
    ) { useUwb, _ ->
        if(uwbAvailable && useUwb) {
            uwbRepository.getUwbState()
        }else{
            UwbState.UNAVAILABLE
        }
    }

    private val bluetoothIntent = flow {
        emit(smartThingsRepository.getEnableBluetoothIntent())
    }

    private val connectUwbIfPossible = shouldConnectUwbIfPossible
        .debounce(1000L) //Don't spam connect/disconnect
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val tagConnectResult = serviceRepository.service.flatMapLatest {
        it?.onConnectResult()?.filter { result -> result.second.isError() }?.mapNotNull { result ->
            val label = smartTagRepository.getKnownTagNames().first()[result.first.hashCode()]
                ?: return@mapNotNull null
            Triple(result.first, label, result.second)
        } ?: emptyFlow()
    }.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val uwbRangingState = combine(
        uwbState,
        connectUwbIfPossible,
        tagConnection,
        hasShownUwbPrompt,
        uwbPermissionRequired
    ) { state, shouldConnect, connection, shownPrompt, permission ->
        when {
            state == UwbState.UNAVAILABLE -> {
                flowOf(UWBEventWrapper.UWBUnavailable)
            }
            permission != null -> {
                flowOf(UWBEventWrapper.UWBPermissionRequired(permission))
            }
            state == UwbState.DISABLED -> {
                if(shownPrompt) {
                    flowOf(UWBEventWrapper.UWBUnavailable)
                }else{
                    flowOf(UWBEventWrapper.UWBDisabled)
                }
            }
            //These are only checked after we check if we need to prompt the user at all
            !shouldConnect || connection == null -> {
                flowOf(UWBEventWrapper.UWBUnavailable)
            }
            else -> {
                uwbRepository.startRanging(viewModelScope, connection) {
                    onRangingEnded()
                }.map { event ->
                    if(event != null) {
                        UWBEventWrapper.Event(event)
                    }else UWBEventWrapper.UWBUnavailable
                }
            }
        }
    }.flattenConcat().map { event ->
        event.toState()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val rssiState = tagConnection.flatMapLatest { connection ->
        connection?.readRssi(REFRESH_RATE_RSSI) ?: flowOf(null)
    }.map { rssi ->
        rssi.toRssiState()
    }.onEach {
        //Start UWB ranging if the Tag is nearby
        val shouldConnectToUwb = it != null && it.distance >= Distance.NOT_FAR
        if(shouldConnectToUwb && !shouldConnectUwbIfPossible.value) {
            shouldConnectUwbIfPossible.emit(true)
        }
    }

    private val countdownOrTimeout = tagConnection.flatMapLatest { connection ->
        if(connection != null) return@flatMapLatest flowOf(null)
        shouldConnectUwbIfPossible.emit(false)
        serviceRepository.runWithService {
            it.startTagScanNow(TIMEOUT_SECONDS * 1000L)
        }
        flow<State> {
            for(i in TIMEOUT_SECONDS downTo 0) {
                val progress = i.toFloat() / TIMEOUT_SECONDS
                emit(State.Searching(i, progress))
                delay(1000L)
            }
            emit(State.Timeout(wasConnected))
        }
    }

    private val requiresInclination = context.getInclination().map { inclination ->
        inclination != null && (inclination < 50 || inclination > 120)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val options = combine(
        suppressUwb,
        requiresInclination,
        settingsRepository.units.asFlow(),
        bluetoothEnabled,
        bluetoothIntent
    ) { uwb, inclination, units, bluetooth, bluetoothIntent ->
        Options(uwb, inclination, units, bluetooth, bluetoothIntent)
    }

    override val state = combine(
        uwbRangingState,
        rssiState,
        countdownOrTimeout,
        options,
        ringState
    ) { uwb, rssi, countdown, options, ring ->
        val suppressUwb = options.suppressUwb
        val inclination = options.requiresInclination
        val units = options.units
        val bluetooth = options.bluetoothEnabled
        when {
            !bluetooth.enabled -> State.EnableBluetooth(options.bluetoothIntent)
            !suppressUwb && uwb != null -> uwb.copyWithOptions(inclination, ring, rssi, units)
            rssi != null -> rssi.copy(ringState = ring)
            countdown != null -> countdown
            else -> State.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private fun UWBEventWrapper.toState(): State? {
        return when(this) {
            is UWBEventWrapper.UWBUnavailable -> null
            is UWBEventWrapper.UWBDisabled -> State.UWBDisabled
            is UWBEventWrapper.UWBPermissionRequired -> State.UWBPermissionRequired(permission)
            is UWBEventWrapper.Event -> {
                when (event) {
                    is UwbEvent.Report -> {
                        val distance = event.distance.value.toDouble()
                        val azimuth = event.azimuth?.value?.toDouble()
                        val altitude = event.altitude?.value?.toDouble()
                        val isCorrected = event.corrected
                        val isCloseBy = distance <= UWB_MIN_DISTANCE_FOR_UP_DOWN
                        val isVeryCloseBy = distance <= UWB_MIN_DISTANCE_FOR_ANIM
                        val direction = if(azimuth != null && (altitude != null || isCorrected)) {
                            if(isCloseBy && altitude != null) {
                                calculateCloseDirection(distance, azimuth, altitude)
                            } else {
                                calculateFarDirection(azimuth)
                            }
                        }else Direction.UNSURE
                        val adjustedAzimuth = when {
                            event.corrected -> azimuth?.let { azimuthRadians ->
                                Math.toDegrees(azimuthRadians).toFloat()
                            }
                            azimuth != null -> {
                                calculateAzimuth(azimuth)
                            }
                            else -> null
                        }
                        UWB(
                            isCloseBy,
                            isVeryCloseBy,
                            isCorrected,
                            distance,
                            direction,
                            adjustedAzimuth.takeUnless { isCloseBy }
                        )
                    }
                    else -> null
                }
            }
        }
    }

    private fun onRangingEnded() = viewModelScope.launch {
        //Reset the UWB flag to allow connection to start again if needed
        shouldConnectUwbIfPossible.emit(false)
    }

    private suspend fun State.copyWithOptions(
        requiresInclination: Boolean,
        ringState: RingState,
        rssi: RSSI?,
        units: Units
    ): State {
        return when {
            this is UWB -> {
                val inclination = requiresInclination && !isCloseBy && !isCorrected
                //Remove direction if RSSI-based distance is not near and user hasn't opted out
                val stripDirection = (rssi != null && rssi.distance < Distance.NEAR)
                        && !allowLongDistanceUwb.firstNotNull()
                copy(
                    requiresInclination = inclination,
                    ringState = ringState,
                    //Remove the azimuth & direction if the user needs to reorient & are not close
                    azimuth = when {
                        inclination || stripDirection -> null
                        else -> azimuth
                    },
                    direction = when {
                        inclination -> null
                        stripDirection -> Direction.UNSURE
                        else -> direction
                    },
                    units = units
                )
            }
            else -> this
        }
    }

    private var previousAzimuth = Float.NaN

    private fun calculateAzimuth(azimuth: Double): Float {
        var azimuthDegrees = Math.toDegrees(azimuth).toFloat()
        if(!JavaFloat.isNaN(previousAzimuth)) {
            val previous: Float = previousAzimuth
            val diff: Float = azimuthDegrees - previous
            if (diff.compareTo(180f) > 0) {
                azimuthDegrees = -1f * (360f - azimuthDegrees)
            } else if ((diff < -180f)) {
                azimuthDegrees = 360f + diff + previous
            }
        }
        previousAzimuth = azimuthDegrees
        return azimuthDegrees
    }

    private fun calculateCloseDirection(
        distance: Double,
        azimuth: Double,
        altitude: Double
    ): Direction {
        val port = azimuth * 180 / Math.PI
        val land = altitude * 180 / Math.PI
        return when {
            port.compareTo(-30) < 0 -> Direction.LEFT
            port > 30 -> Direction.RIGHT
            distance * 100 < 350 -> {
                when {
                    land > 30 -> Direction.UP
                    land < 30 -> Direction.DOWN
                    else -> null
                }
            }
            else -> null
        } ?: Direction.FORWARD
    }

    private fun calculateFarDirection(azimuthRadians: Double): Direction {
        val azimuth = Math.toDegrees(azimuthRadians).toFloat().let {
            if(it < 0) 360 + it else it
        }
        return when {
            azimuth >= 0 && azimuth < 46 -> Direction.FORWARD
            azimuth >= 46 && azimuth < 136 -> Direction.RIGHT
            azimuth >= 136 && azimuth < 216 -> Direction.BACKWARD
            azimuth >= 216 && azimuth < 316 -> Direction.LEFT
            azimuth >= 316 && azimuth < 360 -> Direction.FORWARD
            else -> throw RuntimeException("Invalid azimuth: $azimuth")
        }
    }

    private var lastPingRssi = 0
    private var rssiPingCount = 0
    private var lastMoveAroundText = MoveAroundText.MOVE_AROUND

    private fun Int?.toRssiState(): RSSI? {
        if(this == null) {
            //Clear cached data for new scan
            lastPingRssi = 0
            rssiPingCount = 0
            lastMoveAroundText = MoveAroundText.MOVE_AROUND
            return null
        }
        //Initialise the last RSSI if it's not already been set
        if(lastPingRssi == 0) {
            lastPingRssi = this
        }
        val distance = when {
            this > -60 -> Distance.NEAR
            this > -80 -> Distance.NOT_FAR
            else -> Distance.FAR
        }
        val moveAroundText = if(rssiPingCount > 10) {
            if(lastPingRssi > -50 && this > -50) {
                MoveAroundText.MOVE_AROUND
            }else{
                val diff = lastPingRssi - this
                when {
                    diff > 0 -> MoveAroundText.GETTING_WEAKER
                    diff == 0 -> MoveAroundText.MOVE_AROUND
                    else -> MoveAroundText.GETTING_STRONGER
                }
            }.also {
                rssiPingCount = 0
                lastPingRssi = this
            }
        }else lastMoveAroundText
        rssiPingCount++
        lastMoveAroundText = moveAroundText
        return RSSI(distance, moveAroundText)
    }

    override fun onResume() {
        viewModelScope.launch {
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    override fun close() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onRingClicked() {
        viewModelScope.launch {
            val isRinging = when(val current = state.value) {
                is UWB -> current.ringState is RingState.Ringing
                is RSSI -> current.ringState is RingState.Ringing
                else -> return@launch
            }
            if(!isRinging) {
                tagConnection.value?.startRingingNow()
            }else{
                tagConnection.value?.stopRingingNow()
            }
        }
    }

    private suspend fun RemoteTagConnection.startRingingNow() = ringLock.withLock {
        ringState.emit(RingState.Sending)
        val result = startRinging(true)
        if(result is RingResult.SuccessBluetooth) {
            ringState.emit(RingState.Ringing(result.volume))
        }else{
            ringState.emit(RingState.NotRinging)
            events.emit(Event.RingError)
        }
    }

    private suspend fun RemoteTagConnection.stopRingingNow() = ringLock.withLock {
        val before = ringState.value
        ringState.emit(RingState.Sending)
        val result = stopRingingBluetooth()
        if(result) {
            ringState.emit(RingState.NotRinging)
        }else{
            ringState.emit(before)
            events.emit(Event.RingError)
        }
    }

    override fun onRingVolumeUpClicked() {
        viewModelScope.launch {
            ringLock.withLock {
                val before = ringState.value
                ringState.emit(RingState.Ringing(VolumeLevel.LOW, true))
                if(tagConnection.value?.setRingVolume(VolumeLevel.HIGH) == true) {
                    ringState.emit(RingState.Ringing(VolumeLevel.HIGH, false))
                }else{
                    ringState.emit(before)
                    events.emit(Event.RingError)
                }
            }
        }
    }

    override fun onRingVolumeDownClicked() {
        viewModelScope.launch {
            ringLock.withLock {
                val before = ringState.value
                ringState.emit(RingState.Ringing(VolumeLevel.HIGH, true))
                if(tagConnection.value?.setRingVolume(VolumeLevel.LOW) == true) {
                    ringState.emit(RingState.Ringing(VolumeLevel.LOW, false))
                }else{
                    ringState.emit(before)
                    events.emit(Event.RingError)
                }
            }
        }
    }

    override fun onEnableUWBClicked() {
        viewModelScope.launch {
            hasShownUwbPrompt.emit(true)
            val uwbIntent = getUWBSettingsIntent()
            navigation.navigate(uwbIntent) {
                //Bluetooth settings actually goes to connected devices
                val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                navigation.navigate(settingsIntent)
            }
        }
    }

    override fun onEnableBluetoothClicked() {
        viewModelScope.launch {
            val pendingIntent = (state.value as? State.EnableBluetooth)?.intent
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

    override fun onIgnoreUWBClicked() {
        viewModelScope.launch {
            hasShownUwbPrompt.emit(true)
        }
    }

    override fun onUWBPermissionDenied() {
        viewModelScope.launch {
            suppressUwb.emit(true)
        }
    }

    private fun setupRingStop() = viewModelScope.launch {
        ringStopEvent.collect {
            ringState.emit(RingState.NotRinging)
        }
    }

    private fun setupConnectResult() = viewModelScope.launch {
        tagConnectResult.collect {
            events.emit(Event.FailedToConnect(it.first, it.second, it.third))
        }
    }

    init {
        setupRingStop()
        setupConnectResult()
    }

    sealed class UWBEventWrapper {
        data object UWBUnavailable: UWBEventWrapper()
        data class UWBPermissionRequired(val permission: String): UWBEventWrapper()
        data object UWBDisabled: UWBEventWrapper()
        data class Event(val event: UwbEvent?): UWBEventWrapper()
    }

    private data class Options(
        val suppressUwb: Boolean,
        val requiresInclination: Boolean,
        val units: Units,
        val bluetoothEnabled: BluetoothState,
        val bluetoothIntent: PendingIntent?
    )

    private fun IUTagService.isConnected(deviceId: String) = callbackFlow {
        val callback = object: ITagStateCallback.Stub() {
            override fun onConnectedTagsChanged(
                connectedDeviceIds: Array<out String>,
                scannedDeviceIds: Array<out String>
            ) {
                trySend(connectedDeviceIds.contains(deviceId))
            }
        }
        val id = addTagStateCallback(callback)
        awaitClose {
            try {
                removeTagStateCallback(id ?: return@awaitClose)
            }catch (e: Exception) {
                //Ignore, service will clean this up
            }
        }
    }

}