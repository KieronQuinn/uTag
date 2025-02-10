package com.kieronquinn.app.utag.ui.screens.safearea.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.model.ExitBuffer
import com.kieronquinn.app.utag.repositories.SafeAreaRepository
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea.WiFi
import com.kieronquinn.app.utag.utils.extensions.currentWiFiInfo
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.getMacAddressOrNull
import com.kieronquinn.app.utag.utils.extensions.getSSIDOrNull
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

abstract class SafeAreaWiFiViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onResume()
    abstract fun hasChanges(): Boolean
    abstract fun onNameChanged(name: String)
    abstract fun onSSIDChanged(ssid: String)
    abstract fun onUseMacChanged(enabled: Boolean)
    abstract fun onMACChanged(mac: String)
    abstract fun onExitBufferChanged(exitBuffer: ExitBuffer)
    abstract fun onSaveClicked()
    abstract fun onCloseClicked()
    abstract fun onDeleteClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val id: String,
            val name: String? = null,
            val ssid: String? = null,
            val useMac: Boolean = false,
            val mac: String? = null,
            val scanEnter: Boolean = false,
            val scanExit: Boolean = false,
            val exitBuffer: ExitBuffer = ExitBuffer.NONE,
            val lastExitTimestamp: Long = 0,
            val activeDeviceIds: Set<String> = emptySet()
        ): State()
    }

    enum class Event {
        INVALID_SSID,
        INVALID_MAC
    }

}

class SafeAreaWiFiViewModelImpl(
    private val safeAreaRepository: SafeAreaRepository,
    settingsNavigation: SettingsNavigation,
    moreNavigation: TagMoreNavigation,
    context: Context,
    private val isSettings: Boolean,
    private val addingDeviceId: String,
    currentId: String
): SafeAreaWiFiViewModel() {

    companion object {
        private val REGEX_MAC_ADDRESS = "%02X:%02X:%02X:%02X:%02X:%02X".toRegex()
    }

    private val navigation = if(isSettings) {
        settingsNavigation
    }else{
        moreNavigation
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val currentSafeArea = flow {
        if(currentId.isNotEmpty()) {
            val areas = safeAreaRepository.getSafeAreas().first().filterIsInstance<WiFi>()
            emit(areas.firstOrNull { it.id == currentId })
        }else emit(null)
    }.map {
        it?.let { area ->
            State.Loaded(
                id = area.id,
                name = area.name,
                ssid = area.ssid,
                useMac = area.mac != null,
                mac = area.mac,
                lastExitTimestamp = area.lastExitTimestamp,
                exitBuffer = area.exitBuffer,
                activeDeviceIds = area.activeDeviceIds
            )
        }
    }

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private var hasChanges = false

    private val currentWiFiNetwork = resumeBus.flatMapLatest {
        if(context.hasLocationPermissions()) {
            connectivityManager.currentWiFiInfo()
        }else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val firstWiFiNetwork = flow {
        //Try to get the current WiFi network, with a timeout of 500ms if not connected
        emit(withTimeoutOrNull(500L) {
            currentWiFiNetwork.firstNotNull()
        })
    }

    private val safeArea = MutableStateFlow<State.Loaded?>(null)
    private val id = currentId.takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString()

    override val state = combine(
        currentSafeArea,
        firstWiFiNetwork,
        safeArea
    ) { current, wifi, safeArea ->
        when {
            //Always use modified safeArea if it's set
            safeArea != null -> safeArea
            //Then use the current if it's set
            current != null -> current
            //If not, generate one using the current Wi-Fi network if available
            else -> {
                State.Loaded(
                    id = id,
                    ssid = wifi?.getSSIDOrNull(),
                    mac = wifi?.getMacAddressOrNull()
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val events = MutableSharedFlow<Event>()

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun hasChanges(): Boolean {
        return hasChanges
    }

    override fun onNameChanged(name: String) {
        viewModelScope.launch {
            hasChanges = true
            update {
                copy(name = name.takeIf { it.isNotBlank() })
            }
        }
    }

    override fun onSSIDChanged(ssid: String) {
        viewModelScope.launch {
            hasChanges = true
            update {
                copy(ssid = ssid.takeIf { it.isNotBlank() })
            }
        }
    }

    override fun onUseMacChanged(enabled: Boolean) {
        viewModelScope.launch {
            hasChanges = true
            update {
                copy(useMac = enabled)
            }
        }
    }

    override fun onMACChanged(mac: String) {
        viewModelScope.launch {
            hasChanges = true
            update {
                copy(mac = mac.takeIf { it.isNotBlank() })
            }
        }
    }

    override fun onExitBufferChanged(exitBuffer: ExitBuffer) {
        viewModelScope.launch {
            hasChanges = true
            update {
                copy(exitBuffer = exitBuffer)
            }
        }
    }

    override fun onSaveClicked() {
        val current = (state.value as? State.Loaded) ?: return
        viewModelScope.launch {
            val ssid = if(current.ssid != null) {
                current.ssid
            }else{
                events.emit(Event.INVALID_SSID)
                return@launch
            }
            val mac = if(current.useMac && current.mac != null) {
                if(current.mac.matches(REGEX_MAC_ADDRESS)) {
                    current.mac
                }else{
                    events.emit(Event.INVALID_MAC)
                    return@launch
                }
            }else null
            val activeDeviceIds = addingDeviceId.takeIf { it.isNotEmpty() }?.let {
                current.activeDeviceIds.plus(it)
            } ?: current.activeDeviceIds
            val area = WiFi(
                id = current.id,
                name = current.name ?: ssid,
                ssid = ssid,
                mac = mac,
                isActive = currentWiFiNetwork.value?.matches(ssid, mac) ?: false,
                exitBuffer = current.exitBuffer,
                lastExitTimestamp = current.lastExitTimestamp,
                activeDeviceIds = activeDeviceIds
            )
            safeAreaRepository.updateSafeArea(area)
            if(isSettings) {
                navigation.navigateUpTo(R.id.safeAreaListFragment)
            }else{
                navigation.navigateUpTo(R.id.tagMoreNotifyDisconnectFragment)
            }
        }
    }

    private fun WifiInfo.matches(ssid: String, mac: String?): Boolean {
        return getSSIDOrNull() == ssid && (mac == null || this.getMacAddressOrNull() == mac)
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onDeleteClicked() {
        val current = (state.value as? State.Loaded) ?: return
        viewModelScope.launch {
            safeAreaRepository.deleteWiFiSafeArea(current.id)
            navigation.navigateBack()
        }
    }

    private suspend fun update(block: State.Loaded.() -> State.Loaded) {
        val current = (state.value as? State.Loaded) ?: return
        safeArea.emit(block(current))
    }

}