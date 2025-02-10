package com.kieronquinn.app.utag.ui.screens.settings.contentcreator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

abstract class SettingsContentCreatorViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onEnabledChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data class Loaded(val enabled: Boolean): State()
    }

}

class SettingsContentCreatorViewModelImpl(
    private val historyWidgetRepository: HistoryWidgetRepository,
    settingsRepository: SettingsRepository
): SettingsContentCreatorViewModel() {

    private val enabled = settingsRepository.contentCreatorModeEnabled

    override val state = enabled.asFlow().mapLatest {
        State.Loaded(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            this@SettingsContentCreatorViewModelImpl.enabled.set(enabled)
            val context by inject<Context>(Context::class.java)
            //Update Tags and widgets
            SmartTagRepository.refreshTagStates(context)
            historyWidgetRepository.updateWidgets()
        }
    }

}