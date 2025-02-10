package com.kieronquinn.app.utag.ui.screens.settings.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsSecurityViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onEncryptionClicked()
    abstract fun onBiometricsEnabled()
    abstract fun onBiometricsDisabled()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val biometricsAvailable: Boolean,
            val biometricsEnabled: Boolean
        ): State()
    }

}

class SettingsSecurityViewModelImpl(
    private val navigation: SettingsNavigation,
    private val authRepository: AuthRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    context: Context
): SettingsSecurityViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val biometricManager = BiometricManager.from(context)
    private val biometricsEnabled = encryptedSettingsRepository.biometricPromptEnabled

    private val biometricsAvailable = resumeBus.mapLatest {
        biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS
    }

    override val state = combine(
        biometricsAvailable,
        biometricsEnabled.asFlow()
    ) { available, enabled ->
        State.Loaded(available, enabled)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onEncryptionClicked() {
        viewModelScope.launch {
            navigation.navigate(
                SettingsSecurityFragmentDirections
                    .actionSettingsSecurityFragmentToSettingsEncryptionFragment())
        }
    }

    override fun onBiometricsEnabled() {
        viewModelScope.launch {
            //Pre-pass biometrics to not show the prompt right now
            authRepository.onBiometricSuccess()
            biometricsEnabled.set(true)
        }
    }

    override fun onBiometricsDisabled() {
        viewModelScope.launch {
            biometricsEnabled.set(false)
        }
    }

}