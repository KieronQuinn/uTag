package com.kieronquinn.app.utag.ui.screens.tag.more.automation.type

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import com.kieronquinn.app.utag.service.UTagLaunchIntentService.Companion.ACTION_CAMERA
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.TagMoreAutomationFragment.Companion.onIntentResult
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.tasker.TaskerIntent
import org.koin.androidx.viewmodel.ext.android.viewModel

class TagMoreAutomationTypeFragment: BaseSettingsFragment(), BackAvailable, ProvidesTitle, ProvidesSubtitle {

    private val args by navArgs<TagMoreAutomationTypeFragmentArgs>()
    private val viewModel by viewModel<TagMoreAutomationTypeFragmentViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPreferences {
            preference {
                title = getString(R.string.tag_more_automation_type_app_title)
                summary = getString(R.string.tag_more_automation_type_app_content)
                onClick { viewModel.onAppClicked(args.deviceLabel, args.action) }
            }
            preference {
                title = getString(R.string.tag_more_automation_type_shortcut_title)
                summary = getString(R.string.tag_more_automation_type_shortcut_content)
                onClick { viewModel.onShortcutClicked(args.deviceLabel, args.action) }
            }
            preference {
                title = getString(R.string.tag_more_automation_type_camera_title)
                summary = getString(R.string.tag_more_automation_type_camera_content)
                onClick { onCameraClicked() }
            }
            if(TaskerIntent.taskerInstalled(requireContext())) {
                preference {
                    title = getString(R.string.tag_more_automation_type_tasker_title)
                    summary = getString(R.string.tag_more_automation_type_tasker_content)
                    onClick { viewModel.onTaskerClicked(args.deviceLabel, args.action) }
                }
            }
        }
    }

    private fun onCameraClicked() {
        onIntentResult(this, args.action, Intent(ACTION_CAMERA))
        viewModel.back()
    }

    override fun getTitle(): CharSequence {
        return when(args.action as TagButtonAction) {
            TagButtonAction.PRESS -> R.string.tag_more_automation_press_title
            TagButtonAction.HOLD -> R.string.tag_more_automation_held_title
        }.let {
            getString(it)
        }
    }

    override fun getSubtitle(): CharSequence {
        return args.deviceLabel
    }

}