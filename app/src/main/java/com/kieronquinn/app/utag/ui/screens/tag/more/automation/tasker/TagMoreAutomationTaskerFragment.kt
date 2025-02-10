package com.kieronquinn.app.utag.ui.screens.tag.more.automation.tasker

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.TagMoreAutomationFragment.Companion.onIntentResult
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.tasker.TagMoreAutomationTaskerViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.tasker.TaskerIntent
import org.koin.androidx.viewmodel.ext.android.viewModel

class TagMoreAutomationTaskerFragment: BaseSettingsFragment(), BackAvailable, ProvidesSubtitle {

    private val args by navArgs<TagMoreAutomationTaskerFragmentArgs>()
    private val viewModel by viewModel<TagMoreAutomationTaskerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun getSubtitle(): CharSequence {
        val actionLabel = when(args.action as TagButtonAction) {
            TagButtonAction.PRESS -> R.string.tag_more_automation_press_title
            TagButtonAction.HOLD -> R.string.tag_more_automation_held_title
        }.let {
            getString(it)
        }
        return getString(R.string.subtitle_split, args.deviceLabel, actionLabel)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state) {
            is State.Loading -> setLoading(true)
            is State.Loaded -> setContent(state)
            is State.Empty -> showEmptyDialog()
        }
    }

    private fun setContent(state: State.Loaded) = setPreferences {
        state.taskNames.forEach { task ->
            preference {
                title = task.name
                summary = task.projectName
                onClick {
                    onIntentResult(
                        this@TagMoreAutomationTaskerFragment,
                        args.action,
                        TaskerIntent(task.name)
                    )
                    viewModel.close()
                }
            }
        }
    }

    private fun showEmptyDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tag_more_automation_type_tasker_dialog_title)
            setMessage(R.string.tag_more_automation_type_tasker_dialog_content)
            setPositiveButton(R.string.tag_more_automation_type_tasker_dialog_close) { _, _ ->
                viewModel.back()
            }
            setCancelable(false)
        }.show()
    }

}