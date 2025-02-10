package com.kieronquinn.app.utag.ui.screens.tag.more.passivemode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.repositories.PassiveModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TagMorePassiveModeViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val events: Flow<Event>

    abstract fun onEnabledChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data class Loaded(val deviceId: String, val enabled: Boolean): State()
    }

    enum class Event {
        DISABLED
    }

}

class TagMorePassiveModeViewModelImpl(
    private val passiveModeRepository: PassiveModeRepository,
    deviceId: String
): TagMorePassiveModeViewModel() {

    override val state = passiveModeRepository.isInPassiveModeAsFlow(deviceId).map {
        State.Loaded(deviceId, it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val events = MutableSharedFlow<Event>()

    override fun onEnabledChanged(enabled: Boolean) {
        val deviceId = (state.value as? State.Loaded)?.deviceId ?: return
        viewModelScope.launch {
            passiveModeRepository.setPassiveMode(deviceId, enabled)
            if(!enabled) {
                events.emit(Event.DISABLED)
            }
        }
    }

}