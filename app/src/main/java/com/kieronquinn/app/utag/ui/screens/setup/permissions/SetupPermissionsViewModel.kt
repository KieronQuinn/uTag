package com.kieronquinn.app.utag.ui.screens.setup.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.repositories.ChaserRepository
import com.kieronquinn.app.utag.repositories.ChaserRepository.ChaserCertificate
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.getIgnoreBatteryOptimisationsIntent
import com.kieronquinn.app.utag.xposed.extensions.hasPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SetupPermissionsViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun refreshPermissions()
    abstract fun showAppInfo()
    abstract fun onDisableBatteryOptimisationClicked()
    abstract fun onDisableSmartThingsBatteryOptimisationClicked()
    abstract fun onAnalyticsEnableClicked()
    abstract fun onAnalyticsDisableClicked()
    abstract fun navigateToNext()

    sealed class State {
        data object Loading: State()
        data class Request(
            val hasGrantedSmartThings: Boolean,
            val hasGrantedNotification: Boolean,
            val hasIgnoredBatteryOptimisation: Boolean,
            val hasIgnoredSmartThingsBatteryOptimisation: Boolean,
            val hasSetAnalytics: Boolean
        ): State()
        data object Complete: State()
    }

}

@SuppressLint("BatteryLife")
class SetupPermissionsViewModelImpl(
    private val navigation: SetupNavigation,
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    private val chaserRepository: ChaserRepository,
    smartThingsRepository: SmartThingsRepository,
    settingsRepository: SettingsRepository,
    context: Context
): SetupPermissionsViewModel() {

    private val refreshBus = MutableStateFlow(System.currentTimeMillis())
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val checkAnalyticsBus = MutableStateFlow(System.currentTimeMillis())
    private val analyticsEnabled = settingsRepository.analyticsEnabled

    private val analyticsSet = checkAnalyticsBus.mapLatest {
        analyticsEnabled.exists()
    }

    private val notificationPermission = refreshBus.mapLatest {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else true
    }

    private val smartThingsPermissions = refreshBus.mapLatest {
        smartThingsRepository.hasRequiredPermissions()
    }

    private val batteryOptimisationDisabled = refreshBus.mapLatest {
        powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
    }

    private val smartThingsBatteryOptimisationDisabled = refreshBus.mapLatest {
        powerManager.isIgnoringBatteryOptimizations(PACKAGE_NAME_ONECONNECT)
    }

    override val state = combine(
        smartThingsPermissions,
        notificationPermission,
        batteryOptimisationDisabled,
        smartThingsBatteryOptimisationDisabled,
        analyticsSet
    ) { smartThings, notification, battery, batterySmartThings, analyticsSet ->
        if (!smartThings || !notification || !battery || !batterySmartThings || !analyticsSet) {
            State.Request(
                smartThings,
                notification,
                battery,
                batterySmartThings,
                analyticsSet
            )
        } else {
            State.Complete
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun refreshPermissions() {
        viewModelScope.launch {
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    override fun showAppInfo() {
        viewModelScope.launch {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                setData(Uri.parse("package:$PACKAGE_NAME_ONECONNECT"))
            }
            navigation.navigate(intent)
        }
    }

    override fun onDisableBatteryOptimisationClicked() {
        viewModelScope.launch {
            val intent = getIgnoreBatteryOptimisationsIntent(BuildConfig.APPLICATION_ID)
            navigation.navigate(intent)
        }
    }

    override fun onDisableSmartThingsBatteryOptimisationClicked() {
        viewModelScope.launch {
            val intent = getIgnoreBatteryOptimisationsIntent(PACKAGE_NAME_ONECONNECT)
            navigation.navigate(intent)
        }
    }

    override fun onAnalyticsEnableClicked() {
        viewModelScope.launch {
            analyticsEnabled.set(true)
            checkAnalyticsBus.emit(System.currentTimeMillis())
        }
    }

    override fun onAnalyticsDisableClicked() {
        viewModelScope.launch {
            analyticsEnabled.set(false)
            checkAnalyticsBus.emit(System.currentTimeMillis())
        }
    }

    override fun navigateToNext() {
        viewModelScope.launch {
            when {
                chaserSetupAvailable() -> {
                    navigation.navigate(SetupPermissionsFragmentDirections
                        .actionSetupPermissionsFragmentToSetupChaserFragment())
                }
                !encryptedSettingsRepository.utsScanEnabled.exists() -> {
                    navigation.navigate(SetupPermissionsFragmentDirections
                        .actionSetupPermissionsFragmentToSetupUtsFragment())
                }
                else -> {
                    navigation.navigate(SetupPermissionsFragmentDirections
                        .actionSetupPermissionsFragmentToSetupAccountFragment())
                }
            }
        }
    }

    private suspend fun chaserSetupAvailable(): Boolean {
        return !encryptedSettingsRepository.networkContributionsEnabled.exists()
                && chaserRepository.certificate.firstNotNull() is ChaserCertificate.Certificate
    }

}