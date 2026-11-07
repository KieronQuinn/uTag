package com.kieronquinn.app.utag.ui.screens.update.smartthings

import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.networking.model.github.ModRelease
import com.kieronquinn.app.utag.repositories.DownloadRepository
import com.kieronquinn.app.utag.repositories.DownloadRepository.DownloadRequest
import com.kieronquinn.app.utag.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.utils.extensions.createApkInstallIntent
import com.kieronquinn.app.utag.xposed.Xposed
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

abstract class SmartThingsUpdateViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onDownloadBrowserClicked()
    abstract fun startDownload()
    abstract fun startInstall()
    abstract fun onRetryClicked()
    abstract fun onComplete()
    abstract fun onCloseClicked()

    sealed class State {
        data class Info(val release: ModRelease): State()
        data class Downloading(val downloadState: DownloadState.Progress): State()
        data class StartInstall(val uri: Uri): State()
        data object CheckingUpdate: State()
        data object SettingUpHooks: State()
        data object Complete: State()
        data class Failed(@StringRes val message: Int, val canRetry: Boolean = false): State()
    }

}

class SmartThingsUpdateViewModelImpl(
    private val navigation: SettingsNavigation,
    private val downloadRepository: DownloadRepository,
    private val smartThingsRepository: SmartThingsRepository,
    private val context: Context,
    release: ModRelease
) : SmartThingsUpdateViewModel() {

    private val updateRelease = release
    private val downloadRequest = MutableSharedFlow<DownloadRequest?>()
    private val downloadDescription = context.getString(R.string.download_manager_description_mod)
    private val resumeBus = MutableStateFlow(0L)
    private val hooksBus = MutableStateFlow(HookRequest())
    private val checks = MutableStateFlow(Checks())
    private val hookFinishedReceiver = MutableStateFlow(false)
    private var completionNavigationStarted = false

    init {
        viewModelScope.launch {
            resumeBus.collectLatest { generation ->
                val result = try {
                    withTimeoutOrNull(PROVIDER_TIMEOUT) {
                        ModuleCheck.Ready(smartThingsRepository.getModuleStateForRecovery())
                    } ?: ModuleCheck.TimedOut
                }catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                }catch (_: Exception) {
                    ModuleCheck.TimedOut
                }
                if(checks.value.generation == generation) {
                    checks.value = checks.value.copy(module = result)
                }
            }
        }
        viewModelScope.launch {
            hooksBus.collectLatest { request ->
                val result = try {
                    withTimeoutOrNull(PROVIDER_TIMEOUT) {
                        smartThingsRepository.areHooksReadyForRecovery()
                    } ?: false
                }catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                }catch (_: Exception) {
                    false
                }
                if(checks.value.generation == request.generation && hooksBus.value == request) {
                    checks.value = checks.value.copy(hooksReady = result)
                }
            }
        }
        viewModelScope.launch {
            context.broadcastReceiverAsFlow(IntentFilter(Xposed.ACTION_HOOKING_FINISHED))
                .filter { intent ->
                    try {
                        intent.verifySecurity(PACKAGE_NAME_ONECONNECT)
                        true
                    }catch (_: RuntimeException) {
                        false
                    }
                }.collect {
                    hookFinishedReceiver.value = true
                    if(checks.value.installPhase == InstallPhase.Returned) {
                        refreshHooks(checks.value.generation)
                    }
                }
        }
    }

    private val downloadTitle = { version: String ->
        context.getString(R.string.update_notification_mod, version)
    }

    private val updateDownload = downloadRequest.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(null)
        downloadRepository.download(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val updateState = combine(
        checks,
        updateDownload,
        hookFinishedReceiver
    ) { checks, download, hookFinished ->
        val moduleCheck = checks.module
        val module = (moduleCheck as? ModuleCheck.Ready)?.module
        val hooksReady = checks.hooksReady ||
            (checks.installPhase == InstallPhase.Idle && hookFinished)
        val moduleVersionCode = module.versionCodeOrNull()
        when {
            checks.installPhase == InstallPhase.Failed -> State.Failed(R.string.setup_mod_error_no_installer)
            checks.installPhase == InstallPhase.Requested -> State.CheckingUpdate
            download == null -> State.Info(release)
            moduleVersionCode != null && moduleVersionCode == release.versionCode -> {
                if(hooksReady) {
                    State.Complete
                }else{
                    State.SettingUpHooks
                }
            }
            download is DownloadState.Progress -> {
                State.Downloading(download)
            }
            download is DownloadState.DownloadComplete -> {
                when {
                    checks.installPhase != InstallPhase.Returned -> State.StartInstall(download.file)
                    moduleCheck is ModuleCheck.Pending -> State.CheckingUpdate
                    moduleCheck is ModuleCheck.TimedOut || moduleVersionCode == null -> {
                        State.Failed(R.string.setup_mod_error_module_timeout, canRetry = true)
                    }
                    else -> State.StartInstall(download.file)
                }
            }
            else -> State.Failed(R.string.update_dialog_failed_content)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Info(release))

    override val state = updateState.flatMapLatest { current ->
        if(current != State.SettingUpHooks) return@flatMapLatest flowOf(current)
        flow {
            emit(current)
            delay(PROVIDER_TIMEOUT)
            emit(State.Failed(R.string.update_error_hooks_timeout, canRetry = true))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Info(release))

    override fun onResume() {
        val phase = if(checks.value.installPhase == InstallPhase.Requested) {
            InstallPhase.Returned
        }else checks.value.installPhase
        refreshChecks(phase)
    }

    override fun startDownload() {
        viewModelScope.launch {
            val downloadRequest = DownloadRequest(
                url = updateRelease.download,
                title = downloadTitle(updateRelease.version),
                description = downloadDescription,
                outFileName = updateRelease.downloadFilename
            )
            this@SmartThingsUpdateViewModelImpl.downloadRequest.emit(downloadRequest)
        }
    }

    override fun startInstall() {
        viewModelScope.launch {
            val state = state.value as? State.StartInstall ?: return@launch
            val intent = context.createApkInstallIntent(state.uri)
            if(intent == null) {
                checks.value = checks.value.copy(installPhase = InstallPhase.Failed)
                return@launch
            }
            hookFinishedReceiver.value = false
            checks.value = checks.value.copy(installPhase = InstallPhase.Requested)
            navigation.navigate(intent) {
                checks.value = checks.value.copy(installPhase = InstallPhase.Failed)
            }
        }
    }

    override fun onRetryClicked() {
        refreshChecks(InstallPhase.Returned)
    }

    private fun refreshChecks(phase: InstallPhase) {
        val generation = checks.value.generation + 1
        checks.value = Checks(generation = generation, installPhase = phase)
        resumeBus.value = generation
        refreshHooks(generation)
    }

    private fun refreshHooks(generation: Long) {
        hooksBus.value = HookRequest(generation, hooksBus.value.counter + 1)
    }

    override fun onDownloadBrowserClicked() {
        viewModelScope.launch {
            navigation.navigate(Uri.parse(updateRelease.downloadAlt))
        }
    }

    override fun onComplete() {
        viewModelScope.launch {
            if(completionNavigationStarted) return@launch
            val current = checks.value
            if(current.installPhase == InstallPhase.Requested ||
                current.installPhase == InstallPhase.Failed) return@launch
            val module = (current.module as? ModuleCheck.Ready)?.module
            val ready = current.hooksReady ||
                (current.installPhase == InstallPhase.Idle && hookFinishedReceiver.value)
            if(module.versionCodeOrNull() == updateRelease.versionCode && ready) {
                completionNavigationStarted = true
                navigation.navigateBack()
            }
        }
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onCleared() {
        super.onCleared()
        val currentId = (state.value as? State.Downloading)?.downloadState?.id ?: return
        downloadRepository.cancelDownload(currentId)
    }

    private sealed class ModuleCheck {
        data object Pending: ModuleCheck()
        data object TimedOut: ModuleCheck()
        data class Ready(val module: ModuleState?): ModuleCheck()
    }

    private fun ModuleState?.versionCodeOrNull(): Int? = when(this) {
        is ModuleState.Installed -> versionCode
        is ModuleState.Outdated -> versionCode
        is ModuleState.Newer -> versionCode
        else -> null
    }

    private enum class InstallPhase { Idle, Requested, Returned, Failed }

    private data class HookRequest(val generation: Long = 0L, val counter: Long = 0L)

    private data class Checks(
        val generation: Long = 0L,
        val module: ModuleCheck = ModuleCheck.Pending,
        val hooksReady: Boolean = false,
        val installPhase: InstallPhase = InstallPhase.Idle
    )

    companion object {
        private const val PROVIDER_TIMEOUT = 30_000L
    }

}
