package com.kieronquinn.app.utag.ui.screens.settings.location.chaser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.repositories.ChaserRepository
import com.kieronquinn.app.utag.repositories.ChaserRepository.ChaserCertificate
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsChaserViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onEnabledChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val count: Int
        ): State()
        data object Error: State()
    }

}

class SettingsChaserViewModelImpl(
    chaserRepository: ChaserRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository
): SettingsChaserViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val chaserEnabled = encryptedSettingsRepository.networkContributionsEnabled
    private val chaserCount = encryptedSettingsRepository.chaserCount

    override val state = combine(
        chaserRepository.certificate.filterNotNull(),
        chaserEnabled.asFlow(),
        chaserCount.asFlow()
    ) { certificate, enabled, count ->
        when {
            certificate !is ChaserCertificate.Certificate -> State.Error
            else -> State.Loaded(enabled, count)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            chaserEnabled.set(enabled)
        }
    }

}