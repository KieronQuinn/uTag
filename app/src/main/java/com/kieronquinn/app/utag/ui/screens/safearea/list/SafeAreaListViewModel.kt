package com.kieronquinn.app.utag.ui.screens.safearea.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.SafeAreaRepository
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea.Location
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea.WiFi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SafeAreaListViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onSafeAreaClicked(safeArea: SafeArea)
    abstract fun onAddClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val location: List<Location>,
            val wifi: List<WiFi>
        ): State()
    }

}

class SafeAreaListViewModelImpl(
    private val navigation: SettingsNavigation,
    safeAreaRepository: SafeAreaRepository
): SafeAreaListViewModel() {

    override val state = safeAreaRepository.getSafeAreas().mapLatest { areas ->
        State.Loaded(
            areas.filterIsInstance<Location>().sortedBy { it.name.lowercase() },
            areas.filterIsInstance<WiFi>().sortedBy { it.name.lowercase() }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onSafeAreaClicked(safeArea: SafeArea) {
        viewModelScope.launch {
            when(safeArea) {
                is WiFi -> navigation.navigate(SafeAreaListFragmentDirections
                    .actionSafeAreaListFragmentToSafeAreaWiFiFragment(safeArea.id, ""))
                is Location -> navigation.navigate(SafeAreaListFragmentDirections
                    .actionSafeAreaListFragmentToSafeAreaLocationFragment(safeArea.id, ""))
            }
        }
    }

    override fun onAddClicked() {
        viewModelScope.launch {
            navigation.navigate(SafeAreaListFragmentDirections
                .actionSafeAreaListFragmentToSafeAreaTypeFragment(""))
        }
    }

}