package com.kieronquinn.app.utag.ui.screens.settings.encryption.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class SettingsEncryptionConfirmPINViewModel: ViewModel() {

    abstract var pin: String

    abstract val state: StateFlow<State>

    abstract fun onPinChanged(pin: String)
    abstract fun onPinComplete(pin: String)
    abstract fun close()

    enum class State {
        IDLE, INVALID, LOADING, ERROR, COMPLETE
    }

}

class SettingsEncryptionConfirmPINViewModelImpl(
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    private val encryptionRepository: EncryptionRepository,
    private val apiRepository: ApiRepository,
    private val navigation: SettingsNavigation,
    private val previousPin: String
): SettingsEncryptionConfirmPINViewModel() {

    override var pin = ""
    override val state = MutableStateFlow(State.IDLE)

    override fun onPinChanged(pin: String) {
        this.pin = pin
        viewModelScope.launch {
            state.emit(State.IDLE)
        }
    }

    override fun onPinComplete(pin: String) {
        viewModelScope.launch {
            if(previousPin != pin) {
                state.emit(State.INVALID)
                return@launch
            }
            sendPin(pin)
        }
    }

    override fun close() {
        viewModelScope.launch {
            navigation.navigateUpTo(R.id.settingsEncryptionFragment)
        }
    }

    private suspend fun sendPin(pin: String) {
        if(state.value != State.IDLE) return
        val userId = encryptedSettingsRepository.userId.getOrNull() ?: run {
            state.emit(State.ERROR)
            return
        }
        state.emit(State.LOADING)
        val keyPair = encryptionRepository.generateEncryptionData(pin, userId) ?: run {
            state.emit(State.ERROR)
            return
        }
        val privateKey = keyPair.privateKey ?: return
        val publicKey = keyPair.publicKey ?: return
        val iv = keyPair.iv ?: return
        if(apiRepository.setPin(privateKey, publicKey, iv)) {
            state.emit(State.COMPLETE)
        }else{
            state.emit(State.ERROR)
        }
    }

}