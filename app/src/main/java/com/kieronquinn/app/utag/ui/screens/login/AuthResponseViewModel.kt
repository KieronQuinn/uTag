package com.kieronquinn.app.utag.ui.screens.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationId
import com.kieronquinn.app.utag.utils.SignInUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class AuthResponseViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun handleIntent(intent: Intent)
    abstract fun onSuccess()

    sealed class State {
        data object Idle: State()
        data object Loading: State()
        data object Complete: State()
        data object Cancelled: State()
        data object Error: State()
    }

}

class AuthResponseViewModelImpl(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
): AuthResponseViewModel() {

    private val _state = MutableStateFlow<State>(State.Idle)

    override val state = _state.asStateFlow()

    override fun handleIntent(intent: Intent) {
        if(state.value !is State.Idle) return
        viewModelScope.launch {
            _state.emit(State.Loading)
            val data = intent.data
            val code = data?.getQueryParameter("code")
            val apiServerUrl = data?.getQueryParameter("api_server_url")
            val authServerUrl = data?.getQueryParameter("auth_server_url")
            val state = data?.getQueryParameter("state")
            val retValue = data?.getQueryParameter("retValue")
            val result = data?.getQueryParameter("result")
            when {
                code != null && apiServerUrl != null && authServerUrl != null
                        && state != null && retValue != null -> {
                    decryptState(authServerUrl, code, state, retValue)
                }
                result == "true" -> {
                    handleLogout()
                }
                else -> {
                    _state.emit(State.Cancelled)
                }
            }
        }
    }

    override fun onSuccess() {
        notificationRepository.cancelNotification(NotificationId.LOGGED_OUT)
    }

    private suspend fun decryptState(
        authServerUrl: String,
        code: String,
        state: String,
        retValue: String
    ) {
        val decryptedState = SignInUtils.decrypt(state, authRepository.getState())
        if(decryptedState != null) {
            handleDecryptedState(authServerUrl, code, retValue, decryptedState)
        }else{
            _state.emit(State.Error)
        }
    }

    private suspend fun handleDecryptedState(
        authServerUrl: String,
        code: String,
        retValue: String,
        decryptedState: String
    ) {
        val decryptedAuthServerUrl = SignInUtils.decrypt(authServerUrl, decryptedState)
        val decryptedCode = SignInUtils.decrypt(code, decryptedState)
        val decryptedRetValue = SignInUtils.decrypt(retValue, decryptedState)
        if(decryptedAuthServerUrl != null && decryptedCode != null && decryptedRetValue != null) {
            handleAuth(decryptedAuthServerUrl, decryptedCode, decryptedRetValue)
        }else{
            _state.emit(State.Error)
        }
    }

    private suspend fun handleAuth(
        decryptedAuthServerUrl: String,
        decryptedCode: String,
        username: String
    ) {
        if(authRepository.handleAuthResponse(decryptedAuthServerUrl, decryptedCode, username)) {
            notificationRepository.cancelNotification(NotificationId.LOGGED_OUT)
            _state.emit(State.Complete)
        }else{
            _state.emit(State.Error)
        }
    }

    private suspend fun handleLogout() {
        authRepository.clearCredentials()
        _state.emit(State.Complete)
    }

}