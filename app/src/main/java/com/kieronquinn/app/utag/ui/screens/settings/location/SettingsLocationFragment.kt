package com.kieronquinn.app.utag.ui.screens.settings.location

import android.icu.util.LocaleData
import android.icu.util.LocaleData.MeasurementSystem
import android.icu.util.ULocale
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceGroup
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.RefreshPeriod
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.WidgetRefreshPeriod
import com.kieronquinn.app.utag.repositories.SettingsRepository.Units
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.location.SettingsLocationViewModel.ChaserSettings
import com.kieronquinn.app.utag.ui.screens.settings.location.SettingsLocationViewModel.LocationSettings
import com.kieronquinn.app.utag.ui.screens.settings.location.SettingsLocationViewModel.State
import com.kieronquinn.app.utag.ui.screens.settings.location.SettingsLocationViewModel.UtsSettings
import com.kieronquinn.app.utag.ui.screens.settings.location.SettingsLocationViewModel.UwbSettings
import com.kieronquinn.app.utag.ui.screens.settings.location.SettingsLocationViewModel.WidgetSettings
import com.kieronquinn.app.utag.ui.screens.settings.location.refreshfrequency.RefreshFrequencyFragment.Companion.setupRefreshFrequencyListener
import com.kieronquinn.app.utag.ui.screens.settings.location.staleness.LocationStalenessFragment.Companion.setupLocationStalenessListener
import com.kieronquinn.app.utag.ui.screens.settings.location.widgetfrequency.WidgetFrequencyFragment.Companion.setupWidgetFrequencyListener
import com.kieronquinn.app.utag.utils.extensions.label
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.dropDownPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import com.kieronquinn.app.utag.utils.preferences.switchPreferenceScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsLocationFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsLocationViewModel>()

    private val isSystemMeasurementImperial by lazy {
        LocaleData.getMeasurementSystem(ULocale.getDefault()) != MeasurementSystem.SI
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupListeners() {
        setupRefreshFrequencyListener {
            viewModel.onLocationPeriodChanged(it)
        }
        setupLocationStalenessListener {
            viewModel.onLocationStalenessChanged(it)
        }
        setupWidgetFrequencyListener {
            viewModel.onWidgetPeriodChanged(it)
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state) {
            is State.Loading -> setLoading(true)
            is State.Loaded -> handleLoaded(state)
        }
    }

    private fun handleLoaded(state: State.Loaded) = setPreferences {
        location(state.location)
        chaser(state.chaser)
        uts(state.uts)
        widget(state.widget)
        uwb(state.uwb)
    }

    private fun PreferenceGroup.location(state: LocationSettings) = preferenceCategory(
        "scan_location"
    ) {
        title = getString(R.string.settings_location_location)
        preference {
            title = getString(R.string.settings_location_location_period_title)
            summary = getString(R.string.settings_location_location_period_content,
                getString(state.period.label)
            )
            onClick {
                viewModel.onLocationPeriodClicked()
            }
        }
        switchPreference {
            title = getString(R.string.settings_location_location_battery_saver_title)
            summary = getString(R.string.settings_location_location_battery_saver_content)
            isChecked = state.batterySaver && state.period != RefreshPeriod.NEVER
            isEnabled = state.period != RefreshPeriod.NEVER
            onChange<Boolean> {
                viewModel.onLocationBatterySaverChanged(it)
            }
        }
        preference {
            title = getString(R.string.settings_location_location_staleness_title)
            summary = getString(
                R.string.settings_location_location_staleness_content,
                getString(state.staleness.label)
            )
            onClick {
                viewModel.onLocationStalenessClicked()
            }
        }
        preference {
            title = getString(R.string.settings_location_location_safe_area_title)
            summary = getString(R.string.settings_location_location_safe_area_content)
            onClick {
                viewModel.onSafeAreasClicked()
            }
        }
    }

    private fun PreferenceGroup.chaser(state: ChaserSettings) = preferenceCategory("chaser") {
        title = getString(R.string.settings_location_category_chaser_title)
        if(state.available) {
            switchPreferenceScreen {
                title = getString(R.string.settings_location_chaser_title)
                summary = getString(R.string.settings_location_chaser_content)
                isChecked = state.enabled
                onChange<Boolean> {
                    viewModel.onChaserChanged(it)
                }
                onClick {
                    viewModel.onChaserClicked()
                }
            }
        }else{
            preference {
                title = getString(R.string.settings_location_chaser_title)
                summary = getString(R.string.settings_location_chaser_disabled)
                isEnabled = false
            }
        }
    }

    private fun PreferenceGroup.uts(state: UtsSettings) = preferenceCategory("uts") {
        title = getString(R.string.settings_location_category_uts_title)
        switchPreferenceScreen {
            title = getString(R.string.settings_location_uts_title)
            summary = getString(R.string.settings_location_uts_content)
            isChecked = state.enabled
            onChange<Boolean> {
                viewModel.onUtsChanged(it)
            }
            onClick {
                viewModel.onUtsClicked()
            }
        }
    }

    private fun PreferenceGroup.uwb(state: UwbSettings) = preferenceCategory("scan_nearby") {
        title = getString(R.string.settings_location_category_search_nearby_title)
        switchPreference {
            title = getString(R.string.settings_location_uwb_title)
            summary = if(state.available) {
                getString(R.string.settings_location_uwb_content)
            }else{
                getString(R.string.settings_location_uwb_disabled)
            }
            isChecked = state.available && state.enabled
            isEnabled = state.available
            onChange<Boolean> {
                viewModel.onUseUwbChanged(it)
            }
        }
        switchPreference {
            isChecked = state.allowLongDistance
            title = getString(R.string.settings_location_uwb_long_distance_title)
            summary = getString(R.string.settings_location_uwb_long_distance_content)
            isEnabled = state.enabled
            onChange<Boolean> {
                viewModel.onAllowLongDistanceUwbChanged(it)
            }
        }
        if(state.available) {
            val default = if(isSystemMeasurementImperial) {
                Units.IMPERIAL
            }else{
                Units.METRIC
            }
            dropDownPreference {
                title = getString(R.string.settings_location_units_title)
                value = state.units.name
                summary = getString(state.units.label, getString(default.label))
                entries = Units.entries.toTypedArray().map {
                    getString(it.label, getString(default.label))
                }.toTypedArray()
                entryValues = Units.entries.toTypedArray().map { it.name }.toTypedArray()
                onChange<String> {
                    val option = Units.valueOf(it)
                    viewModel.onUnitsChanged(option)
                }
            }
        }
    }

    private fun PreferenceGroup.widget(state: WidgetSettings) = preferenceCategory("widget") {
        title = getString(R.string.settings_location_category_widget_title)
        preference {
            title = getString(R.string.settings_location_widget_period_title)
            summary = if(state.available) {
                getString(R.string.settings_location_widget_period_content, getString(state.period.label))
            }else{
                getString(R.string.settings_location_widget_period_content_disabled)
            }
            isEnabled = state.available
            onClick { viewModel.onWidgetPeriodClicked() }
        }
        switchPreference {
            title = getString(R.string.settings_location_widget_battery_saver_title)
            summary = getString(R.string.settings_location_widget_battery_saver_content)
            isChecked = state.available && state.batterySaver &&
                    state.period != WidgetRefreshPeriod.NEVER
            isEnabled = state.available && state.period != WidgetRefreshPeriod.NEVER
            onChange<Boolean> { viewModel.onWidgetBatterySaverChanged(it) }
        }
    }
    
}