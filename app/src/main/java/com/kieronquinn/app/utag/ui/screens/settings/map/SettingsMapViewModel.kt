package com.kieronquinn.app.utag.ui.screens.settings.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsMapViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onMapsMyLocationChanged(enabled: Boolean)
    abstract fun onMapsStyleChanged(style: MapStyle)
    abstract fun onMapsThemeChanged(theme: MapTheme)
    abstract fun onShowBuildingsChanged(enabled: Boolean)
    abstract fun onSwapLocationHistoryChanged(enabled: Boolean)
    abstract fun onShowPermissions()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val mapsLocationEnabled: Boolean,
            val mapStyle: MapStyle,
            val mapTheme: MapTheme,
            val showBuildings: Boolean,
            val swapLocationHistory: Boolean
        ): State()
    }

}

class SettingsMapViewModelImpl(
    private val navigation: SettingsNavigation,
    context: Context,
    settingsRepository: SettingsRepository
): SettingsMapViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val mapsShowLocation = settingsRepository.isMyLocationEnabled
    private val mapStyle = settingsRepository.mapStyle
    private val mapTheme = settingsRepository.mapTheme
    private val mapShowBuildings = settingsRepository.mapShowBuildings
    private val mapSwapLocationHistory = settingsRepository.mapSwapLocationHistory

    private val hasLocationPermission = resumeBus.mapLatest {
        context.hasLocationPermissions()
    }

    private val mapsLocationEnabled = combine(
        hasLocationPermission,
        mapsShowLocation.asFlow()
    ) { permission, setting ->
        permission && setting
    }

    override val state = combine(
        mapsLocationEnabled,
        mapStyle.asFlow(),
        mapTheme.asFlow(),
        mapShowBuildings.asFlow(),
        mapSwapLocationHistory.asFlow()
    ) { location, style, theme, buildings, swapLocationHistory ->
        State.Loaded(location, style, theme, buildings, swapLocationHistory)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onMapsMyLocationChanged(enabled: Boolean) {
        viewModelScope.launch {
            mapsShowLocation.set(enabled)
        }
    }

    override fun onMapsStyleChanged(style: MapStyle) {
        viewModelScope.launch {
            mapStyle.set(style)
        }
    }

    override fun onMapsThemeChanged(theme: MapTheme) {
        viewModelScope.launch {
            mapTheme.set(theme)
        }
    }

    override fun onShowBuildingsChanged(enabled: Boolean) {
        viewModelScope.launch {
            mapShowBuildings.set(enabled)
        }
    }

    override fun onSwapLocationHistoryChanged(enabled: Boolean) {
        viewModelScope.launch {
            mapSwapLocationHistory.set(enabled)
        }
    }

    override fun onShowPermissions() {
        viewModelScope.launch {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                setData(Uri.parse("package:${BuildConfig.APPLICATION_ID}"))
            }
            navigation.navigate(intent)
        }
    }

}