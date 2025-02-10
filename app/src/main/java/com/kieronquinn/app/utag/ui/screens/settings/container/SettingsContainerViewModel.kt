package com.kieronquinn.app.utag.ui.screens.settings.container

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.repositories.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SettingsContainerViewModel : ViewModel() {

    abstract val showUpdateSnackbar: StateFlow<Pair<Boolean, Boolean>>
    abstract val showBottomNavigation: StateFlow<Boolean>

    abstract fun onResume()
    abstract fun setCanShowSnackbar(showSnackbar: Boolean)
    abstract fun setCanShowBottomNavigation(showBottomNavigation: Boolean)
    abstract fun onUpdateDismissed()
    abstract fun getSelectedTabId(): Int?
    abstract fun onTabSelected(id: Int)

}

class SettingsContainerViewModelImpl(
    private val navigation: SettingsNavigation,
    smartThingsRepository: SmartThingsRepository,
    updateRepository: UpdateRepository
): SettingsContainerViewModel() {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val canShowSnackbar = MutableStateFlow(false)
    private val hasDismissedSnackbar = MutableStateFlow(false)
    private var selectedTabId: Int? = null

    private val uTagUpdate = resumeBus.mapLatest {
        updateRepository.getUpdate()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val smartThingsUpdate = resumeBus.mapLatest {
        updateRepository.getModRelease()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val moduleState = resumeBus.mapLatest {
        smartThingsRepository.getModuleState()
    }

    override val showUpdateSnackbar = combine(
        canShowSnackbar,
        uTagUpdate,
        smartThingsUpdate,
        moduleState,
        hasDismissedSnackbar
    ){ canShow, uTag, smartThings, module, hasDismissed ->
        val smartThingsVersionCode = when(module) {
            is ModuleState.Installed -> module.versionCode
            is ModuleState.Outdated -> module.versionCode
            is ModuleState.Newer -> module.versionCode
            else -> null
        }
        val smartThingsUpdate = smartThings?.takeUnless {
            smartThingsVersionCode == it.versionCode || module?.wasInstalledOnPlay == true
        }
        val show = canShow && (uTag != null || smartThingsUpdate != null) && !hasDismissed
        val multiple = smartThingsUpdate != null && uTag != null
        Pair(show, multiple)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(false, false))

    override val showBottomNavigation = MutableStateFlow(false)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun setCanShowSnackbar(showSnackbar: Boolean) {
        viewModelScope.launch {
            canShowSnackbar.emit(showSnackbar)
        }
    }

    override fun setCanShowBottomNavigation(showBottomNavigation: Boolean) {
        viewModelScope.launch {
            this@SettingsContainerViewModelImpl.showBottomNavigation.emit(showBottomNavigation)
        }
    }

    override fun onUpdateDismissed() {
        viewModelScope.launch {
            hasDismissedSnackbar.emit(true)
        }
    }

    override fun getSelectedTabId(): Int? {
        return selectedTabId
    }

    override fun onTabSelected(id: Int) {
        viewModelScope.launch {
            if(selectedTabId == id) return@launch
            selectedTabId = id
            when(id) {
                R.id.nav_graph_settings -> navigation.navigate(R.id.action_global_settingsMainFragment)
                R.id.nav_graph_updates -> navigation.navigate(R.id.action_global_nav_graph_updates)
            }
        }
    }

}