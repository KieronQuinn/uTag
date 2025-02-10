package com.kieronquinn.app.utag.ui.screens.tag.locationhistory

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.core.graphics.Insets
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagContainerNavigation
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.ContentCreatorRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptionRepository
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.ExportLocation
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.HistoryState
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.LocationHistoryPoint
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.PinEntryResult
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

abstract class TagLocationHistoryViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val event: Flow<Event>

    abstract fun onBackPressed()
    abstract fun onResume()
    abstract fun onPause()

    abstract fun onInsetsChanged(insets: Insets)
    abstract fun onPreviousClicked()
    abstract fun onNextClicked()
    abstract fun onDateClicked()
    abstract fun onDateSelected(date: LocalDateTime)
    abstract fun onPointClicked(point: LocationHistoryPoint)
    abstract fun onDeleteClicked()
    abstract fun onIntroGotItClicked()
    abstract fun showPinEntry()
    abstract fun onPinCancelled()
    abstract fun onPinEntered(pinEntryResult: PinEntryResult.Success)
    abstract fun onExportUriResult(uri: Uri)

    sealed class State(
        open val selectedDate: LocalDateTime,
        open val previousEnabled: Boolean,
        open val nextEnabled: Boolean,
        open val debugEnabled: Boolean,
        open val insets: Insets?
    ) {
        data class Loading(
            override val selectedDate: LocalDateTime,
            override val previousEnabled: Boolean,
            override val nextEnabled: Boolean,
            override val debugEnabled: Boolean,
            override val insets: Insets?,
            val progress: Int?
        ): State(selectedDate, previousEnabled, nextEnabled, debugEnabled, insets)
        data class Loaded(
            val points: List<LocationHistoryPoint>,
            val selectedPoint: LocationHistoryPoint,
            val decryptFailed: Boolean,
            val mapOptions: MapOptions,
            val exportLocations: List<ExportLocation>,
            override val selectedDate: LocalDateTime,
            override val previousEnabled: Boolean,
            override val nextEnabled: Boolean,
            override val debugEnabled: Boolean,
            override val insets: Insets?
        ): State(selectedDate, previousEnabled, nextEnabled, debugEnabled, insets) {
            override fun showMenu() = true
        }
        data class Empty(
            override val selectedDate: LocalDateTime,
            override val previousEnabled: Boolean,
            override val nextEnabled: Boolean,
            override val debugEnabled: Boolean,
            override val insets: Insets?,
            private val hasOtherDays: Boolean
        ): State(selectedDate, previousEnabled, nextEnabled, debugEnabled, insets) {
            override fun showMenu() = hasOtherDays
        }
        data class Error(
            override val selectedDate: LocalDateTime,
            override val previousEnabled: Boolean,
            override val nextEnabled: Boolean,
            override val debugEnabled: Boolean,
            override val insets: Insets?
        ): State(selectedDate, previousEnabled, nextEnabled, debugEnabled, insets)
        data class PINRequired(
            override val selectedDate: LocalDateTime,
            override val previousEnabled: Boolean,
            override val nextEnabled: Boolean,
            override val debugEnabled: Boolean,
            override val insets: Insets?
        ): State(selectedDate, previousEnabled, nextEnabled, debugEnabled, insets)
        data class Intro(
            override val selectedDate: LocalDateTime,
            override val previousEnabled: Boolean,
            override val nextEnabled: Boolean,
            override val debugEnabled: Boolean,
            override val insets: Insets?
        ): State(selectedDate, previousEnabled, nextEnabled, debugEnabled, insets)

        open fun showMenu() = false
    }

    enum class Event {
        DELETE_FAILED,
        DELETE_DISABLED,
        EXPORT_DISABLED
    }

    data class MapOptions(
        val location: Location?,
        val style: MapStyle,
        val theme: MapTheme,
        val showBuildings: Boolean
    )

}

class TagLocationHistoryViewModelImpl(
    private val navigation: TagContainerNavigation,
    private val deviceId: String,
    private val deviceName: String,
    private val apiRepository: ApiRepository,
    private val locationHistoryRepository: LocationHistoryRepository,
    private val encryptionRepository: EncryptionRepository,
    private val smartTagRepository: SmartTagRepository,
    private val contentCreatorRepository: ContentCreatorRepository,
    settingsRepository: SettingsRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    context: Context
): TagLocationHistoryViewModel() {

    //We only show 7 days but request 8 to show a start time for day 1's first location
    private val firstDayToShow = LocalDateTime.now().minusDays(7).toLocalDate().atStartOfDay()
    private val lastDayToShow = LocalDateTime.now().toLocalDate().atStartOfDay()

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val isResumed = MutableStateFlow(false)
    private val selectedDay = MutableStateFlow(lastDayToShow)
    private val selectedPoint = MutableStateFlow<LocationHistoryPoint?>(null)
    private val hasSeenIntro = settingsRepository.hasSeenLocationHistoryIntro
    private val insets = MutableStateFlow<Insets?>(null)

    private val hasSuppressedPin = MutableStateFlow(false)
    private var pinState = PinState.IDLE
    private var isPinError = false

    private val refreshBus = MutableStateFlow(System.currentTimeMillis())

    private val hasLocationPermissions = resumeBus.mapLatest {
        context.hasLocationPermissions()
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

    private val mapOptions = combine(
        myLocation,
        settingsRepository.mapStyle.asFlow(),
        settingsRepository.mapTheme.asFlow(),
        settingsRepository.mapShowBuildings.asFlow()
    ) { location, style, theme, buildings ->
        MapOptions(
            location,
            style,
            theme,
            buildings
        )
    }

    private val fullHistory = refreshBus.flatMapLatest {
        flow {
            emit(HistoryState.Loading(deviceId, progress = 0))
            val result = locationHistoryRepository.getLocationHistory(deviceId) {
                emit(HistoryState.Loading(deviceId, progress = it))
            }
            emit(result)
        }
    }.flowOn(Dispatchers.IO).onEach {
        if(pinState == PinState.WAITING) {
            pinState = PinState.IDLE
        }
    }

    private val selected = combine(
        selectedDay,
        selectedPoint
    ) { day, point ->
        Pair(day, point)
    }

    private val debugMode = encryptedSettingsRepository
        .isDebugModeEnabled(viewModelScope)

    private val options = combine(
        hasSeenIntro.asFlow(),
        hasSuppressedPin,
        debugMode.filterNotNull(),
    ) { hasSeenIntro, hasSuppressedPin, debugMode ->
        Triple(hasSeenIntro, hasSuppressedPin, debugMode)
    }

    override val event = MutableSharedFlow<Event>()

    override val state = combine(
        fullHistory,
        selected,
        insets,
        options,
        mapOptions
    ) { history, selected, insets, options, mapOptions ->
        val hasSeenIntro = options.first
        val hasSuppressedPin = options.second
        val debugMode = options.third
        val date = selected.first
        val point = selected.second
        val nextEnabled = date < lastDayToShow
        val previousEnabled = date > firstDayToShow.plusDays(1)
        when {
            !hasSeenIntro -> {
                return@combine State.Intro(date, previousEnabled, nextEnabled, debugMode, insets)
            }
            history is HistoryState.Loading -> {
                return@combine State.Loading(
                    date,
                    previousEnabled,
                    nextEnabled,
                    debugMode,
                    insets,
                    history.progress
                )
            }
            history is HistoryState.Error -> {
                return@combine State.Error(date, previousEnabled, nextEnabled, debugMode, insets)
            }
            history is HistoryState.Loaded && history.decryptFailed && !hasSuppressedPin -> {
                return@combine State.PINRequired(date, previousEnabled, nextEnabled, debugMode, insets)
            }
        }
        history as HistoryState.Loaded
        //Reset PIN error if needed
        isPinError = false
        val day = date.toLocalDate()
        val points = history.items.filter {
            it.isOnDay(day)
        }
        if(points.isEmpty()) {
            val hasOtherDays = history.items.isNotEmpty()
            return@combine State.Empty(
                date, previousEnabled, nextEnabled, debugMode, insets, hasOtherDays
            )
        }
        State.Loaded(
            points = points,
            selectedPoint = point ?: points.last(),
            selectedDate = date,
            previousEnabled = previousEnabled,
            nextEnabled = nextEnabled,
            debugEnabled = debugMode,
            insets = insets,
            decryptFailed = history.decryptFailed,
            mapOptions = mapOptions,
            exportLocations = history.exportLocations
        )
    }.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        //Initial state has buttons disabled to prevent interactions
        State.Loading(
            selectedDay.value,
            previousEnabled = false,
            nextEnabled = false,
            debugEnabled = false,
            insets = null,
            progress = 0
        )
    )

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateUpTo(R.id.tagMapFragment)
        }
    }

    override fun onResume() {
        viewModelScope.launch {
            isResumed.emit(true)
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onPause() {
        viewModelScope.launch {
            isResumed.emit(false)
        }
    }

    override fun onInsetsChanged(insets: Insets) {
        viewModelScope.launch {
            this@TagLocationHistoryViewModelImpl.insets.emit(insets)
        }
    }

    override fun onPreviousClicked() {
        viewModelScope.launch {
            val previous = selectedDay.value.minusDays(1)
                .takeUnless { it < firstDayToShow } ?: return@launch
            selectedPoint.emit(null)
            selectedDay.emit(previous)
        }
    }

    override fun onNextClicked() {
        viewModelScope.launch {
            val next = selectedDay.value.plusDays(1)
                .takeUnless { it > lastDayToShow } ?: return@launch
            selectedPoint.emit(null)
            selectedDay.emit(next)
        }
    }

    override fun onDateClicked() {
        viewModelScope.launch {
            val selected = selectedDay.value
            navigation.navigate(
                TagLocationHistoryFragmentDirections
                    .actionTagLocationHistoryFragmentToTagLocationHistoryDatePickerDialogFragment(selected)
            )
        }
    }

    override fun onDateSelected(date: LocalDateTime) {
        viewModelScope.launch {
            selectedPoint.emit(null)
            selectedDay.emit(date)
        }
    }

    override fun onPointClicked(point: LocationHistoryPoint) {
        viewModelScope.launch {
            selectedPoint.emit(point)
        }
    }

    override fun onDeleteClicked() {
        viewModelScope.launch {
            //Don't delete history in content creator mode
            if(contentCreatorRepository.isEnabled()) {
                event.emit(Event.DELETE_DISABLED)
                return@launch
            }
            if(apiRepository.deleteLocationHistory(deviceId)) {
                refreshBus.emit(System.currentTimeMillis())
            }else{
                event.emit(Event.DELETE_FAILED)
            }
        }
    }

    override fun onIntroGotItClicked() {
        viewModelScope.launch {
            hasSeenIntro.set(true)
        }
    }

    override fun showPinEntry() {
        if(pinState != PinState.IDLE) return
        pinState = PinState.SHOWING
        viewModelScope.launch {
            navigation.navigate(TagLocationHistoryFragmentDirections
                .actionTagLocationHistoryFragmentToTagPinEntryDialogFragment(
                    deviceName, isPinError, true
                ))
        }
    }

    override fun onPinCancelled() {
        viewModelScope.launch {
            hasSuppressedPin.emit(true)
            //Even though we don't use TagState here, suppress PIN elsewhere in the app too
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

    override fun onExportUriResult(uri: Uri) {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            //Don't export in content creator mode
            if(contentCreatorRepository.isEnabled()) {
                //Allow time for the activity to resume
                delay(500L)
                event.emit(Event.EXPORT_DISABLED)
                return@launch
            }
            navigation.navigate(TagLocationHistoryFragmentDirections
                .actionTagLocationHistoryFragmentToTagLocationExportDialogFragment(
                    uri, state.exportLocations.toTypedArray()
                ))
        }
    }

    enum class PinState {
        IDLE, SHOWING, WAITING
    }

}