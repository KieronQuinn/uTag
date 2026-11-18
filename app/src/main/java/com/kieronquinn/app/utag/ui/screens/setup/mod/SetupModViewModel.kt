package com.kieronquinn.app.utag.ui.screens.setup.mod

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.repositories.DownloadRepository
import com.kieronquinn.app.utag.repositories.DownloadRepository.DownloadRequest
import com.kieronquinn.app.utag.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.repositories.UpdateRepository
import com.kieronquinn.app.utag.ui.screens.setup.mod.SetupModViewModel.State.Error.ErrorReason
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.utils.extensions.createApkInstallIntent
import com.kieronquinn.app.utag.utils.extensions.isOneUI
import com.kieronquinn.app.utag.utils.extensions.isOneUICompatible
import com.kieronquinn.app.utag.xposed.Xposed
import com.kieronquinn.app.utag.xposed.extensions.isPackageInstalled
import com.kieronquinn.app.utag.xposed.extensions.isSmartThingsModded
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

abstract class SetupModViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onStartDownloadClicked()
    abstract fun onAcceptOneUIClicked()
    abstract fun onUninstallClicked()
    abstract fun onInstallClicked()
    abstract fun onSetupClicked()
    abstract fun onRetryClicked()
    abstract fun onCloseClicked()
    abstract fun onNext()

    sealed class State {
        data object Loading: State()
        data class Info(val fileSize: Long): State()
        data class OneUI(val compatible: Boolean): State()
        data object StartingDownload: State()
        data class Downloading(val id: Long, val progress: Int, val progressText: String): State()
        data class Uninstall(val installUri: Uri): State()
        data class Install(val installUri: Uri): State()
        data object SettingUpHooks: State()
        data object Setup: State()
        data object Complete: State()
        data class Error(val reason: ErrorReason): State() {
            enum class ErrorReason(@StringRes val message: Int) {
                FAILED_TO_GET(R.string.setup_mod_error_failed_to_get),
                FAILED_TO_DOWNLOAD(R.string.setup_mod_error_failed_to_download),
                HOOKS_TIMEOUT(R.string.setup_mod_error_hooks_timeout),
                MODULE_TIMEOUT(R.string.setup_mod_error_module_timeout),
                NO_INSTALLER(R.string.setup_mod_error_no_installer)
            }
        }
    }

    companion object {
        /**
         *  How long to wait for SmartThings to report that the hooks are set up before assuming
         *  the modded build is never going to answer (ie. it crashes on launch).
         */
        const val HOOKS_TIMEOUT = 30_000L
    }

}

class SetupModViewModelImpl(
    private val smartThingsRepository: SmartThingsRepository,
    private val navigation: SetupNavigation,
    private val downloadRepository: DownloadRepository,
    updateRepository: UpdateRepository,
    private val context: Context
): SetupModViewModel() {

    private val backingState = MutableStateFlow<State>(State.Loading)
    private val downloadRequest = MutableStateFlow<DownloadRequest?>(null)
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val oneUIDismissed = MutableStateFlow(false)

    private val downloadState = downloadRequest.flatMapLatest {
        if(it != null) {
            downloadRepository.download(it)
        }else flowOf(null)
    }

    private val isSmartThingsInstalled = resumeBus.mapLatest {
        context.packageManager.isPackageInstalled(PACKAGE_NAME_ONECONNECT)
    }

    private val isSmartThingsModded = resumeBus.mapLatest {
        context.packageManager.isSmartThingsModded()
    }

    private val isOneUI = resumeBus.mapLatest {
        context.isOneUI()
    }

    private val isOneUiCompatible = resumeBus.mapLatest {
        context.isOneUICompatible()
    }

    private val description = context.getString(R.string.download_manager_description_mod)

    private val downloadTitle = { version: String ->
        context.getString(R.string.update_notification_mod, version)
    }

    private val moduleState = resumeBus.flatMapLatest {
        flow {
            emit(ModuleCheck.Pending)
            if(!context.packageManager.isPackageInstalled(PACKAGE_NAME_ONECONNECT)) {
                emit(ModuleCheck.Ready(null))
                return@flow
            }
            emit(withTimeoutOrNull(SetupModViewModel.HOOKS_TIMEOUT) {
                ModuleCheck.Ready(smartThingsRepository.getModuleStateForRecovery())
            } ?: ModuleCheck.TimedOut)
        }.catch { emit(ModuleCheck.TimedOut) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ModuleCheck.Pending)

    private val modRelease = flow {
        emit(updateRepository.getModRelease())
    }

    private val areHooksSetup = resumeBus.flatMapLatest {
        flow {
            emit(false)
            emit(withTimeoutOrNull(SetupModViewModel.HOOKS_TIMEOUT) {
                smartThingsRepository.areHooksReadyForRecovery()
            } ?: false)
        }.catch { emit(false) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val hookFinishedReceiver = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            context.broadcastReceiverAsFlow(IntentFilter(Xposed.ACTION_HOOKING_FINISHED))
                .collect { intent ->
                    try {
                        intent.verifySecurity(PACKAGE_NAME_ONECONNECT)
                    }catch (_: RuntimeException) {
                        // Ignore missing, invalid, or malformed security tags.
                        return@collect
                    }
                    hookFinishedReceiver.value = true
                }
        }
    }

    private val hooksReady = combine(
        hookFinishedReceiver,
        areHooksSetup
    ) { finished, setup ->
        finished || setup
    }

    private val oneUI = combine(
        isOneUI,
        isOneUiCompatible,
        oneUIDismissed
    ) { isOneUI, oneUiCompatible: Boolean, oneUIDismissed ->
        when {
            //User has accepted OneUI warning
            oneUIDismissed -> null
            //Not OneUI
            !isOneUI -> null
            //Compatibility check
            else -> oneUiCompatible
        }
    }

    private val smartThings = combine(
        isSmartThingsInstalled,
        isSmartThingsModded,
        hooksReady,
        oneUI
    ) { installed, modded, hooksReady, isOneUICompatible ->
        SmartThingsState(
            installed,
            modded,
            hooksReady,
            isOneUICompatible
        )
    }

    private val setupState = combine(
        backingState,
        modRelease,
        smartThings,
        moduleState,
        downloadState
    ) { backing, release, smartThings, moduleCheck, download ->
        val smartThingsInstalled = smartThings.isSmartThingsInstalled
        val modInstalled = smartThings.isSmartThingsModded
        val hooksReady = smartThings.hooksReady
        val isOneUICompatible = smartThings.isOneUICompatible
        when {
            backing is State.Loading && moduleCheck is ModuleCheck.Pending -> State.Loading
            backing is State.Loading && moduleCheck is ModuleCheck.TimedOut && smartThingsInstalled -> {
                when {
                    hooksReady -> State.Complete
                    modInstalled -> State.Error(ErrorReason.HOOKS_TIMEOUT)
                    else -> State.Error(ErrorReason.MODULE_TIMEOUT)
                }
            }
            backing is State.Loading &&
                (moduleCheck as? ModuleCheck.Ready)?.module.canContinue() -> State.Complete
            isOneUICompatible != null -> State.OneUI(isOneUICompatible)
            //If the user hasn't interacted yet, show info
            backing is State.Loading && release != null -> {
                State.Info(release.fileSize)
            }
            //If the download has started, show progress or completed states
            backing is State.StartingDownload -> {
                when {
                    release == null -> {
                        State.Error(ErrorReason.FAILED_TO_GET)
                    }
                    //Not yet started, set the URL and return starting
                    download == null -> {
                        downloadRequest.emit(
                            DownloadRequest(
                                release.download,
                                downloadTitle(release.version),
                                description,
                                release.downloadFilename
                            )
                        )
                        State.StartingDownload
                    }
                    download is DownloadState.Progress -> {
                        State.Downloading(download.id, download.percentage, download.progressText)
                    }
                    download is DownloadState.Failed -> State.Error(ErrorReason.FAILED_TO_DOWNLOAD)
                    download is DownloadState.DownloadComplete -> {
                        when {
                            smartThingsInstalled && !modInstalled -> {
                                //The user needs to uninstall the old version first
                                State.Uninstall(download.file)
                            }
                            !modInstalled -> {
                                //No old version, skip to installing
                                State.Install(download.file)
                            }
                            !hooksReady -> State.SettingUpHooks
                            else -> {
                                //Mod is now installed, show setup
                                State.Setup
                            }
                        }
                    }
                    else -> State.StartingDownload
                }
            }
            else -> backing
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    //Start the timer only while waiting for hooks, and restart it after returning to setup.
    override val state = combine(setupState, resumeBus) { state, _ -> state }
        .flatMapLatest { current ->
            if(current != State.SettingUpHooks) return@flatMapLatest flowOf(current)
            flow {
                emit(current)
                delay(SetupModViewModel.HOOKS_TIMEOUT)
                emit(State.Error(ErrorReason.HOOKS_TIMEOUT))
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private fun ModuleState?.canContinue(): Boolean {
        return when(this) {
            //We allow newer & outdated builds here to not block the user
            is ModuleState.Installed,
            is ModuleState.Newer,
            is ModuleState.Outdated -> true
            else -> false
        }
    }

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
            when(state.value) {
                //User has returned, probably from setting up, move to complete
                is State.Setup -> backingState.emit(State.Complete)
                else -> {
                    //No-op
                }
            }
        }
    }

    override fun onStartDownloadClicked() {
        viewModelScope.launch {
            if(state.value is State.Info) {
                backingState.emit(State.StartingDownload)
            }
        }
    }

    override fun onAcceptOneUIClicked() {
        viewModelScope.launch {
            oneUIDismissed.emit(true)
        }
    }

    override fun onUninstallClicked() {
        viewModelScope.launch {
            //Allow a cancelled uninstall to show the timeout again, even if no resume occurs.
            hookFinishedReceiver.value = false
            resumeBus.value += 1
            Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${PACKAGE_NAME_ONECONNECT}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }.also {
                navigation.navigate(it)
            }
        }
    }

    override fun onInstallClicked() {
        viewModelScope.launch {
            val uri = (state.value as? State.Install)?.installUri ?: return@launch
            hookFinishedReceiver.value = false
            val intent = context.createApkInstallIntent(uri)
            if(intent == null) {
                backingState.emit(State.Error(ErrorReason.NO_INSTALLER))
                return@launch
            }
            navigation.navigate(intent) {
                backingState.emit(State.Error(ErrorReason.NO_INSTALLER))
            }
        }
    }

    override fun onSetupClicked() {
        viewModelScope.launch {
            navigation.navigate(
                Intent(smartThingsRepository.getLaunchIntent() ?: return@launch)
            )
        }
    }

    override fun onRetryClicked() {
        resumeBus.value += 1
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onNext() {
        viewModelScope.launch {
            navigation.navigate(
                SetupModFragmentDirections.actionSetupModFragmentToSetupPermissionsFragment()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        val currentId = (state.value as? State.Downloading)?.id
        if(currentId != null) {
            downloadRepository.cancelDownload(currentId)
        }
        downloadRepository.clearCache()
    }

    private data class SmartThingsState(
        val isSmartThingsInstalled: Boolean,
        val isSmartThingsModded: Boolean,
        val hooksReady: Boolean,
        val isOneUICompatible: Boolean? //null = not OneUI
    )

    private sealed class ModuleCheck {
        data object Pending: ModuleCheck()
        data object TimedOut: ModuleCheck()
        data class Ready(val module: ModuleState?): ModuleCheck()
    }

}
