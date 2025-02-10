package com.kieronquinn.app.utag.ui.screens.tag.more.automation.tasker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.tasker.TagMoreAutomationTaskerViewModel.State.Loaded.Task
import com.kieronquinn.app.utag.utils.extensions.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class TagMoreAutomationTaskerViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun back()
    abstract fun close()

    sealed class State {
        data object Loading: State()
        data class Loaded(val taskNames: List<Task>): State() {
            data class Task(
                val name: String,
                val projectName: String?
            )
        }
        data object Empty: State()
    }

}

class TagMoreAutomationTaskerViewModelImpl(
    private val navigation: TagMoreNavigation,
    context: Context
): TagMoreAutomationTaskerViewModel() {

    companion object {
        private val URI_TASKS = Uri.parse("content://net.dinglisch.android.tasker/tasks")
    }

    private val contentResolver = context.contentResolver
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val taskNames = resumeBus.mapLatest {
        val c = contentResolver.query(URI_TASKS, null, null, null)
            ?: return@mapLatest emptyList()
        val nameColumn = c.getColumnIndex("name")
        val projectColumn = c.getColumnIndex("project_name")
        c.map {
            val name = it.getString(nameColumn)
            val project = it.getString(projectColumn)
            Task(name, project)
        }.sortedBy {
            it.name.lowercase()
        }
    }.flowOn(Dispatchers.IO)

    override val state = taskNames.mapLatest {
        if(it.isNotEmpty()) {
            State.Loaded(it)
        }else{
            State.Empty
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun back() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun close() {
        viewModelScope.launch {
            navigation.navigateUpTo(R.id.tagMoreAutomationFragment)
        }
    }

}