package com.kieronquinn.app.utag.ui.screens.tag.more.automation.shortcutpicker

import android.content.ComponentName
import android.content.Context
import androidx.apppickerview.widget.AppPickerView.AppLabelInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.utils.extensions.getInstalledShortcuts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TagMoreAutomationShortcutPickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun close()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val components: List<ComponentName>,
            val labels: List<AppLabelInfo>
        ): State()
    }

}

class TagMoreAutomationShortcutPickerViewModelImpl(
    private val navigation: TagMoreNavigation,
    context: Context
): TagMoreAutomationShortcutPickerViewModel() {

    private val packageManager = context.packageManager

    private val installedShortcuts = flow {
        emit(packageManager.getInstalledShortcuts())
    }.flowOn(Dispatchers.IO)

    override val state = installedShortcuts.mapLatest {
        val components = ArrayList<ComponentName>()
        val appLabelInfos = ArrayList<AppLabelInfo>()
        it.mapNotNull { app ->
            components.add(ComponentName(app.packageName, app.name))
            val label = app.loadLabel(packageManager).toString()
            appLabelInfos.add(AppLabelInfo(app.packageName, label, app.name))
        }
        State.Loaded(components, appLabelInfos)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun close() {
        viewModelScope.launch {
            navigation.navigateUpTo(R.id.tagMoreAutomationFragment)
        }
    }

}