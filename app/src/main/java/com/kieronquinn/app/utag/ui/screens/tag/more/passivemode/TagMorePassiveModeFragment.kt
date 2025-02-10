package com.kieronquinn.app.utag.ui.screens.tag.more.passivemode

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.more.passivemode.TagMorePassiveModeViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.more.passivemode.TagMorePassiveModeViewModel.State
import com.kieronquinn.app.utag.utils.extensions.appendBullet
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.switchBarPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TagMorePassiveModeFragment: BaseSettingsFragment(), BackAvailable, ProvidesSubtitle {

    private val args by navArgs<TagMorePassiveModeFragmentArgs>()

    private val viewModel by viewModel<TagMorePassiveModeViewModel> {
        parametersOf(args.deviceId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupEvents()
    }

    override fun getSubtitle(): CharSequence {
        return args.deviceLabel
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
        }
    }

    private fun setupEvents() = whenResumed {
        viewModel.events.collect {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.DISABLED -> showDisabledDialog()
        }
    }

    private fun setContent(state: State.Loaded) = setPreferences {
        switchBarPreference {
            title = getString(R.string.passive_mode_enabled_switch)
            isChecked = state.enabled
            onChange<Boolean> {
                viewModel.onEnabledChanged(it)
            }
        }
        preferenceCategory("passive_info") {
            tipsCardPreference {
                title = getString(R.string.passive_mode_info_title)
                summary = getText(R.string.passive_mode_info_content)
            }
            tipsCardPreference {
                title = getString(R.string.passive_mode_lost_functionality_title)
                summary = createInfo(
                    R.array.passive_mode_lost_functionality_items,
                    R.string.passive_mode_lost_functionality_footer
                )
            }
        }
    }

    private fun createInfo(
        @ArrayRes items: Int,
        @StringRes footer: Int
    ) = SpannableStringBuilder().apply {
        val footerItems = resources.getTextArray(items)
        footerItems.forEachIndexed { i, item ->
            appendBullet()
            append(item)
            if(i < footerItems.size - 1) {
                appendLine()
            }
        }
        appendLine()
        appendLine()
        append(getText(footer))
    }

    private fun showDisabledDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setMessage(R.string.passive_mode_disabled_dialog_content)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

}