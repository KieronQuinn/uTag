package com.kieronquinn.app.utag.ui.screens.findmydevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository
import com.kieronquinn.app.utag.ui.activities.FindMyDeviceActivity.FindMyDeviceActivityConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

abstract class FindMyDeviceViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    sealed class State {
        data object Loading: State()
        data class Delay(val progress: Int): State()
        data class Ringing(val progress: Int): State()
        data object Close: State()
    }

}

class FindMyDeviceViewModelImpl(
    private val config: FindMyDeviceActivityConfig,
    private val findMyDeviceRepository: FindMyDeviceRepository
): FindMyDeviceViewModel() {

    override val state = MutableStateFlow<State>(State.Loading)

    private suspend fun tick() {
        val now = System.currentTimeMillis()
        val newState = when {
            now < config.startTime - config.delayTime -> State.Loading
            now < config.startTime -> {
                val progress = 100 - (((config.startTime - now) / config.delayTime.toFloat()) * 100)
                State.Delay(progress.toInt())
            }
            now < config.endTime -> {
                val startTime = config.endTime - config.timeout
                val progress = ((now - startTime) / config.timeout.toFloat()) * 100
                State.Ringing(progress.toInt())
            }
            else -> State.Close
        }
        state.emit(newState)
    }

    private fun loop() = viewModelScope.launch {
        while(isActive) {
            tick()
            delay(1000L)
        }
    }

    override fun onCleared() {
        super.onCleared()
        findMyDeviceRepository.sendStopIntent()
    }

    init {
        loop()
    }

}