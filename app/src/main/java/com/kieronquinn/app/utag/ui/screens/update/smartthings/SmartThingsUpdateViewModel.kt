package com.kieronquinn.app.utag.ui.screens.update.smartthings

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.networking.model.github.ModRelease
import com.kieronquinn.app.utag.repositories.DownloadRepository
import com.kieronquinn.app.utag.repositories.DownloadRepository.DownloadRequest
import com.kieronquinn.app.utag.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.repositories.UpdateRepository.Companion.CONTENT_TYPE_APK
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.xposed.Xposed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SmartThingsUpdateViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onDownloadBrowserClicked()
    abstract fun startDownload()
    abstract fun startInstall()
    abstract fun onCloseClicked()

    sealed class State {
        data class Info(val release: ModRelease): State()
        data class Downloading(val downloadState: DownloadState.Progress): State()
        data class StartInstall(val uri: Uri): State()
        data object SettingUpHooks: State()
        data object Complete: State()
        data object Failed: State()
    }

}

class SmartThingsUpdateViewModelImpl(
    private val navigation: SettingsNavigation,
    private val downloadRepository: DownloadRepository,
    smartThingsRepository: SmartThingsRepository,
    context: Context,
    release: ModRelease
) : SmartThingsUpdateViewModel() {

    private val updateRelease = release
    private val downloadRequest = MutableSharedFlow<DownloadRequest?>()
    private val downloadDescription = context.getString(R.string.download_manager_description_mod)
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val moduleState = resumeBus.mapLatest {
        smartThingsRepository.getModuleState()
    }

    private val downloadTitle = { version: String ->
        context.getString(R.string.update_notification_mod, version)
    }

    private val areHooksSetup = resumeBus.mapLatest {
        smartThingsRepository.areHooksSetup()
    }

    private val hookFinishedReceiver = context.broadcastReceiverAsFlow(
        IntentFilter(Xposed.ACTION_HOOKING_FINISHED)
    ).map {
        true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val hooksReady = combine(
        hookFinishedReceiver,
        areHooksSetup
    ) { finished, setup ->
        finished || setup
    }

    private val updateDownload = downloadRequest.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(null)
        downloadRepository.download(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val state = combine(
        moduleState,
        updateDownload.filterNotNull(),
        hooksReady
    ) { module, download, hooksReady ->
        val moduleVersionCode = when(module) {
            is ModuleState.Installed -> module.versionCode
            is ModuleState.Outdated -> module.versionCode
            is ModuleState.Newer -> module.versionCode
            else -> null
        }
        when {
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
                State.StartInstall(download.file)
            }
            else -> {
                State.Failed
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Info(release))

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
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
            val intent = Intent(Intent.ACTION_VIEW).apply {
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                setDataAndType(state.uri, CONTENT_TYPE_APK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            navigation.navigate(intent)
        }
    }

    override fun onDownloadBrowserClicked() {
        viewModelScope.launch {
            navigation.navigate(Uri.parse(updateRelease.downloadAlt))
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

}