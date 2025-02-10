package com.kieronquinn.app.utag.ui.screens.unknowntag.container

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

abstract class UnknownTagContainerViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onBiometricSuccess()

    sealed class State(val destination: Int?) {
        data object Loading: State(R.id.action_global_decisionFragment22)
        data object Loaded: State(R.id.action_nav_graph_include_unknown_tag_self)
        data object BiometricPrompt: State(null)
    }

}

class UnknownTagContainerViewModelImpl(
    private val authRepository: AuthRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository
): UnknownTagContainerViewModel() {

    private val ensureKeyAndIv = flow {
        emit(encryptedSettingsRepository.ensureKeyAndIV())
    }

    override val state = combine(
        encryptedSettingsRepository.biometricPromptEnabled.asFlow(),
        authRepository.biometricPassed,
        ensureKeyAndIv
    ) { biometricEnabled, biometricPassed, _ ->
        when {
            biometricEnabled && !biometricPassed -> State.BiometricPrompt
            else -> State.Loaded
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onBiometricSuccess() {
        viewModelScope.launch {
            authRepository.onBiometricSuccess()
        }
    }

}