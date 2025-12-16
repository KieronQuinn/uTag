package com.kieronquinn.app.utag.ui.screens.setup.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.ui.activities.AuthResponseActivity
import com.kieronquinn.app.utag.utils.extensions.getAuthIntent
import com.kieronquinn.app.utag.utils.extensions.isIntentChrome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class SetupAccountViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onSignInClicked()
    abstract fun onOpenBrowser(
        url: String,
        forceChrome: Boolean = false,
        ignoreChrome: Boolean = false
    )

    sealed class State {
        data object SignIn: State()
        data object Loading: State()
        data class OpenBrowser(val url: String): State()
        data object Error: State()
    }

    enum class Event {
        THIRD_PARTY_BROWSER_PROMPT
    }

}

class SetupAccountViewModelImpl(
    private val authRepository: AuthRepository,
    private val navigation: SetupNavigation,
    context: Context
): SetupAccountViewModel() {

    private val _state = MutableStateFlow<State>(State.SignIn)
    private val packageManager = context.packageManager

    override val state = _state.asStateFlow()
    override val events = MutableSharedFlow<Event>()

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

    override fun onOpenBrowser(url: String, forceChrome: Boolean, ignoreChrome: Boolean) {
        viewModelScope.launch {
            val authIntent = getAuthIntent(url, forceChrome)
            if(!forceChrome && !ignoreChrome && !authIntent.isIntentChrome(packageManager)) {
                // Due to an issue with Google Sign In, warn the user if not using Chrome
                events.emit(Event.THIRD_PARTY_BROWSER_PROMPT)
                return@launch
            }
            //Consume event immediately
            _state.emit(State.Loading)
            AuthResponseActivity.setEnabled(true)
            navigation.navigate(authIntent)
            //Give intent time to open
            delay(500L)
            _state.emit(State.SignIn)
        }
    }

}