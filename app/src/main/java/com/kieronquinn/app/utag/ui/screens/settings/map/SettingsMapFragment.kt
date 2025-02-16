package com.kieronquinn.app.utag.ui.screens.settings.map

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.map.SettingsMapViewModel.State
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.dropDownPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsMapFragment: BaseSettingsFragment(), BackAvailable {

    private val locationPermissionPrompt = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if(granted.all { it.value }) {
            viewModel.onMapsMyLocationChanged(true)
        }else{
            Toast.makeText(
                requireContext(), R.string.settings_map_show_location_toast, Toast.LENGTH_LONG
            ).show()
            viewModel.onShowPermissions()
        }
        viewModel.onResume()
    }

    private val viewModel by viewModel<SettingsMapViewModel>()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
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
        switchPreference {
            title = getString(R.string.settings_map_show_location_title)
            summary = getString(R.string.settings_map_show_location_content)
            isChecked = state.mapsLocationEnabled
            onChange<Boolean> { enabled ->
                if(enabled && !requireContext().hasLocationPermissions()) {
                    locationPermissionPrompt.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }else{
                    viewModel.onMapsMyLocationChanged(enabled)
                }
            }
        }
        dropDownPreference {
            title = getString(R.string.settings_map_style_title)
            value = state.mapStyle.name
            summary = getString(state.mapStyle.label)
            entries = MapStyle.entries.toTypedArray().map { getString(it.label) }.toTypedArray()
            entryValues = MapStyle.entries.toTypedArray().map { it.name }.toTypedArray()
            onChange<String> {
                val option = MapStyle.valueOf(it)
                viewModel.onMapsStyleChanged(option)
            }
        }
        dropDownPreference {
            title = getString(R.string.settings_map_theme_title)
            value = state.mapTheme.name
            summary = if(state.mapStyle == MapStyle.NORMAL) {
                getString(state.mapTheme.label)
            }else{
                getString(R.string.settings_map_theme_disabled)
            }
            entries = MapTheme.entries.toTypedArray()
                .map { getString(it.label) }.toTypedArray()
            entryValues = MapTheme.entries.toTypedArray().map { it.name }.toTypedArray()
            isEnabled = state.mapStyle == MapStyle.NORMAL
            onChange<String> {
                val option = MapTheme.valueOf(it)
                viewModel.onMapsThemeChanged(option)
            }
        }
        switchPreference {
            title = getString(R.string.settings_map_show_buildings_title)
            summary = if(state.mapStyle == MapStyle.NORMAL) {
                getString(R.string.settings_map_show_buildings_content)
            }else{
                getString(R.string.settings_map_theme_disabled)
            }
            isChecked = state.showBuildings && state.mapStyle == MapStyle.NORMAL
            isEnabled = state.mapStyle == MapStyle.NORMAL
            onChange<Boolean> {
                viewModel.onShowBuildingsChanged(it)
            }
        }
        switchPreference {
            title = getString(R.string.settings_map_swap_location_history_title)
            summary = getString(R.string.settings_map_swap_location_history_content)
            isChecked = state.swapLocationHistory
            onChange<Boolean> {
                viewModel.onSwapLocationHistoryChanged(it)
            }
        }
    }
    
}