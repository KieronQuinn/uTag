package com.kieronquinn.app.utag.ui.screens.update.utag

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.model.Release
import com.kieronquinn.app.utag.repositories.DownloadRepository
import com.kieronquinn.app.utag.repositories.DownloadRepository.DownloadRequest
import com.kieronquinn.app.utag.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.utag.repositories.UpdateRepository.Companion.CONTENT_TYPE_APK
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class UTagUpdateViewModel : ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onDownloadBrowserClicked()
    abstract fun startDownload()
    abstract fun startInstall()
    abstract fun onCloseClicked()

    sealed class State {
        data class Info(val release: Release): State()
        data class Downloading(val downloadState: DownloadState.Progress): State()
        data class StartInstall(val uri: Uri): State()
        data object Failed: State()
    }

}

class UTagUpdateViewModelImpl(
    private val navigation: SettingsNavigation,
    private val downloadRepository: DownloadRepository,
    context: Context,
    release: Release
) : UTagUpdateViewModel() {

    private val updateRelease = release
    private val downloadRequest = MutableSharedFlow<DownloadRequest?>()
    private val downloadDescription = context.getString(R.string.download_manager_description)

    private val downloadTitle = { version: String ->
        context.getString(R.string.update_notification, version)
    }

    private val updateDownload = downloadRequest.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(null)
        downloadRepository.download(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val state = updateDownload.filterNotNull().mapLatest { download ->
        when(download) {
            is DownloadState.Progress -> {
                State.Downloading(download)
            }
            is DownloadState.DownloadComplete -> {
                State.StartInstall(download.file)
            }
            is DownloadState.Failed -> {
                State.Failed
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Info(release))

    override fun startDownload() {
        viewModelScope.launch {
            val downloadRequest = DownloadRequest(
                url = updateRelease.downloadUrl,
                title = downloadTitle(updateRelease.versionName),
                description = downloadDescription,
                outFileName = updateRelease.fileName
            )
            this@UTagUpdateViewModelImpl.downloadRequest.emit(downloadRequest)
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
            navigation.navigate(Uri.parse(updateRelease.gitHubUrl))
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