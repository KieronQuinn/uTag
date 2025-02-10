package com.kieronquinn.app.utag.ui.screens.tag.more.automation.apppicker

import android.content.Context
import androidx.apppickerview.widget.AppPickerView.AppLabelInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.utils.extensions.getInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TagMoreAutomationAppPickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun close()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val packageNames: List<String>,
            val labels: List<AppLabelInfo>
        ): State()
    }

}

class TagMoreAutomationAppPickerViewModelImpl(
    private val navigation: TagMoreNavigation,
    context: Context
): TagMoreAutomationAppPickerViewModel() {

    private val packageManager = context.packageManager

    private val installedApps = flow {
        emit(packageManager.getInstalledApps())
    }.flowOn(Dispatchers.IO)

    override val state = installedApps.mapLatest {
        val packageNames = ArrayList<String>()
        val appLabelInfos = ArrayList<AppLabelInfo>()
        it.mapNotNull { app ->
            packageNames.add(app.packageName)
            val label = app.loadLabel(packageManager).toString()
            appLabelInfos.add(AppLabelInfo(app.packageName, label, app.name))
        }
        State.Loaded(packageNames, appLabelInfos)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun close() {
        viewModelScope.launch {
            navigation.navigateUpTo(R.id.tagMoreAutomationFragment)
        }
    }

}