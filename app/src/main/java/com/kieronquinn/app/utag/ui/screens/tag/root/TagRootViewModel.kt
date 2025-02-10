package com.kieronquinn.app.utag.ui.screens.tag.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TagRootViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onBiometricSuccess()

    sealed class State(val destination: Int?) {
        data object Loading: State(R.id.action_global_decisionFragment2)
        data object Setup: State(R.id.action_global_tagLandingFragment)
        data object Settings: State(R.id.action_global_tagContainerFragment)
        data object BiometricPrompt: State(null)
    }

}

class TagRootViewModelImpl(
    private val authRepository: AuthRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository
): TagRootViewModel() {

    private val isSignedIn = authRepository.isLoggedIn()

    private val ensureKeyAndIv = flow {
        emit(encryptedSettingsRepository.ensureKeyAndIV())
    }

    override val state = combine(
        isSignedIn,
        encryptedSettingsRepository.biometricPromptEnabled.asFlow(),
        authRepository.biometricPassed,
        ensureKeyAndIv
    ) { isSignedIn, biometricEnabled, biometricPassed, _ ->
        when {
            biometricEnabled && !biometricPassed -> State.BiometricPrompt
            isSignedIn == true -> State.Settings
            isSignedIn == false -> State.Setup
            else -> State.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onBiometricSuccess() {
        viewModelScope.launch {
            authRepository.onBiometricSuccess()
        }
    }

}