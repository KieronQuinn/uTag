package com.kieronquinn.app.utag.ui.screens.tag.more.automation

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.ButtonVolumeLevel
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.TagMoreAutomationViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.TagMoreAutomationViewModel.State
import com.kieronquinn.app.utag.xposed.extensions.getParcelableCompat
import com.kieronquinn.app.utag.xposed.extensions.getSerializableCompat
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.dropDownPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import com.kieronquinn.app.utag.utils.preferences.switchPreferenceScreen
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TagMoreAutomationFragment: BaseSettingsFragment(), BackAvailable, ProvidesSubtitle {

    companion object {
        private const val KEY_INTENT = "intent"
        private const val KEY_ACTION = "action"

        private fun Fragment.setupIntentListener(callback: (action: TagButtonAction, intent: Intent) -> Unit) {
            setFragmentResultListener(KEY_INTENT) { requestKey, bundle ->
                if(requestKey != KEY_INTENT) return@setFragmentResultListener
                val intent = bundle.getParcelableCompat(KEY_INTENT, Intent::class.java)
                    ?: return@setFragmentResultListener
                val action = bundle.getSerializableCompat(KEY_ACTION, TagButtonAction::class.java)
                    ?: return@setFragmentResultListener
                callback.invoke(action, intent)
            }
        }

        fun onIntentResult(fragment: Fragment, action: TagButtonAction, intent: Intent) {
            fragment.setFragmentResult(
                KEY_INTENT, bundleOf(KEY_INTENT to intent, KEY_ACTION to action)
            )
        }
    }

    private val args by navArgs<TagMoreAutomationFragmentArgs>()
    private var isShowingWarning = false

    private val viewModel by viewModel<TagMoreAutomationViewModel> {
        parametersOf(args.deviceId, args.deviceLabel, args.isSharedTag)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupEvents()
        setupResult()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun getSubtitle(): CharSequence {
        return args.deviceLabel
    }

    private fun setupResult() {
        setupIntentListener { action, intent ->
            when(action) {
                TagButtonAction.PRESS -> viewModel.onPressChanged(intent)
                TagButtonAction.HOLD -> viewModel.onHoldChanged(intent)
            }
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

    private fun handleState(state: State) {
        when(state) {
            is State.Loading -> setLoading(true)
            is State.Loaded -> {
                handleContent(state)
                if(state.showSharedWarningDialog) {
                    showWarningDialog()
                }
            }
            is State.Error -> showErrorDialog()
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences {
        if (state.hasOverlayPermission && state.pressTitle != null && !state.config.pressRemoteEnabled) {
            switchPreferenceScreen {
                title = getString(R.string.tag_more_automation_press_title)
                summary =
                    getString(R.string.tag_more_automation_press_content, state.pressTitle)
                isChecked = state.config.pressEnabled
                isEnabled = !state.isSending
                setSummaryAccented(true)
                onChange<Boolean> {
                    viewModel.onPressStateChanged(it)
                }
                onClick {
                    viewModel.onPressClicked()
                }
            }
        } else {
            preference {
                title = getString(R.string.tag_more_automation_press_title)
                summary = when {
                    state.config.pressRemoteEnabled -> {
                        getString(R.string.tag_more_automation_press_content_disabled)
                    }
                    !state.hasOverlayPermission -> {
                        getString(R.string.tag_more_automation_press_content_error)
                    }
                    else -> {
                        getString(R.string.tag_more_automation_press_content_unset)
                    }
                }
                isEnabled = !state.isSending && !state.config.pressRemoteEnabled
                onClick {
                    viewModel.onPressClicked()
                }
            }
        }
        if (state.hasOverlayPermission && state.holdTitle != null && !state.config.holdRemoteEnabled) {
            switchPreferenceScreen {
                title = getString(R.string.tag_more_automation_held_title)
                summary = getString(R.string.tag_more_automation_held_content, state.holdTitle)
                isChecked = state.config.holdEnabled
                isEnabled = !state.isSending
                setSummaryAccented(true)
                onChange<Boolean> {
                    viewModel.onHoldStateChanged(it)
                }
                onClick {
                    viewModel.onHoldClicked()
                }
            }
        } else {
            preference {
                title = getString(R.string.tag_more_automation_held_title)
                summary = when {
                    state.config.holdRemoteEnabled -> {
                        getString(R.string.tag_more_automation_held_content_disabled)
                    }
                    !state.hasOverlayPermission -> {
                        getString(R.string.tag_more_automation_held_content_error)
                    }
                    else -> {
                        getString(R.string.tag_more_automation_held_content_unset)
                    }
                }
                isEnabled = !state.isSending && !state.config.holdRemoteEnabled
                onClick {
                    viewModel.onHoldClicked()
                }
            }
        }

        preferenceCategory("automation_options") {
            title = getString(R.string.tag_more_automation_category_options)
            dropDownPreference {
                title = getString(R.string.tag_more_automation_volume)
                summary = getString(state.buttonVolumeLevel.label)
                entries = ButtonVolumeLevel.entries.toTypedArray().map { getString(it.label) }
                    .toTypedArray()
                entryValues = ButtonVolumeLevel.entries.toTypedArray().map { it.name }.toTypedArray()
                value = state.buttonVolumeLevel.name
                isEnabled = !state.isSending
                onChange<String> {
                    val option = ButtonVolumeLevel.valueOf(it)
                    viewModel.onButtonVolumeChanged(option)
                }
            }
        }

        preferenceCategory("automation_custom") {
            tipsCardPreference {
                title = getString(R.string.tag_more_automation_footer_title)
                summary = createFooterContent()
            }
        }
    }

    private fun setupEvents() {
        whenResumed {
            viewModel.events.collect {
                handleEvent(it)
            }
        }
    }

    private fun createFooterContent(): CharSequence {
        return SpannableStringBuilder().apply {
            appendLine(getString(R.string.tag_more_automation_footer_content, args.deviceLabel))
            appendLine()
            val items = resources.getTextArray(R.array.tag_more_automation_footer_items)
            items.forEachIndexed { i, item ->
                appendBullet()
                append(item)
                if(i < items.size - 1) {
                    appendLine()
                }
            }
        }
    }

    private fun SpannableStringBuilder.appendBullet() {
        append(
            " ",
            BulletSpan(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.ERROR -> Toast.makeText(
                requireContext(), R.string.tag_more_automation_error_toast, Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tag_more_automation_error_dialog_title)
            setMessage(R.string.tag_more_automation_error_dialog_content)
            setCancelable(false)
            setPositiveButton(R.string.tag_more_automation_error_dialog_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.onBackPressed()
            }
        }.show()
    }

    private fun showWarningDialog() {
        if(isShowingWarning) return
        isShowingWarning = true
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tag_more_automation_shared_tag_warning_dialog_title)
            setMessage(R.string.tag_more_automation_shared_tag_warning_dialog_content)
            setPositiveButton(R.string.tag_more_automation_shared_tag_warning_dialog_dismiss) { dialog, _ ->
                dialog.dismiss()
                viewModel.onWarningDismissed()
            }
            setCancelable(false)
        }.show()
    }

}