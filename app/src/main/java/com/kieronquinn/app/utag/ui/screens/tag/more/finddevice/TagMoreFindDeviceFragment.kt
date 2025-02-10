package com.kieronquinn.app.utag.ui.screens.tag.more.finddevice

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository.WarningAction
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.more.finddevice.TagMoreFindDeviceViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.more.finddevice.TagMoreFindDeviceViewModel.State
import com.kieronquinn.app.utag.utils.extensions.appendBullet
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.seekbarPreference
import com.kieronquinn.app.utag.utils.preferences.switchBarPreference
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.cbrt
import kotlin.math.pow

class TagMoreFindDeviceFragment: BaseSettingsFragment(), BackAvailable, ProvidesSubtitle {

    private val args by navArgs<TagMoreFindDeviceFragmentArgs>()
    private var isShowingWarning = false
    private var contentHash: Int? = null

    private val viewModel by viewModel<TagMoreFindDeviceViewModel> {
        parametersOf(args.deviceId, args.isSharedDevice)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun getSubtitle(): CharSequence {
        return args.deviceLabel
    }

    private fun setupEvents() = whenCreated {
        viewModel.events.collect {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.NETWORK_ERROR -> {
                Toast.makeText(
                    requireContext(), R.string.tag_more_find_device_error_toast, Toast.LENGTH_LONG
                ).show()
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
                setContent(state)
                if(state.showWarning) {
                    showWarningDialog()
                }
            }
        }
    }

    private fun setContent(state: State.Loaded) {
        val contentHash = state.copy(config = state.config.copy(volume = 0f)).hashCode()
        if(this.contentHash == contentHash) return
        this.contentHash = contentHash
        setPreferences {
            val available = (state.errorState == null || !state.errorState.isCritical)
                    && state.config.enabled
            if (state.errorState != null) {
                actionCardPreference {
                    title = getString(state.errorState.title)
                    summary = getText(state.errorState.content)
                    addButton(getString(state.errorState.action.label)) {
                        when (state.errorState.action) {
                            WarningAction.NOTIFICATION_SETTINGS -> {
                                viewModel.onNotificationSettingsClicked()
                            }

                            WarningAction.NOTIFICATION_CHANNEL_SETTINGS -> {
                                viewModel.onNotificationChannelSettingsClicked()
                            }

                            WarningAction.FULL_SCREEN_INTENT_SETTINGS -> {
                                viewModel.onFullScreenIntentSettingsClicked()
                            }

                            WarningAction.DISABLE_REMOTE_RING, WarningAction.DISABLE_PET_WALKING -> {
                                viewModel.onDisableSmartThingsActionsClicked()
                            }
                        }
                    }
                }
            }
            val switchBar = {
                switchBarPreference {
                    title = getString(R.string.tag_more_find_device_switch)
                    isChecked = state.config.enabled
                    onChange<Boolean> {
                        viewModel.onEnabledChanged(it)
                    }
                }
            }
            if (state.errorState == null) {
                switchBar()
            }
            if (state.errorState?.isCritical == false) {
                preferenceCategory("find_device_switch") {
                    title = ""
                    switchBar()
                }
            }
            preferenceCategory("find_device_settings") {
                title = getString(R.string.tag_more_find_device_category_settings)
                switchPreference {
                    isEnabled = available
                    title = getString(R.string.tag_more_find_device_vibrate_title)
                    summary = getString(R.string.tag_more_find_device_vibrate_content)
                    isChecked = state.config.vibrate
                    onChange<Boolean> {
                        viewModel.onVibrateChanged(it)
                    }
                }
                switchPreference {
                    isEnabled = available
                    title = getString(R.string.tag_more_find_device_delay_title)
                    summary = getText(R.string.tag_more_find_device_delay_content)
                    isChecked = state.config.delay
                    onChange<Boolean> {
                        viewModel.onDelayChanged(it)
                    }
                }
                seekbarPreference {
                    isEnabled = available
                    title = getString(R.string.tag_more_find_device_volume_title)
                    summary = SpannableStringBuilder().apply {
                        appendLine(getText(R.string.tag_more_find_device_volume_content))
                    }
                    min = 0
                    max = 100
                    value = (cbrt(state.config.volume * 10000f)).toInt()
                    updatesContinuously = false
                    onChange<Int> {
                        val adjusted = it.toDouble().pow(3) / 10000f
                        viewModel.onVolumeChanged(adjusted.toFloat())
                        value = it
                    }
                }
            }

            preferenceCategory("find_device_footer") {
                tipsCardPreference {
                    title = getString(R.string.tag_more_find_device_footer_title)
                    summary = createFooterContent()
                }
            }
        }
    }

    private fun createFooterContent(): CharSequence {
        return SpannableStringBuilder().apply {
            appendLine(getString(R.string.tag_more_find_device_footer_content, args.deviceLabel))
            appendLine()
            val items = resources.getTextArray(R.array.tag_more_find_device_footer_items)
            items.forEachIndexed { i, item ->
                appendBullet()
                append(item)
                if(i < items.size - 1) {
                    appendLine()
                }
            }
        }
    }

    private fun showWarningDialog() {
        if(isShowingWarning) return
        isShowingWarning = true
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tag_more_find_device_shared_tag_warning_dialog_title)
            setMessage(R.string.tag_more_find_device_shared_tag_warning_dialog_content)
            setPositiveButton(R.string.tag_more_find_device_shared_tag_warning_dialog_dismiss) { dialog, _ ->
                dialog.dismiss()
                viewModel.onFindMyDeviceWarningDismissed()
            }
            setCancelable(false)
        }.show()
    }

}