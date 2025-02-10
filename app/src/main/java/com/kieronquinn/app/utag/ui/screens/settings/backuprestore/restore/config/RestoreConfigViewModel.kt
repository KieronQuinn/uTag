package com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.config

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.BackupRepository
import com.kieronquinn.app.utag.repositories.BackupRepository.LoadBackupResult
import com.kieronquinn.app.utag.repositories.BackupRepository.LoadBackupResult.ErrorReason
import com.kieronquinn.app.utag.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagBackup
import com.kieronquinn.app.utag.utils.extensions.hasBackgroundLocationPermission
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class RestoreConfigViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun close()
    abstract fun onAutomationConfigsChanged(enabled: Boolean)
    abstract fun onFindMyDeviceConfigsChanged(enabled: Boolean)
    abstract fun onLocationSafeAreasChanged(enabled: Boolean)
    abstract fun onNotifyDisconnectConfigsChanged(enabled: Boolean)
    abstract fun onPassiveModeConfigsChanged(enabled: Boolean)
    abstract fun onUnknownTagsChanged(enabled: Boolean)
    abstract fun onWiFiSafeAreasChanged(enabled: Boolean)
    abstract fun onSettingsChanged(enabled: Boolean)
    abstract fun onRestoreClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val backup: UTagBackup,
            val config: RestoreConfig,
            val hasLocationPermission: Boolean,
            val hasBackgroundLocationPermission: Boolean,
            val hasPermissions: Boolean,
        ): State()
        data class Error(val reason: ErrorReason): State()
    }

}

class RestoreConfigViewModelImpl(
    private val navigation: SettingsNavigation,
    context: Context,
    backupRepository: BackupRepository,
    uri: Uri
): RestoreConfigViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val restoreConfig = MutableStateFlow<RestoreConfig?>(null)

    private val loadBackup = flow {
        emit(backupRepository.loadBackup(uri))
    }.flowOn(Dispatchers.IO)

    private val hasLocationPermission = resumeBus.mapLatest {
        context.hasLocationPermissions()
    }

    private val hasBackgroundLocationPermission = resumeBus.mapLatest {
        context.hasBackgroundLocationPermission()
    }

    override val state = combine(
        loadBackup,
        restoreConfig,
        hasLocationPermission,
        hasBackgroundLocationPermission
    ) { backup, config, permission, backgroundPermission ->
        when(backup) {
            is LoadBackupResult.Success -> {
                State.Loaded(
                    backup.backup,
                    config ?: backup.config,
                    permission,
                    backgroundPermission,
                    permission && backgroundPermission
                )
            }
            is LoadBackupResult.Error -> {
                State.Error(backup.reason)
            }
        }

    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun close() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onAutomationConfigsChanged(enabled: Boolean) {
        update {
            copy(shouldRestoreAutomationConfigs = enabled)
        }
    }

    override fun onFindMyDeviceConfigsChanged(enabled: Boolean) {
        update {
            copy(shouldRestoreFindMyDeviceConfigs = enabled)
        }
    }

    override fun onLocationSafeAreasChanged(enabled: Boolean) {
        update {
            copy(shouldRestoreLocationSafeAreas = enabled)
        }
    }

    override fun onNotifyDisconnectConfigsChanged(enabled: Boolean) {
        update {
            copy(shouldRestoreNotifyDisconnectConfigs = enabled)
        }
    }

    override fun onPassiveModeConfigsChanged(enabled: Boolean) {
        update {
            copy(shouldRestorePassiveModeConfigs = enabled)
        }
    }

    override fun onUnknownTagsChanged(enabled: Boolean) {
        update {
            copy(shouldRestoreUnknownTags = enabled)
        }
    }

    override fun onWiFiSafeAreasChanged(enabled: Boolean) {
        update {
            copy(shouldRestoreWifiSafeAreas = enabled)
        }
    }

    override fun onSettingsChanged(enabled: Boolean) {
        update {
            copy(shouldRestoreSettings = enabled)
        }
    }

    override fun onRestoreClicked() {
        val state = state.value as? State.Loaded ?: return
        viewModelScope.launch {
            navigation.navigate(RestoreConfigFragmentDirections
                .actionRestoreConfigFragmentToRestoreProgressFragment(state.backup, state.config))
        }
    }

    private fun update(block: RestoreConfig.() -> RestoreConfig) {
        val current = (state.value as? State.Loaded)?.config ?: return
        viewModelScope.launch {
            restoreConfig.emit(block(current))
        }
    }

}