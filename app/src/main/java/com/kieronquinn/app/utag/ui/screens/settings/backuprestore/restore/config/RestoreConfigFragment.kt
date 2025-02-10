package com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.config

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.BackupRepository.LoadBackupResult.ErrorReason
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.config.RestoreConfigViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RestoreConfigFragment: BaseSettingsFragment(), BackAvailable {

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onResume()
    }

    private val args by navArgs<RestoreConfigFragmentArgs>()

    private val viewModel by viewModel<RestoreConfigViewModel> {
        parametersOf(args.uri)
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
            is State.Loaded -> handleContent(state)
            is State.Error -> showErrorDialog(state.reason)
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences {
        preferenceCategory("restore_config") {
            title = getString(R.string.restore_category_options)
            if(state.config.hasAutomationConfigs) {
                switchPreference {
                    title = getString(R.string.restore_automation_configs_title)
                    summary = getString(R.string.restore_automation_configs_content)
                    isChecked = state.config.shouldRestoreAutomationConfigs
                    onChange<Boolean> {
                        viewModel.onAutomationConfigsChanged(it)
                    }
                }
            }
            if(state.config.hasFindMyDeviceConfigs) {
                switchPreference {
                    title = getString(R.string.restore_find_my_device_configs_title)
                    isChecked = state.config.shouldRestoreFindMyDeviceConfigs
                    onChange<Boolean> {
                        viewModel.onFindMyDeviceConfigsChanged(it)
                    }
                }
            }
            if(state.config.hasLocationSafeAreas) {
                switchPreference {
                    title = getString(R.string.restore_location_safe_areas_title)
                    summary = if (!state.hasPermissions) {
                        getString(R.string.restore_additional_permissions_required)
                    } else null
                    isChecked = state.config.shouldRestoreLocationSafeAreas &&
                            state.hasPermissions
                    onChange<Boolean> {
                        when {
                            state.hasPermissions -> {
                                viewModel.onLocationSafeAreasChanged(it)
                            }

                            !state.hasLocationPermission -> {
                                locationPermission.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                    )
                                )
                            }

                            !state.hasBackgroundLocationPermission -> {
                                locationPermission.launch(
                                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                )
                            }
                        }
                    }
                }
            }
            if(state.config.hasNotifyDisconnectConfigs) {
                switchPreference {
                    title = getString(R.string.restore_notify_disconnect_configs_title)
                    isChecked = state.config.shouldRestoreNotifyDisconnectConfigs
                    onChange<Boolean> {
                        viewModel.onNotifyDisconnectConfigsChanged(it)
                    }
                }
            }
            if(state.config.hasPassiveModeConfigs) {
                switchPreference {
                    title = getString(R.string.restore_passive_mode_configs_title)
                    isChecked = state.config.shouldRestorePassiveModeConfigs
                    onChange<Boolean> {
                        viewModel.onPassiveModeConfigsChanged(it)
                    }
                }
            }
            if(state.config.hasWifiSafeAreas) {
                switchPreference {
                    title = getString(R.string.restore_wifi_safe_areas_title)
                    summary = if (!state.hasPermissions) {
                        getString(R.string.restore_additional_permissions_required)
                    } else null
                    isChecked = state.config.shouldRestoreWifiSafeAreas &&
                            state.hasPermissions
                    onChange<Boolean> {
                        when {
                            state.hasPermissions -> {
                                viewModel.onWiFiSafeAreasChanged(it)
                            }

                            !state.hasLocationPermission -> {
                                locationPermission.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                    )
                                )
                            }

                            !state.hasBackgroundLocationPermission -> {
                                locationPermission.launch(
                                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                )
                            }
                        }
                    }
                }
            }
            if(state.config.hasUnknownTags) {
                switchPreference {
                    title = getString(R.string.restore_unknown_tags_title)
                    summary = getString(R.string.restore_unknown_tags_subtitle)
                    isChecked = state.config.shouldRestoreUnknownTags
                    onChange<Boolean> {
                        viewModel.onUnknownTagsChanged(it)
                    }
                }
            }
            if(state.config.hasSettings) {
                switchPreference {
                    title = getString(R.string.restore_settings_title)
                    summary = getString(R.string.restore_settings_content)
                    isChecked = state.config.shouldRestoreSettings
                    onChange<Boolean> {
                        viewModel.onPassiveModeConfigsChanged(it)
                    }
                }
            }
        }
        preferenceCategory("restore_config_info") {
            actionCardPreference {
                title = getString(R.string.restore_footer_title)
                summary = getText(R.string.restore_footer_content)
                addButton(getString(R.string.restore_footer_restore)) {
                    viewModel.onRestoreClicked()
                }
            }
        }
    }

    private fun showErrorDialog(reason: ErrorReason) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.restore_error_dialog_title)
            setMessage(reason.description)
            setPositiveButton(R.string.restore_error_dialog_close) { _, _ ->
                viewModel.close()
            }
            setCancelable(false)
        }.show()
    }

}