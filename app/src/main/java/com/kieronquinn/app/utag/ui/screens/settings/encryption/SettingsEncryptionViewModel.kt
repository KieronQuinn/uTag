package com.kieronquinn.app.utag.ui.screens.settings.encryption

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.model.EncryptionKey
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.PinTimeout
import com.kieronquinn.app.utag.repositories.EncryptionRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

abstract class SettingsEncryptionViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onBackPressed()
    abstract fun onSetClicked()
    abstract fun onClearClicked()
    abstract fun onPinTimeoutClicked()
    abstract fun onPinTimeoutChanged(pinTimeout: PinTimeout)
    abstract fun onBiometricsRequiredChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val encryptionEnabled: Boolean,
            val keyTimestamp: LocalDateTime?,
            val hasSavedPin: Boolean,
            val pinTimeout: PinTimeout,
            val biometricsAvailable: Boolean,
            val requireBiometrics: Boolean
        ): State()
        data object Error: State()
    }

}

class SettingsEncryptionViewModelImpl(
    private val navigation: SettingsNavigation,
    private val encryptionRepository: EncryptionRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    apiRepository: ApiRepository,
    deviceRepository: DeviceRepository,
    smartTagRepository: SmartTagRepository,
    context: Context
): SettingsEncryptionViewModel() {

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())
    private val savedPin = encryptedSettingsRepository.savedPin
    private val pinTimeout = encryptedSettingsRepository.pinTimeout
    private val requireBiometrics = encryptedSettingsRepository.biometricsRequiredToChangePin
    private val biometricManager = BiometricManager.from(context)

    private val biometricsAvailable = reloadBus.mapLatest {
        biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS
    }

    private val key = reloadBus.mapLatest {
        apiRepository.getEncryptionKey()
    }

    private val pin = combine(
        savedPin.asFlow(),
        pinTimeout.asFlow()
    ) { saved, timeout ->
        Pair(saved, timeout)
    }

    private val biometrics = combine(
        biometricsAvailable,
        requireBiometrics.asFlow()
    ) { available, required ->
        Pair(available, required)
    }

    private val deviceIds = flow {
        emit(deviceRepository.getDeviceIds())
    }

    private val deviceStates = deviceIds.flatMapLatest {
        if(it.isNullOrEmpty()) return@flatMapLatest flowOf(emptyList())
        val flows = it.map { id -> smartTagRepository.getTagState(id) }.toTypedArray()
        combine(*flows) { items ->
            items.toList()
        }
    }

    private val encryptionAvailable = deviceStates.mapLatest {
        it.any { tag ->
            (tag as? TagState.Loaded)?.getLocation()?.isEncrypted == true
        }
    }

    override val state = combine(
        encryptionAvailable,
        key,
        pin,
        biometrics
    ) { encryptionAvailable, key, pin, biometrics ->
        val savedPin = pin.first
        val pinTimeout = pin.second
        val biometricsAvailable = biometrics.first
        val biometricsRequired = biometrics.second
        val keyTimestamp = when(key) {
            is EncryptionKey.Set -> key.timeUpdated.toTimestamp()
            is EncryptionKey.Unset -> null
            else -> return@combine State.Error
        }
        State.Loaded(
            encryptionAvailable,
            keyTimestamp,
            savedPin.isNotEmpty(),
            pinTimeout,
            biometricsAvailable,
            biometricsRequired
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onSetClicked() {
        viewModelScope.launch {
            navigation.navigate(
                SettingsEncryptionFragmentDirections
                    .actionSettingsEncryptionFragmentToSettingsEncryptionSetPINFragment()
            )
        }
    }

    override fun onClearClicked() {
        viewModelScope.launch {
            savedPin.clear()
            encryptionRepository.clearPin()
        }
    }

    override fun onPinTimeoutClicked() {
        val current = when(val current = state.value) {
            is State.Loaded -> current.pinTimeout
            else -> return
        }
        viewModelScope.launch {
            navigation.navigate(SettingsEncryptionFragmentDirections
                .actionSettingsEncryptionFragmentToPinTimeoutFragment(current))
        }
    }

    override fun onPinTimeoutChanged(pinTimeout: PinTimeout) {
        viewModelScope.launch {
            this@SettingsEncryptionViewModelImpl.pinTimeout.set(pinTimeout)
        }
    }

    override fun onBiometricsRequiredChanged(enabled: Boolean) {
        viewModelScope.launch {
            requireBiometrics.set(enabled)
        }
    }

    private fun Long.toTimestamp(): LocalDateTime? {
        return try {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
        }catch (e: Exception) {
            null
        }
    }

}