package com.kieronquinn.app.utag.ui.screens.tag.ring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection.RingResult
import com.kieronquinn.app.utag.model.VolumeLevel
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.utils.extensions.next
import com.kieronquinn.app.utag.utils.extensions.previous
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class TagRingDialogViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val errorEvent: Flow<Unit>

    abstract fun onStartClicked()
    abstract fun onStopClicked()
    abstract fun onVolumeUpClicked()
    abstract fun onVolumeDownClicked()

    sealed class State {
        data object Stopped: State()
        data object Loading: State()
        data class RingingBluetooth(
            val volumeLevel: VolumeLevel,
            val sendingVolumeLevel: VolumeDirection?
        ): State()
        data object RingingNetwork: State()
    }

    enum class VolumeDirection {
        DOWN, UP
    }

}

class TagRingDialogViewModelImpl(
    deviceId: String,
    smartTagRepository: SmartTagRepository
): TagRingDialogViewModel() {

    private val commandLock = Mutex()
    private val tagConnection = smartTagRepository.getTagConnection(deviceId)
    private val ringStopEvent = tagConnection?.onRingStop()

    override val state = MutableStateFlow<State>(State.Stopped)
    override val errorEvent = MutableSharedFlow<Unit>()

    override fun onStartClicked() {
        viewModelScope.launch {
            startRinging()
        }
    }

    override fun onStopClicked() {
        viewModelScope.launch {
            stopRinging()
        }
    }

    override fun onVolumeDownClicked() {
        viewModelScope.launch {
            sendVolume(VolumeDirection.DOWN)
        }
    }

    override fun onVolumeUpClicked() {
        viewModelScope.launch {
            sendVolume(VolumeDirection.UP)
        }
    }

    private suspend fun sendVolume(direction: VolumeDirection) = commandLock.withLock {
        val oldState = state.value as? State.RingingBluetooth ?: return@withLock
        if(oldState.sendingVolumeLevel != null) return@withLock
        val newVolume = when(direction) {
            VolumeDirection.UP -> oldState.volumeLevel.next()
            VolumeDirection.DOWN -> oldState.volumeLevel.previous()
        } ?: return@withLock
        viewModelScope.launch {
            state.emit(oldState.copy(sendingVolumeLevel = direction))
            if(tagConnection?.setRingVolume(newVolume) == true) {
                state.emit(oldState.copy(volumeLevel = newVolume))
            }else{
                state.emit(oldState)
                errorEvent.emit(Unit)
            }
        }
    }

    private suspend fun startRinging() = commandLock.withLock {
        if(state.value !is State.Stopped) return@withLock
        state.emit(State.Loading)
        val nextState = when(val result = tagConnection?.startRinging()) {
            is RingResult.SuccessBluetooth -> {
                State.RingingBluetooth(result.volume, null)
            }
            is RingResult.SuccessNetwork -> State.RingingNetwork
            else -> {
                errorEvent.emit(Unit)
                State.Stopped
            }
        }
        state.emit(nextState)
    }

    private suspend fun startRingingAfterConnectingLate() = commandLock.withLock {
        if(state.value !is State.RingingNetwork) return@withLock
        state.emit(State.Loading)
        //Stop network ringing so it doesn't incorrectly sync later
        val stopNetworkResult = tagConnection?.stopRingingNetwork() ?: false
        //Now start ringing locally
        val result = if(stopNetworkResult) {
            tagConnection?.startRinging()
        }else null
        val nextState = when(result) {
            is RingResult.SuccessBluetooth -> {
                State.RingingBluetooth(result.volume, null)
            }
            is RingResult.SuccessNetwork -> State.RingingNetwork
            else -> {
                errorEvent.emit(Unit)
                State.Stopped
            }
        }
        state.emit(nextState)
    }

    private suspend fun stopRinging() = commandLock.withLock {
        val result = when(state.value) {
            is State.RingingBluetooth -> tagConnection?.stopRingingBluetooth()
            is State.RingingNetwork -> tagConnection?.stopRingingNetwork()
            else -> return@withLock
        }
        if(result == true) {
            state.emit(State.Stopped)
        }else{
            errorEvent.emit(Unit)
        }
    }

    private fun setupRingStop() = viewModelScope.launch {
        ringStopEvent?.collect {
            state.emit(State.Stopped)
        }
    }

    /**
     *  If the Tag starts out disconnected but connects while the dialog is open, and the user is
     *  on the remote ring screen, start ringing immediately
     */
    private fun setupRingStart() = viewModelScope.launch {
        tagConnection?.getIsConnected()?.collect {
            if(it && state.value is State.RingingNetwork) {
                //We've connected after the network ring has already been set, start ringing
                startRingingAfterConnectingLate()
            }
        }
    }

    init {
        setupRingStop()
        setupRingStart()
        //Start ringing when the dialog launches for the first time
        viewModelScope.launch {
            startRinging()
        }
    }

}