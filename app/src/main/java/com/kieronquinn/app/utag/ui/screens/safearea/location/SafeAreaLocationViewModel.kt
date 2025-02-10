package com.kieronquinn.app.utag.ui.screens.safearea.location

import android.content.Context
import androidx.core.graphics.Insets
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.model.ExitBuffer
import com.kieronquinn.app.utag.repositories.ContentCreatorRepository
import com.kieronquinn.app.utag.repositories.GeocoderRepository
import com.kieronquinn.app.utag.repositories.SafeAreaRepository
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea.Location
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import android.location.Location as AndroidLocation

abstract class SafeAreaLocationViewModel: ViewModel() {

    abstract var hasShownToast: Boolean
    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onResume()
    abstract fun onInsetsChanged(insets: Insets)
    abstract fun hasChanges(): Boolean
    abstract fun onMapMoved()
    abstract fun onCenterChanged(center: LatLng, shouldAnimate: Boolean)
    abstract fun onNameChanged(name: String)
    abstract fun onRadiusChanged(radius: Float)
    abstract fun onExitBufferChanged(exitBuffer: ExitBuffer)
    abstract fun onSaveClicked()
    abstract fun onCloseClicked()
    abstract fun onDeleteClicked()
    abstract fun onCenterClicked()

    sealed class State(open val insets: Insets?) {
        data class Loading(override val insets: Insets?): State(insets)
        data class Loaded(
            override val insets: Insets? = null,
            val id: String,
            val name: String? = null,
            val center: LatLng,
            val radius: Float,
            val exitBuffer: ExitBuffer = ExitBuffer.NONE,
            val lastExitTimestamp: Long = 0,
            val activeDeviceIds: Set<String> = emptySet(),
            val mapOptions: MapOptions? = null,
        ): State(insets)
        data class NoLocation(override val insets: Insets?): State(insets)
    }

    enum class Event {
        NAME_GENERATION_FAILED
    }

    data class MapOptions(
        val location: AndroidLocation?,
        val style: MapStyle,
        val theme: MapTheme,
        val shouldMoveMap: Boolean,
        val shouldAnimateMap: Boolean
    )

}

class SafeAreaLocationViewModelImpl(
    private val safeAreaRepository: SafeAreaRepository,
    private val geocoderRepository: GeocoderRepository,
    settingsNavigation: SettingsNavigation,
    moreNavigation: TagMoreNavigation,
    settings: SettingsRepository,
    contentCreatorRepository: ContentCreatorRepository,
    context: Context,
    private val isSettings: Boolean,
    private val addingDeviceId: String,
    currentId: String
): SafeAreaLocationViewModel() {

    companion object {
        private const val DEFAULT_RADIUS = 500f
    }

    private val navigation = if(isSettings) {
        settingsNavigation
    }else{
        moreNavigation
    }

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private var hasChanges = false

    private val safeArea = MutableStateFlow<State.Loaded?>(null)
    private val insets = MutableStateFlow<Insets?>(null)
    private val shouldMoveMap = MutableStateFlow(true)
    private val shouldAnimateMap = MutableStateFlow(true)
    private val id = currentId.takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString()

    private val currentLocation = resumeBus.flatMapLatest {
        if(context.hasLocationPermissions()) {
            contentCreatorRepository.wrapLocationAsFlow()
        }else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val firstLocation = flow {
        //Try to get the current location, with a timeout of 2500ms if not in range
        emit(withTimeoutOrNull(2500L) {
            currentLocation.firstNotNull()
        })
    }

    private val currentSafeArea = flow {
        if(currentId.isNotEmpty()) {
            val areas = safeAreaRepository.getSafeAreas().first().filterIsInstance<Location>()
            emit(areas.firstOrNull { it.id == currentId })
        }else emit(null)
    }.map {
        it?.let { area ->
            State.Loaded(
                id = area.id,
                name = area.name,
                center = area.latLng,
                radius = area.radius,
                lastExitTimestamp = area.lastExitTimestamp,
                exitBuffer = area.exitBuffer,
                activeDeviceIds = area.activeDeviceIds
            )
        }
    }

    private val mapOptions = combine(
        currentLocation,
        settings.mapTheme.asFlow(),
        settings.mapStyle.asFlow(),
        shouldMoveMap,
        shouldAnimateMap
    ) { location, theme, style, shouldMoveMap, shouldAnimateMap ->
        MapOptions(location, style, theme, shouldMoveMap, shouldAnimateMap)
    }

    override var hasShownToast = false

    override val state = combine(
        currentSafeArea,
        firstLocation,
        safeArea,
        mapOptions,
        insets
    ) { current, location, safeArea, mapOptions, insets ->
        when {
            //Always use modified safeArea if it's set
            safeArea != null -> safeArea.copy(insets = insets, mapOptions = mapOptions)
            //Then use the current if it's set
            current != null -> current.copy(insets = insets, mapOptions = mapOptions)
            //If not, generate one using the current Wi-Fi network if available
            else -> {
                State.Loaded(
                    id = id,
                    center = location?.toLatLng() ?: return@combine State.NoLocation(insets),
                    radius = DEFAULT_RADIUS,
                    insets = insets,
                    mapOptions = mapOptions
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading(insets.value))

    override val events = MutableSharedFlow<Event>()

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onInsetsChanged(insets: Insets) {
        viewModelScope.launch {
            this@SafeAreaLocationViewModelImpl.insets.emit(insets)
        }
    }

    override fun hasChanges(): Boolean {
        return hasChanges
    }

    override fun onMapMoved() {
        viewModelScope.launch {
            shouldMoveMap.emit(false)
        }
    }

    override fun onCenterChanged(center: LatLng, shouldAnimate: Boolean) {
        viewModelScope.launch {
            hasChanges = true
            shouldAnimateMap.emit(shouldAnimate)
            update {
                copy(center = center)
            }
        }
    }

    override fun onRadiusChanged(radius: Float) {
        viewModelScope.launch {
            hasChanges = true
            update {
                copy(radius = radius)
            }
        }
    }

    override fun onNameChanged(name: String) {
        viewModelScope.launch {
            hasChanges = true
            update {
                copy(name = name.takeIf { it.isNotBlank() })
            }
        }
    }

    override fun onExitBufferChanged(exitBuffer: ExitBuffer) {
        viewModelScope.launch {
            hasChanges = true
            update {
                copy(exitBuffer = exitBuffer)
            }
        }
    }

    override fun onSaveClicked() {
        val current = (state.value as? State.Loaded) ?: return
        viewModelScope.launch {
            val activeDeviceIds = addingDeviceId.takeIf { it.isNotEmpty() }?.let {
                current.activeDeviceIds.plus(it)
            } ?: current.activeDeviceIds
            val name = current.name
                ?: geocoderRepository.geocode(current.center, true)
                ?: run {
                    events.emit(Event.NAME_GENERATION_FAILED)
                    return@launch
                }
            val area = Location(
                id = current.id,
                name = name,
                latLng = current.center,
                radius = current.radius,
                isActive = false,
                exitBuffer = current.exitBuffer,
                lastExitTimestamp = current.lastExitTimestamp,
                activeDeviceIds = activeDeviceIds
            ).let {
                currentLocation.value?.let { location ->
                    it.copy(isActive = it.matches(location))
                } ?: it
            }
            safeAreaRepository.updateSafeArea(area)
            if(isSettings) {
                navigation.navigateUpTo(R.id.safeAreaListFragment)
            }else{
                navigation.navigateUpTo(R.id.tagMoreNotifyDisconnectFragment)
            }
        }
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onDeleteClicked() {
        val current = (state.value as? State.Loaded) ?: return
        viewModelScope.launch {
            safeAreaRepository.deleteLocationSafeArea(current.id)
            navigation.navigateBack()
        }
    }

    override fun onCenterClicked() {
        viewModelScope.launch {
            shouldMoveMap.emit(true)
        }
    }

    private suspend fun update(block: State.Loaded.() -> State.Loaded) {
        val current = (state.value as? State.Loaded) ?: return
        safeArea.emit(block(current))
    }

    private fun AndroidLocation.toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

}