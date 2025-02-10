package com.kieronquinn.app.utag.ui.screens.widget.location

import android.content.Context
import android.widget.RemoteViews
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.WidgetContainerNavigation
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.repositories.WidgetRepository
import com.kieronquinn.app.utag.repositories.WidgetRepository.AppWidgetConfig
import com.kieronquinn.app.utag.ui.screens.widget.history.WidgetHistoryViewModel.PreviewState
import com.kieronquinn.app.utag.ui.screens.widget.location.WidgetLocationFragment.Companion.KEY_ON_CLICK_PICKER
import com.kieronquinn.app.utag.ui.screens.widget.location.WidgetLocationFragment.Companion.KEY_STATUS_PICKER
import com.kieronquinn.app.utag.ui.screens.widget.location.WidgetLocationFragment.Companion.KEY_TAG_PICKER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WidgetLocationViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>
    abstract var hasChanges: Boolean

    abstract fun onDevicesClicked()
    abstract fun onDevicesChanged(deviceIds: Set<String>)
    abstract fun onClickDeviceClicked()
    abstract fun onClickDeviceChanged(deviceId: String)
    abstract fun onStatusDeviceClicked()
    abstract fun onStatusDeviceChanged(deviceId: String)
    abstract fun onMapStyleChanged(style: MapStyle)
    abstract fun onMapThemeChanged(theme: MapTheme)
    abstract fun onSaveClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val appWidgetId: Int,
            val callingPackage: String,
            val biometricsEnabled: Boolean,
            val previewState: PreviewState = PreviewState.Loading,
            val devices: List<Device>,
            val onClick: Device?,
            val status: Device?,
            val mapStyle: MapStyle,
            val mapTheme: MapTheme
        ): State()
        data object Error: State()
    }

    sealed class PreviewState {
        data object Loading: PreviewState()
        data object None: PreviewState()
        data class Loaded(val remoteViews: RemoteViews): PreviewState()
        data object Error: PreviewState()
    }

    data class Device(val id: String, val label: String, val available: Boolean)

    enum class Event {
        NO_DEVICES, SET_RESULT_AND_CLOSE
    }

}

class WidgetLocationViewModelImpl(
    private val widgetRepository: WidgetRepository,
    private val navigation: WidgetContainerNavigation,
    smartTagRepository: SmartTagRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    context: Context,
    appWidgetId: Int,
    callingPackage: String
): WidgetLocationViewModel() {

    private val current = flow {
        emit(widgetRepository.getWidget(appWidgetId))
    }

    private val modified = MutableStateFlow<AppWidgetConfig?>(null)
    private val isLoading = MutableStateFlow(false)
    private val biometricsEnabled = encryptedSettingsRepository.biometricPromptEnabled
    private val width = context.resources.getDimensionPixelSize(R.dimen.widget_preview_width)
    private val height = context.resources.getDimensionPixelSize(R.dimen.widget_preview_height)

    override val events = MutableSharedFlow<Event>()
    override var hasChanges = false

    private val config = combine(
        current,
        modified
    ) { current, modified ->
        modified ?: current ?: AppWidgetConfig(appWidgetId, callingPackage)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val deviceIds = config.filterNotNull().map {
        it.deviceIds
    }.distinctUntilChanged()

    private val tagStates = deviceIds.flatMapLatest { ids ->
        isLoading.emit(true)
        if(ids.isEmpty()) return@flatMapLatest flowOf(emptyList())
        val flows = ids.map { deviceId ->
            smartTagRepository.getTagState(deviceId)
        }.toTypedArray()
        combine(*flows) {
            it.toList()
        }
    }.onEach {
        isLoading.emit(false)
    }

    private val previewState = combine(
        config.filterNotNull(),
        tagStates
    ) { config, tagStates ->
        val fallbackDeviceId = tagStates.firstOrNull()?.deviceId
        val configWithFallback = config.copy(
            openDeviceId = config.openDeviceId ?: fallbackDeviceId,
            statusDeviceId = config.statusDeviceId ?: fallbackDeviceId,
        )
        if(tagStates.isNotEmpty()) {
            widgetRepository.getRemoteViews(tagStates, configWithFallback, width, height)?.let {
                PreviewState.Loaded(it)
            } ?: PreviewState.Error
        }else{
            PreviewState.None
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, PreviewState.Loading)

    override val state = combine(
        config.filterNotNull(),
        tagStates,
        previewState,
        biometricsEnabled.asFlow(),
        isLoading
    ) { config, tagStates, previewState, biometrics, loading ->
        if(loading) {
            return@combine State.Loading
        }
        val errors = tagStates.filterIsInstance<TagState.Error>()
        if(errors.isNotEmpty()) {
            return@combine State.Error
        }
        val devices = tagStates.filterIsInstance<TagState.Loaded>().map {
            Device(it.deviceId, it.device.label, !it.requiresAgreement())
        }.sortedBy { it.label.lowercase() }
        val clickDevice = devices.firstOrNull { it.id == config.openDeviceId }
        val statusDevice = devices.firstOrNull { it.id == config.statusDeviceId }
        State.Loaded(
            config.appWidgetId,
            config.packageName,
            biometrics,
            previewState,
            devices,
            clickDevice,
            statusDevice,
            config.mapStyle,
            config.mapTheme
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onDevicesClicked() {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            navigation.navigate(
                WidgetLocationFragmentDirections
                    .actionWidgetLocationFragmentToWidgetDevicePickerFragment(
                        isMultiSelect = true,
                        key = KEY_TAG_PICKER,
                        title = R.string.widget_configuration_location_devices_title,
                        selectedDeviceIds = state.devices.map { it.id }.toTypedArray(),
                        popUpTo = R.id.widgetLocationFragment,
                        knownDeviceIds = emptyArray() //Show all
                    )
            )
        }
    }

    override fun onDevicesChanged(deviceIds: Set<String>) {
        val current = config.value ?: return
        updateConfig {
            //Remove the open device ID if the user has deselected it
            val openDeviceId = current.openDeviceId?.takeIf { deviceIds.contains(it) }
            //And the same for the status device ID
            val statusDeviceId = current.statusDeviceId?.takeIf { deviceIds.contains(it) }
            copy(
                deviceIds = deviceIds,
                openDeviceId = openDeviceId,
                statusDeviceId = statusDeviceId
            ).also {
                hasChanges = true
            }
        }
    }

    override fun onClickDeviceClicked() {
        val state = state.value as? State.Loaded ?: return
        val clickDevice = state.onClick ?: state.devices.firstOrNull() ?: return
        viewModelScope.launch {
            navigation.navigate(
                WidgetLocationFragmentDirections
                    .actionWidgetLocationFragmentToWidgetDevicePickerFragment(
                        isMultiSelect = false,
                        key = KEY_ON_CLICK_PICKER,
                        title = R.string.widget_configuration_location_click_device_title,
                        selectedDeviceIds = arrayOf(clickDevice.id),
                        popUpTo = R.id.widgetLocationFragment,
                        //Only show selected devices
                        knownDeviceIds = state.devices.map { it.id }.toTypedArray()
                    )
            )
        }
    }

    override fun onClickDeviceChanged(deviceId: String) {
        updateConfig {
            copy(openDeviceId = deviceId).also {
                hasChanges = true
            }
        }
    }

    override fun onStatusDeviceClicked() {
        val state = state.value as? State.Loaded ?: return
        val statusDevice = state.status ?: state.devices.firstOrNull() ?: return
        viewModelScope.launch {
            navigation.navigate(
                WidgetLocationFragmentDirections
                    .actionWidgetLocationFragmentToWidgetDevicePickerFragment(
                        isMultiSelect = false,
                        key = KEY_STATUS_PICKER,
                        title = R.string.widget_configuration_location_status_device_title,
                        selectedDeviceIds = arrayOf(statusDevice.id),
                        popUpTo = R.id.widgetLocationFragment,
                        //Only show selected devices
                        knownDeviceIds = state.devices.map { it.id }.toTypedArray()
                    )
            )
        }
    }

    override fun onStatusDeviceChanged(deviceId: String) {
        updateConfig {
            copy(statusDeviceId = deviceId).also {
                hasChanges = true
            }
        }
    }

    override fun onMapStyleChanged(style: MapStyle) {
        updateConfig {
            copy(mapStyle = style).also {
                hasChanges = true
            }
        }
    }

    override fun onMapThemeChanged(theme: MapTheme) {
        updateConfig {
            copy(mapTheme = theme).also {
                hasChanges = true
            }
        }
    }

    override fun onSaveClicked() {
        val current = config.value ?: return
        viewModelScope.launch {
            val fallback = (state.value as? State.Loaded)?.devices?.firstOrNull()?.id
            if(current.deviceIds.isEmpty()) {
                events.emit(Event.NO_DEVICES)
                return@launch
            }
            //Set the open & status device IDs to the first if they're not set, to match the UI
            val modified = current.copy(
                openDeviceId = current.openDeviceId ?: fallback,
                statusDeviceId = current.statusDeviceId ?: fallback,
            )
            widgetRepository.updateWidget(modified)
            events.emit(Event.SET_RESULT_AND_CLOSE)
        }
    }

    private fun updateConfig(block: AppWidgetConfig.() -> AppWidgetConfig) {
        val current = config.value ?: return
        viewModelScope.launch {
            modified.emit(block(current))
        }
    }

}