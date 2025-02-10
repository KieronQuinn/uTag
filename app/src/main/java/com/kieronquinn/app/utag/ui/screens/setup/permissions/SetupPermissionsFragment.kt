package com.kieronquinn.app.utag.ui.screens.setup.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.setup.permissions.SetupPermissionsViewModel.State
import com.kieronquinn.app.utag.utils.extensions.showPermissionRationale
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupPermissionsFragment: BaseSettingsFragment(), BackAvailable {

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if(result.any { requireActivity().showPermissionRationale(it.key) }) {
            //At least one permission can no longer be requested, open app info
            viewModel.showAppInfo()
        }
        viewModel.refreshPermissions()
    }

    private val viewModel by viewModel<SetupPermissionsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
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
            is State.Request -> setContent(state)
            is State.Complete -> {
                setLoading(true)
                viewModel.navigateToNext()
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun setContent(state: State.Request) {
        setPreferences {
            if(!state.hasGrantedSmartThings) {
                actionCardPreference {
                    title = getString(R.string.permission_smartthings_title)
                    summary = getText(R.string.permission_smartthings_content)
                    addButton(getString(R.string.permission_smartthings_grant)) {
                        viewModel.showAppInfo()
                    }
                }
            }
            if(!state.hasGrantedNotification) {
                actionCardPreference {
                    title = getString(R.string.permission_notification_title)
                    summary = getText(R.string.permission_notification_content)
                    addButton(getString(R.string.permission_notification_grant)) {
                        requestPermission.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
            }
            if(!state.hasSetAnalytics) {
                actionCardPreference {
                    title = getString(R.string.permission_analytics_title)
                    summary = getString(R.string.permission_analytics_content)
                    addButton(getString(R.string.permission_analytics_allow)) {
                        viewModel.onAnalyticsEnableClicked()
                    }
                    addButton(getString(R.string.permission_analytics_deny)) {
                        viewModel.onAnalyticsDisableClicked()
                    }
                }
            }
            if(!state.hasIgnoredBatteryOptimisation) {
                actionCardPreference {
                    title = getString(R.string.permission_battery_title)
                    summary = getText(R.string.permission_battery_content)
                    addButton(getString(R.string.permission_battery_disable)) {
                        viewModel.onDisableBatteryOptimisationClicked()
                    }
                }
            }
            if(!state.hasIgnoredSmartThingsBatteryOptimisation) {
                actionCardPreference {
                    title = getString(R.string.permission_smartthings_battery_title)
                    summary = getText(R.string.permission_smartthings_battery_content)
                    addButton(getString(R.string.permission_battery_disable)) {
                        viewModel.onDisableSmartThingsBatteryOptimisationClicked()
                    }
                }
            }
        }
    }

}