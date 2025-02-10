package com.kieronquinn.app.utag.ui.screens.safearea.type

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.repositories.UTagServiceRepository
import com.kieronquinn.app.utag.utils.extensions.hasBackgroundLocationPermission
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SafeAreaTypeViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onLocationClicked()
    abstract fun onWiFiClicked()
    abstract fun openSettings()

    sealed class State {
        data object Loading: State()
        data object LocationPermission: State()
        data object BackgroundLocationPermission: State()
        data object Types: State()
    }

}

class SafeAreaTypeViewModelImpl(
    private val serviceRepository: UTagServiceRepository,
    moreNavigation: TagMoreNavigation,
    settingsNavigation: SettingsNavigation,
    context: Context,
    private val isSettings: Boolean,
    private val addingDeviceId: String,
): SafeAreaTypeViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private var hadPermissions: Boolean? = null

    private val navigation = if(isSettings) {
        settingsNavigation
    }else{
        moreNavigation
    }

    override val state = resumeBus.mapLatest {
        when {
            !context.hasLocationPermissions() -> State.LocationPermission
            !context.hasBackgroundLocationPermission() -> State.BackgroundLocationPermission
            else -> State.Types
        }
    }.onEach {
        when(it) {
            is State.LocationPermission, State.BackgroundLocationPermission -> {
                hadPermissions = false
            }
            is State.Types -> {
                if(hadPermissions == false) {
                    //Permissions have changed to granted, update the service
                    serviceRepository.runWithService { service ->
                        service.onLocationPermissionsChanged()
                    }
                }
                hadPermissions = true
            }
            else -> {
                //No-op
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onLocationClicked() {
        viewModelScope.launch {
            val args = bundleOf(
                "is_settings" to isSettings,
                "current_id" to "",
                "adding_device_id" to addingDeviceId,
            )
            if(isSettings) {
                navigation.navigate(
                    R.id.action_safeAreaTypeFragment_to_safeAreaLocationFragment,
                    args
                )
            }else{
                navigation.navigate(
                    R.id.action_safeAreaTypeFragment2_to_safeAreaLocationFragment2,
                    args
                )
            }
        }
    }

    override fun onWiFiClicked() {
        viewModelScope.launch {
            val args = bundleOf(
                "is_settings" to isSettings,
                "current_id" to "",
                "adding_device_id" to addingDeviceId,
            )
            if(isSettings) {
                navigation.navigate(
                    R.id.action_safeAreaTypeFragment_to_safeAreaWiFiFragment,
                    args
                )
            }else{
                navigation.navigate(
                    R.id.action_safeAreaTypeFragment2_to_safeAreaWiFiFragment2,
                    args
                )
            }
        }
    }

    override fun openSettings() {
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