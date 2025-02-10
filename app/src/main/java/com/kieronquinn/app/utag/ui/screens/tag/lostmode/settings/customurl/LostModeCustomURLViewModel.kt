package com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.customurl

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.Application.Companion.LOSTMESSAGE_URL
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.model.DeviceInfo
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse.MessageType
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse.PredefinedMessage.DREAM_SACP_BODY_THIS_SMARTTAG_HAS_BEEN_LOST
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.UserRepository
import com.kieronquinn.app.utag.utils.extensions.isLoadingDelayed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class LostModeCustomURLViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onResume()
    abstract fun close()
    abstract fun onCustomUrlChanged(url: String, override: Boolean = false): SetInitialError?

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val currentUrl: String,
            val customUrl: String?,
            val savedUrl: String?,
            val saveableData: String?,
            val saveableUrl: String?,
            val email: String,
            val isLoading: Boolean
        ): State()
        data object Error: State()
    }

    enum class SetInitialError {
        ERROR,
        WARN,
        INVALID_URL
    }

    enum class Event {
        ERROR
    }

}

class LostModeCustomURLViewModelImpl(
    smartTagRepository: SmartTagRepository,
    private val apiRepository: ApiRepository,
    private val navigation: TagMoreNavigation,
    private val deviceId: String,
    deviceRepository: DeviceRepository,
    userRepository: UserRepository,
    context: Context
): LostModeCustomURLViewModel() {

    companion object {
        private const val SPACER = " "

        /**
         *  Converts string from resources into "email" by replacing all spaces with padding char,
         *  and appending an @ to the end, padded to be off the screen as much as possible
         */
        private fun String.convertToEmail(): String {
            return replace(" ", SPACER)
                .padEnd(64, SPACER.toCharArray().first()).let {
                    "$it@$SPACER"
                }
        }
    }

    private val isSending = MutableStateFlow(false)
    private val isLoading = isLoadingDelayed()
    private val refreshBus = MutableStateFlow(System.currentTimeMillis())
    private val tagConnection = smartTagRepository.getTagConnection(deviceId)
    private val isConnected = tagConnection?.getIsConnected() ?: flowOf(false)

    private val warningEmail = context.getString(R.string.lost_mode_custom_url_warning)
        .convertToEmail()

    private val deviceInfo = flow {
        emit(deviceRepository.getDeviceInfo(deviceId))
    }

    private val savedUrl = combine(
        deviceInfo,
        refreshBus
    ) { info, _ ->
        apiRepository.getLostMode(deviceId)?.message?.extractLostMessageUrl(info)
    }

    private val currentUrl = combine(
        isConnected,
        refreshBus
    ) { _, _ ->
        tagConnection?.getLostModeUrl()
    }.onEach {
        isLoading.emit(false)
    }

    private val userInfo = flow {
        emit(userRepository.getUserInfo())
    }

    private val loading = combine(
        isLoading,
        isSending
    ) { loading, sending ->
        loading == true || sending
    }

    override val state = combine(
        currentUrl,
        savedUrl,
        userInfo,
        loading
    ) { current, saved, userInfo, loading ->
        if(current != null && userInfo != null) {
            val custom = current.takeIf { !it.isLostMessageUrl() }
            val saveable = current.extractSaveable() ?: saved?.extractSaveable()
            val saveableUrl = current.takeIf { it.isLostMessageUrl() } ?: saved
            State.Loaded(current, custom, saved, saveable, saveableUrl, userInfo.email, loading)
        }else State.Error
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val events = MutableSharedFlow<Event>()

    override fun onResume() {
        viewModelScope.launch {
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    override fun close() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onCustomUrlChanged(url: String, override: Boolean): SetInitialError? {
        val state = state.value as? State.Loaded ?: return null
        if(state.currentUrl == url) return null
        if(Uri.parse(url) == null) return SetInitialError.INVALID_URL
        //If the URL is a lost message URL, check if it's for this Tag if we can
        if(url.isLostMessageUrl() && state.savedUrl != null && url != state.savedUrl) {
            return SetInitialError.ERROR
        }
        //If the URL is a lost message URL and we can't check, prompt the user
        if(url.isLostMessageUrl() && state.savedUrl == null && !override) {
            return SetInitialError.WARN
        }
        setCustomUrl(url, state.saveableData, state.email)
        return null
    }

    private fun setCustomUrl(
        url: String, saveableData: String?, email: String
    ) = viewModelScope.launch {
        isSending.emit(true)
        when {
            //Reset the lost mode data if this is a lost message URL
            url.isLostMessageUrl() -> {
                if(!apiRepository.setLostMode(
                        deviceId, LostModeRequestResponse(
                            enabled = true,
                            messageType = MessageType.PREDEFINED,
                            message = DREAM_SACP_BODY_THIS_SMARTTAG_HAS_BEEN_LOST.name,
                            phoneNumber = null,
                            email = email,
                        )
                    )) {
                    events.emit(Event.ERROR)
                    isSending.emit(false)
                    return@launch
                }
            }
            //Save the current URL to the server if we can, and set the warning
            saveableData != null -> {
                if(!apiRepository.setLostMode(
                        deviceId, LostModeRequestResponse(
                            enabled = true,
                            messageType = MessageType.CUSTOM,
                            message = saveableData,
                            phoneNumber = null,
                            email = warningEmail,
                        )
                    )) {
                    events.emit(Event.ERROR)
                    isSending.emit(false)
                    return@launch
                }
            }
        }
        //Update the Tag with the new URL
        if(tagConnection?.setLostModeUrl(url) != true) {
            events.emit(Event.ERROR)
        }else{
            isLoading.emit(true)
        }
        isSending.emit(false)
        refreshBus.emit(System.currentTimeMillis())
    }

    private fun String.isLostMessageUrl(): Boolean {
        return startsWith(LOSTMESSAGE_URL)
    }

    private fun String.extractSaveable(): String? {
        if(!isLostMessageUrl()) return null
        val uri = Uri.parse(this) ?: return null
        val r = uri.getQueryParameter("r")
        val d = uri.getQueryParameter("d")
        return "$r$SPACER$d"
    }

    private fun String.extractLostMessageUrl(deviceInfo: DeviceInfo?): String? {
        if(deviceInfo == null || !contains(SPACER)) return null
        val split = split(SPACER)
        val r = split[0]
        val d = split[1]
        return Uri.parse(LOSTMESSAGE_URL).buildUpon()
            .appendQueryParameter("c", "t")
            .appendQueryParameter("mn", deviceInfo.mnId)
            .appendQueryParameter("s", deviceInfo.setupId)
            .appendQueryParameter("mo", deviceInfo.modelName)
            .appendQueryParameter("r", r)
            .appendQueryParameter("d", d)
            .build().toString()
    }

}