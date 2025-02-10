package com.kieronquinn.app.utag.ui.screens.widget.history

import android.content.Context
import android.widget.RemoteViews
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.WidgetContainerNavigation
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository.AppWidgetConfig
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.ui.screens.widget.history.WidgetHistoryFragment.Companion.KEY_TAG_PICKER
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WidgetHistoryViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>
    abstract var hasChanges: Boolean

    abstract fun onDeviceClicked()
    abstract fun onDeviceChanged(deviceId: String)
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
            val device: Device?,
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

    data class Device(val id: String, val label: String)

    enum class Event {
        NO_DEVICE, SET_RESULT_AND_CLOSE
    }

}

class WidgetHistoryViewModelImpl(
    private val widgetRepository: HistoryWidgetRepository,
    private val navigation: WidgetContainerNavigation,
    smartTagRepository: SmartTagRepository,
    historyRepository: LocationHistoryRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    context: Context,
    appWidgetId: Int,
    callingPackage: String
): WidgetHistoryViewModel() {

    private val current = flow {
        emit(widgetRepository.getWidget(appWidgetId))
    }

    private val modified = MutableStateFlow<AppWidgetConfig?>(null)
    private val isTagStateLoading = MutableStateFlow(false)
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

    private val deviceId = config.filterNotNull().mapLatest { config ->
        config.deviceId
    }.distinctUntilChanged()

    private val tagState = deviceId.flatMapLatest { deviceId ->
        isTagStateLoading.emit(true)
        smartTagRepository.getTagState(
            deviceId ?: return@flatMapLatest flowOf(null)
        )
    }.onEach {
        isTagStateLoading.emit(false)
    }

    private val historyState = deviceId.mapLatest { deviceId ->
        historyRepository.getLocationHistory(deviceId ?: return@mapLatest null)
    }

    private val previewState = combine(
        config.filterNotNull(),
        historyState,
        tagState
    ) { config, historyState, tagState ->
        when {
            historyState != null && tagState != null && historyState.deviceId == tagState.deviceId -> {
                if(tagState is TagState.Loaded) {
                    widgetRepository.getRemoteViews(
                        historyState,
                        tagState,
                        config,
                        width,
                        height
                    )?.let {
                        PreviewState.Loaded(it)
                    } ?: PreviewState.Error
                }else{
                    PreviewState.Error
                }
            }
            tagState != null -> PreviewState.Loading
            else -> PreviewState.None
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, PreviewState.Loading)

    override val state = combine(
        config.filterNotNull(),
        tagState,
        previewState,
        biometricsEnabled.asFlow(),
        isTagStateLoading
    ) { config, tagState, previewState, biometrics, loading ->
        if(loading) {
            return@combine State.Loading
        }
        val device = when (tagState) {
            null -> null //Not set yet
            is TagState.Loaded -> Device(tagState.deviceId, tagState.device.label)
            else -> return@combine State.Error
        }
        State.Loaded(
            config.appWidgetId,
            config.packageName,
            biometrics,
            previewState,
            device,
            config.mapStyle,
            config.mapTheme
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onDeviceClicked() {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            navigation.navigate(
                WidgetHistoryFragmentDirections
                    .actionWidgetHistoryFragmentToWidgetDevicePickerFragment(
                        isMultiSelect = false,
                        key = KEY_TAG_PICKER,
                        title = R.string.widget_configuration_history_device_title,
                        selectedDeviceIds = listOfNotNull(state.device?.id).toTypedArray(),
                        popUpTo = R.id.widgetHistoryFragment,
                        knownDeviceIds = emptyArray() //Show all
                    )
            )
        }
    }

    override fun onDeviceChanged(deviceId: String) {
        updateConfig {
            copy(deviceId = deviceId).also {
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
            if(current.deviceId == null) {
                events.emit(Event.NO_DEVICE)
                return@launch
            }
            widgetRepository.updateWidget(current)
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