package com.kieronquinn.app.utag.ui.screens.settings.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.model.LocationStaleness
import com.kieronquinn.app.utag.repositories.ChaserRepository
import com.kieronquinn.app.utag.repositories.ChaserRepository.ChaserCertificate
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.RefreshPeriod
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.WidgetRefreshPeriod
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository.Units
import com.kieronquinn.app.utag.repositories.UwbRepository
import com.kieronquinn.app.utag.repositories.WidgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsLocationViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onLocationPeriodClicked()
    abstract fun onLocationPeriodChanged(period: RefreshPeriod)
    abstract fun onLocationBatterySaverChanged(enabled: Boolean)
    abstract fun onLocationStalenessClicked()
    abstract fun onLocationStalenessChanged(staleness: LocationStaleness)
    abstract fun onChaserClicked()
    abstract fun onChaserChanged(enabled: Boolean)
    abstract fun onUtsClicked()
    abstract fun onUtsChanged(enabled: Boolean)
    abstract fun onWidgetPeriodClicked()
    abstract fun onWidgetPeriodChanged(period: WidgetRefreshPeriod)
    abstract fun onWidgetBatterySaverChanged(enabled: Boolean)
    abstract fun onUseUwbChanged(enabled: Boolean)
    abstract fun onAllowLongDistanceUwbChanged(enabled: Boolean)
    abstract fun onUnitsChanged(units: Units)
    abstract fun onSafeAreasClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val uwb: UwbSettings,
            val location: LocationSettings,
            val chaser: ChaserSettings,
            val uts: UtsSettings,
            val widget: WidgetSettings
        ): State()
    }

    data class LocationSettings(
        val period: RefreshPeriod,
        val batterySaver: Boolean,
        val staleness: LocationStaleness
    )

    data class UwbSettings(
        val available: Boolean,
        val enabled: Boolean,
        val allowLongDistance: Boolean,
        val units: Units
    )

    data class WidgetSettings(
        val available: Boolean,
        val period: WidgetRefreshPeriod,
        val batterySaver: Boolean
    )

    data class ChaserSettings(
        val available: Boolean,
        val enabled: Boolean
    )

    data class UtsSettings(
        val enabled: Boolean
    )

}

class SettingsLocationViewModelImpl(
    private val navigation: SettingsNavigation,
    settingsRepository: SettingsRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    uwbRepository: UwbRepository,
    widgetRepository: WidgetRepository,
    historyWidgetRepository: HistoryWidgetRepository,
    chaserRepository: ChaserRepository
): SettingsLocationViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val useUwb = settingsRepository.useUwb
    private val allowLongDistanceUwb = settingsRepository.allowLongDistanceUwb
    private val units = settingsRepository.units
    private val locationPeriod = encryptedSettingsRepository.locationRefreshPeriod
    private val locationBatterySaver = encryptedSettingsRepository.locationOnBatterySaver
    private val locationStaleness = encryptedSettingsRepository.locationStaleness
    private val widgetPeriod = encryptedSettingsRepository.widgetRefreshPeriod
    private val widgetBatterySaver = encryptedSettingsRepository.widgetRefreshOnBatterySaver
    private val chaserEnabled = encryptedSettingsRepository.networkContributionsEnabled
    private val utsEnabled = encryptedSettingsRepository.utsScanEnabled

    private val chaserAvailable = chaserRepository.certificate.filterNotNull().map {
        it is ChaserCertificate.Certificate
    }

    private val uwbAvailable = resumeBus.mapLatest {
        uwbRepository.isUwbAvailable()
    }

    private val uwb = combine(
        uwbAvailable,
        useUwb.asFlow(),
        allowLongDistanceUwb.asFlow(),
        units.asFlow()
    ) { available, use, allowLongDistance, units ->
        UwbSettings(available, use, allowLongDistance, units)
    }

    private val location = combine(
        locationPeriod.asFlow(),
        locationBatterySaver.asFlow(),
        locationStaleness.asFlow()
    ) { location, battery, staleness ->
        LocationSettings(location, battery, staleness)
    }

    private val chaser = combine(
        chaserAvailable,
        chaserEnabled.asFlow()
    ) { available, enabled ->
        ChaserSettings(available, enabled)
    }

    private val uts = utsEnabled.asFlow().map { enabled ->
        UtsSettings(enabled)
    }

    private val widget = combine(
        widgetRepository.hasWidgets(),
        historyWidgetRepository.hasWidgets(),
        widgetPeriod.asFlow(),
        widgetBatterySaver.asFlow()
    ) { hasWidgets, hasHistoryWidgets, period, batterySaver ->
        WidgetSettings(hasWidgets && hasHistoryWidgets, period, batterySaver)
    }

    override val state = combine(
        uwb,
        location,
        chaser,
        uts,
        widget
    ) { uwb, location, chaser, uts, widget ->
        State.Loaded(uwb, location, chaser, uts, widget)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onUseUwbChanged(enabled: Boolean) {
        viewModelScope.launch {
            useUwb.set(enabled)
        }
    }

    override fun onAllowLongDistanceUwbChanged(enabled: Boolean) {
        viewModelScope.launch {
            allowLongDistanceUwb.set(enabled)
        }
    }

    override fun onUnitsChanged(units: Units) {
        viewModelScope.launch {
            this@SettingsLocationViewModelImpl.units.set(units)
        }
    }

    override fun onSafeAreasClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsLocationFragmentDirections
                .actionSettingsLocationFragmentToSafeAreaListFragment())
        }
    }

    override fun onLocationPeriodClicked() {
        val current = (state.value as? State.Loaded)?.location?.period ?: return
        viewModelScope.launch {
            navigation.navigate(SettingsLocationFragmentDirections
                .actionSettingsLocationFragmentToRefreshFrequencyFragment(current))
        }
    }

    override fun onLocationPeriodChanged(period: RefreshPeriod) {
        viewModelScope.launch {
            locationPeriod.set(period)
        }
    }

    override fun onLocationBatterySaverChanged(enabled: Boolean) {
        viewModelScope.launch {
            locationBatterySaver.set(enabled)
        }
    }

    override fun onLocationStalenessClicked() {
        val current = (state.value as? State.Loaded)?.location?.staleness ?: return
        viewModelScope.launch {
            navigation.navigate(SettingsLocationFragmentDirections
                .actionSettingsLocationFragmentToLocationStalenessFragment(current))
        }
    }

    override fun onLocationStalenessChanged(staleness: LocationStaleness) {
        viewModelScope.launch {
            locationStaleness.set(staleness)
        }
    }

    override fun onChaserClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsLocationFragmentDirections
                .actionSettingsLocationFragmentToSettingsChaserFragment())
        }
    }

    override fun onChaserChanged(enabled: Boolean) {
        viewModelScope.launch {
            chaserEnabled.set(enabled)
        }
    }

    override fun onUtsClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsLocationFragmentDirections
                .actionSettingsLocationFragmentToUnknownTagSettingsFragment())
        }
    }

    override fun onUtsChanged(enabled: Boolean) {
        viewModelScope.launch {
            utsEnabled.set(enabled)
        }
    }

    override fun onWidgetPeriodClicked() {
        val current = (state.value as? State.Loaded)?.widget?.period ?: return
        viewModelScope.launch {
            navigation.navigate(SettingsLocationFragmentDirections
                .actionSettingsLocationFragmentToWidgetFrequencyFragment(current))
        }
    }

    override fun onWidgetPeriodChanged(period: WidgetRefreshPeriod) {
        viewModelScope.launch {
            widgetPeriod.set(period)
        }
    }

    override fun onWidgetBatterySaverChanged(enabled: Boolean) {
        viewModelScope.launch {
            widgetBatterySaver.set(enabled)
        }
    }

}