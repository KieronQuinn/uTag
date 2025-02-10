package com.kieronquinn.app.utag.ui.screens.safearea.type

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.safearea.type.SafeAreaTypeViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SafeAreaTypeFragment: BaseSettingsFragment(), BackAvailable {

    private val permissionsRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onResume()
        if(!permissions.all { it.value }) {
            Toast.makeText(requireContext(), R.string.safe_area_type_toast, Toast.LENGTH_LONG).show()
            viewModel.openSettings()
        }
    }

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissions ->
        viewModel.onResume()
        if(!permissions) {
            Toast.makeText(requireContext(), R.string.safe_area_type_toast, Toast.LENGTH_LONG).show()
            viewModel.openSettings()
        }
    }

    private val args by navArgs<SafeAreaTypeFragmentArgs>()

    private val viewModel by viewModel<SafeAreaTypeViewModel> {
        parametersOf(args.isSettings, args.addingDeviceId)
    }

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
            is State.LocationPermission -> handleLocationPermission()
            is State.BackgroundLocationPermission -> handleBackgroundLocationPermission()
            is State.Types -> handleTypes()
        }
    }

    private fun handleLocationPermission() = setPreferences {
        actionCardPreference {
            title = getString(R.string.safe_area_type_location_permission_title)
            summary = getString(R.string.safe_area_type_location_permission_content)
            addButton(getString(R.string.safe_area_type_grant)) {
                permissionsRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun handleBackgroundLocationPermission() = setPreferences {
        actionCardPreference {
            title = getString(R.string.safe_area_type_background_location_permission_title)
            summary = getString(R.string.safe_area_type_background_location_permission_content)
            addButton(getString(R.string.safe_area_type_grant)) {
                permissionRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun handleTypes() = setPreferences {
        preference {
            title = getString(R.string.safe_area_type_location_title)
            summary = getString(R.string.safe_area_type_location_content)
            onClick {
                viewModel.onLocationClicked()
            }
        }
        preference {
            title = getString(R.string.safe_area_type_wifi_title)
            summary = getString(R.string.safe_area_type_wifi_content)
            onClick {
                viewModel.onWiFiClicked()
            }
        }
    }

}