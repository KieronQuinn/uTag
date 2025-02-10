package com.kieronquinn.app.utag.ui.screens.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.model.Release
import com.kieronquinn.app.utag.networking.model.github.ModRelease
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.repositories.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class UpdatesViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onUTagUpdateClicked()
    abstract fun onSmartThingsUpdateClicked()
    abstract fun onAutoUpdatesChanged(enabled: Boolean)
    abstract fun onRerunSetupClicked()

    abstract fun onContributorsClicked()
    abstract fun onDonateClicked()
    abstract fun onGitHubClicked()
    abstract fun onCrowdinClicked()
    abstract fun onLibrariesClicked()
    abstract fun onBlueskyClicked()
    abstract fun onXdaClicked()

    abstract fun onDebugModeToggled(): Boolean?

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val uTagVersion: String,
            val uTagUpdate: Release?,
            val smartThingsVersion: String?,
            val smartThingsUpdate: ModRelease?,
            val smartThingsState: SmartThingsState?,
            val autoUpdatesAvailable: Boolean,
            val autoUpdatesEnabled: Boolean,
            val debugModeVisible: Boolean
        ): State()
    }

    enum class SmartThingsState {
        EXTERNAL, PLAY, MODDED
    }

}

class UpdatesViewModelImpl(
    private val navigation: SettingsNavigation,
    private val authRepository: AuthRepository,
    context: Context,
    smartThingsRepository: SmartThingsRepository,
    updateRepository: UpdateRepository,
    settingsRepository: SettingsRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository
): UpdatesViewModel() {

    companion object {
        private const val LINK_BLUESKY = "https://kieronquinn.co.uk/redirect/uTag/bluesky"
        private const val LINK_DONATE = "https://kieronquinn.co.uk/redirect/uTag/donate"
        private const val LINK_GITHUB = "https://kieronquinn.co.uk/redirect/uTag/github"
        private const val LINK_CROWDIN = "https://kieronquinn.co.uk/redirect/uTag/crowdin"
        private const val LINK_XDA = "https://kieronquinn.co.uk/redirect/uTag/xda"
    }

    private val autoUpdatesEnabled = settingsRepository.autoUpdatesEnabled
    private val debugModeVisible = encryptedSettingsRepository.debugModeVisible
    private val debugModeEnabled = encryptedSettingsRepository.debugModeEnabled
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val uTagUpdate = resumeBus.mapLatest {
        updateRepository.getUpdate()
    }

    private val smartThingsUpdate = resumeBus.mapLatest {
        updateRepository.getModRelease()
    }

    private val moduleState = resumeBus.mapLatest {
        smartThingsRepository.getModuleState()
    }

    private val updatesAvailable = resumeBus.mapLatest {
        NotificationChannel.UPDATES.isEnabled(context)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val options = combine(
        autoUpdatesEnabled.asFlow(),
        debugModeVisible.asFlow()
    ) { autoUpdates, debugMode ->
        Pair(autoUpdates, debugMode)
    }

    override val state = combine(
        uTagUpdate,
        smartThingsUpdate,
        moduleState,
        options,
        updatesAvailable.filterNotNull()
    ) { utag, smartthings, module, options, updatesAvailable ->
        val updates = options.first
        val debugMode = options.second
        val smartThingsVersionCode = when(module) {
            is ModuleState.Installed -> module.versionCode
            is ModuleState.Outdated -> module.versionCode
            is ModuleState.Newer -> module.versionCode
            else -> null
        }
        val smartThingsVersionName = when(module) {
            is ModuleState.Installed -> module.versionName
            is ModuleState.Outdated -> module.versionName
            is ModuleState.Newer -> module.versionName
            else -> null
        }
        val smartThingsState = when {
            //Can't be modded and installed by Play
            module?.wasInstalledOnPlay == true -> SmartThingsState.PLAY
            module?.isUTagBuild == true -> SmartThingsState.MODDED
            module?.isUTagBuild == false -> SmartThingsState.EXTERNAL
            else -> null
        }
        State.Loaded(
            BuildConfig.TAG_NAME,
            utag, //Updates repository already checks tag
            smartThingsVersionName,
            smartthings?.takeUnless { it.versionCode == smartThingsVersionCode },
            smartThingsState,
            updates,
            updatesAvailable,
            debugMode
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onUTagUpdateClicked() {
        val release = (state.value as? State.Loaded)?.uTagUpdate ?: return
        viewModelScope.launch {
            navigation.navigate(
                UpdatesFragmentDirections.actionUpdatesFragmentToUTagUpdateFragment(release)
            )
        }
    }

    override fun onSmartThingsUpdateClicked() {
        val release = (state.value as? State.Loaded)?.smartThingsUpdate ?: return
        viewModelScope.launch {
            navigation.navigate(
                UpdatesFragmentDirections.actionUpdatesFragmentToSmartThingsUpdateFragment(release)
            )
        }
    }

    override fun onAutoUpdatesChanged(enabled: Boolean) {
        viewModelScope.launch {
            autoUpdatesEnabled.set(enabled)
        }
    }

    override fun onRerunSetupClicked() {
        authRepository.clearCredentials()
    }

    override fun onContributorsClicked() {
        viewModelScope.launch {
            navigation.navigate(
                UpdatesFragmentDirections.actionUpdatesFragmentToSettingsContributorsFragment()
            )
        }
    }

    override fun onDonateClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_DONATE.toIntent())
        }
    }

    override fun onGitHubClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_GITHUB.toIntent())
        }
    }

    override fun onCrowdinClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_CROWDIN.toIntent())
        }
    }

    override fun onLibrariesClicked() {
        viewModelScope.launch {
            navigation.navigate(
                UpdatesFragmentDirections.actionUpdatesFragmentToOssLicensesMenuActivity()
            )
        }
    }

    override fun onBlueskyClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_BLUESKY.toIntent())
        }
    }

    override fun onXdaClicked() {
        viewModelScope.launch {
            navigation.navigate(LINK_XDA.toIntent())
        }
    }

    override fun onDebugModeToggled(): Boolean? {
        val current = (state.value as? State.Loaded)?.debugModeVisible ?: return null
        val new = !current
        viewModelScope.launch {
            debugModeVisible.set(new)
            if(!new) {
                //Disable debug mode too if the toggle will be hidden
                debugModeEnabled.set(false)
            }
        }
        return new
    }

    private fun String.toIntent(): Intent {
        return CustomTabsIntent.Builder().build().intent.apply {
            data = Uri.parse(this@toIntent)
        }
    }

}