package com.kieronquinn.app.utag.ui.screens.tag.more.automation.apppicker

import android.os.Bundle
import android.view.View
import androidx.apppickerview.widget.AppPickerView
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentMoreAutomationAppPickerBinding
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.TagMoreAutomationFragment.Companion.onIntentResult
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.apppicker.TagMoreAutomationAppPickerViewModel.State
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class TagMoreAutomationAppPickerFragment: BoundFragment<FragmentMoreAutomationAppPickerBinding>(FragmentMoreAutomationAppPickerBinding::inflate), BackAvailable, ProvidesSubtitle {

    private val args by navArgs<TagMoreAutomationAppPickerFragmentArgs>()
    private val viewModel by viewModel<TagMoreAutomationAppPickerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupInsets()
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

    private fun setupInsets() = with(binding) {
        val bottomPadding = resources.getDimensionPixelSize(R.dimen.app_picker_bottom_padding)
        appPickerList.onApplyInsets { view, insets ->
            val inset = insets.getInsets(SYSTEM_INSETS)
            view.updatePadding(bottom = inset.bottom + bottomPadding)
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) = with(binding) {
        when(state) {
            is State.Loading -> root.setDisplayedChildIfNeeded(1)
            is State.Loaded -> {
                root.setDisplayedChildIfNeeded(0)
                appPickerList.setAppPickerView(
                    AppPickerView.TYPE_LIST,
                    state.packageNames,
                    AppPickerView.ORDER_ASCENDING_IGNORE_CASE,
                    state.labels
                )
                binding.appPickerList.setOnBindListener { holder, _, packageName ->
                    holder.item.setOnClickListener {
                        onAppClicked(packageName)
                    }
                }
            }
        }
    }

    private fun onAppClicked(packageName: String) {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            ?: return
        onIntentResult(this, args.action, intent)
        viewModel.close()
    }

}