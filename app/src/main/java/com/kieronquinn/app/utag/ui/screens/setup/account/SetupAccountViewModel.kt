package com.kieronquinn.app.utag.ui.screens.setup.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.ui.activities.AuthResponseActivity
import com.kieronquinn.app.utag.utils.extensions.getAuthIntent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class SetupAccountViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onSignInClicked()
    abstract fun onOpenBrowser(url: String)

    sealed class State {
        data object SignIn: State()
        data object Loading: State()
        data class OpenBrowser(val url: String): State()
        data object Error: State()
    }

}

class SetupAccountViewModelImpl(
    private val authRepository: AuthRepository,
    private val navigation: SetupNavigation
): SetupAccountViewModel() {

    private val _state = MutableStateFlow<State>(State.SignIn)

    override val state = _state.asStateFlow()

    override fun onSignInClicked() {
        if(_state.value is State.Loading) return
        viewModelScope.launch {
            _state.emit(State.Loading)
            val result = authRepository.generateLoginUrl()?.let {
                State.OpenBrowser(it)
            } ?: State.Error
            _state.emit(result)
        }
    }

    override fun onOpenBrowser(url: String) {
        viewModelScope.launch {
            //Consume event immediately
            _state.emit(State.Loading)
            AuthResponseActivity.setEnabled(true)
            navigation.navigate(getAuthIntent(url))
            //Give intent time to open
            delay(500L)
            _state.emit(State.SignIn)
        }
    }

}