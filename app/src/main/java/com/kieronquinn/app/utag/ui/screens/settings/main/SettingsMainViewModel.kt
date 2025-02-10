package com.kieronquinn.app.utag.ui.screens.settings.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.networking.model.smartthings.UserInfoResponse
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.repositories.UserRepository
import com.kieronquinn.app.utag.ui.activities.AuthResponseActivity
import com.kieronquinn.app.utag.utils.extensions.getAuthIntent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsMainViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onResume()
    abstract fun onSignOutClicked()
    abstract fun onSmartThingsClicked()
    abstract fun onSmartThingsPermissionClicked()
    abstract fun onUpdateSnackbarVisiblityChanged(visible: Boolean)
    abstract fun onAdvancedClicked()

    abstract fun onLocationClicked()
    abstract fun onMapClicked()
    abstract fun onSecurityClicked()
    abstract fun onBackupRestoreClicked()
    abstract fun onFaqClicked()
    abstract fun onWikiClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val moduleState: ModuleState?,
            val hasPermissions: Boolean,
            val accountInfo: UserInfoResponse?,
            val updateSnackbarVisible: Boolean
        ): State()
    }

    enum class Event {
        LOGOUT_ERROR
    }

}

class SettingsMainViewModelImpl(
    private val authRepository: AuthRepository,
    private val navigation: SettingsNavigation,
    smartThingsRepository: SmartThingsRepository,
    settingsRepository: SettingsRepository,
    userRepository: UserRepository
): SettingsMainViewModel() {

    companion object {
        private const val LINK_WIKI = "https://kieronquinn.co.uk/redirect/uTag/wiki"
    }

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val updateSnackbarVisible = MutableStateFlow(false)
    private val contentCreatorMode = settingsRepository.contentCreatorModeEnabled

    override val events = MutableSharedFlow<Event>()

    private val accountInfo = combine(
        contentCreatorMode.asFlow(),
        resumeBus
    ) { enabled, _ ->
        userRepository.getUserInfo()?.let {
            it.copy(email = if(enabled) "@" else it.email)
        }
    }

    private val moduleState = resumeBus.mapLatest {
        smartThingsRepository.getModuleState()
    }

    private val permissionsGranted = resumeBus.mapLatest {
        smartThingsRepository.hasRequiredPermissions()
    }

    private val options = combine(
        moduleState,
        accountInfo,
        permissionsGranted
    ) { state, accountInfo, permissionsGranted ->
        Options(state, accountInfo, permissionsGranted)
    }

    override val state = combine(
        options,
        updateSnackbarVisible
    ) { options, updateSnackbarVisible ->
        State.Loaded(
            options.moduleState,
            options.permissionsGranted,
            options.accountInfo,
            updateSnackbarVisible
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onSmartThingsClicked() {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                `package` = PACKAGE_NAME_ONECONNECT
            }
            navigation.navigate(intent)
        }
    }

    override fun onSmartThingsPermissionClicked() {
        viewModelScope.launch {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                data = Uri.parse("package:$PACKAGE_NAME_ONECONNECT")
            }
            navigation.navigate(intent)
        }
    }

    override fun onUpdateSnackbarVisiblityChanged(visible: Boolean) {
        viewModelScope.launch {
            updateSnackbarVisible.emit(visible)
        }
    }

    override fun onAdvancedClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsMainFragmentDirections
                .actionSettingsMainFragmentToSettingsAdvancedFragment())
        }
    }

    override fun onSignOutClicked() {
        viewModelScope.launch {
            val logoutUrl = authRepository.generateLogoutUrl()
            if(logoutUrl != null) {
                AuthResponseActivity.setEnabled(true)
                navigation.navigate(getAuthIntent(logoutUrl))
            }else{
                events.emit(Event.LOGOUT_ERROR)
            }
        }
    }

    override fun onLocationClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsMainFragmentDirections
                .actionSettingsMainFragmentToSettingsLocationFragment())
        }
    }

    override fun onMapClicked() {
        viewModelScope.launch {
            navigation.navigate(
                SettingsMainFragmentDirections.actionSettingsMainFragmentToSettingsMapFragment()
            )
        }
    }

    override fun onSecurityClicked() {
        viewModelScope.launch {
            navigation.navigate(
                SettingsMainFragmentDirections
                    .actionSettingsMainFragmentToSettingsSecurityFragment()
            )
        }
    }

    override fun onBackupRestoreClicked() {
        viewModelScope.launch {
            navigation.navigate(
                SettingsMainFragmentDirections
                    .actionSettingsMainFragmentToBackupRestoreFragment()
            )
        }
    }

    override fun onFaqClicked() {
        viewModelScope.launch {
            navigation.navigate(SettingsMainFragmentDirections
                .actionSettingsMainFragmentToSettingsFaqFragment())
        }
    }

    override fun onWikiClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_WIKI)
        }
    }

    private data class Options(
        val moduleState: ModuleState?,
        val accountInfo: UserInfoResponse?,
        val permissionsGranted: Boolean
    )

}