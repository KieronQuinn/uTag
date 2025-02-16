package com.kieronquinn.app.utag.ui.screens.tag.more.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceGroup
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.networking.model.smartthings.SetSearchingStatusRequest.SearchingStatus
import com.kieronquinn.app.utag.repositories.ApiRepository.Companion.FIND_PRIVACY_URL
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesBack
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.more.container.MoreContainerFragmentArgs
import com.kieronquinn.app.utag.ui.screens.tag.more.main.MoreMainViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.more.main.MoreMainViewModel.State
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.Companion.setupPinEntryResultListener
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.PinEntryResult
import com.kieronquinn.app.utag.utils.extensions.containerParentNavArgs
import com.kieronquinn.app.utag.utils.extensions.formatTimeSince
import com.kieronquinn.app.utag.utils.extensions.navigateTo
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import com.kieronquinn.app.utag.utils.preferences.switchPreferenceScreen
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MoreMainFragment : BaseSettingsFragment(), BackAvailable, ProvidesBack, ProvidesSubtitle {

    private val args by containerParentNavArgs<MoreContainerFragmentArgs>()

    private val viewModel by viewModel<MoreMainViewModel> {
        parametersOf(args.deviceId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupEvents()
        setupPinResult()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
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
        when (state) {
            is State.Loading -> setLoading(true)
            is State.Loaded -> {
                setLoading(false)
                setContentWithHeader(state)
            }

            is State.PINRequired -> viewModel.showPinEntry()
            is State.Error -> showErrorDialog()
        }
    }

    private fun setupPinResult() {
        setupPinEntryResultListener {
            when (it) {
                is PinEntryResult.Success -> {
                    viewModel.onPinEntered(it)
                }

                is PinEntryResult.Failed -> {
                    viewModel.onPinCancelled()
                }
            }
        }
    }

    private fun setupEvents() = whenResumed {
        viewModel.events.collect {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Event) {
        when (event) {
            Event.ERROR -> {
                Toast.makeText(requireContext(), R.string.tag_more_error_toast, Toast.LENGTH_LONG)
                    .show()
            }

            Event.SHAREABLE_ENABLED -> showShareableDialog()
            Event.NOTIFY_ENABLED -> showNotifyDialog()
        }
    }

    private fun setContentWithHeader(state: State.Loaded) = setPreferences {
        if(state.offline) {
            preferenceCategory("offline") {
                tipsCardPreference {
                    title = getString(R.string.map_offline_dialog_title)
                    summary = getString(R.string.map_offline_dialog_content)
                }
            }
            preferenceCategory("main") {
                setContent(state)
            }
        }else{
            setContent(state)
        }
    }

    private fun PreferenceGroup.setContent(state: State.Loaded) {
        val isSharedDevice = !state.deviceInfo.isOwner
        if(!state.requiresAgreement && !state.swapLocationHistory) {
            preference {
                title = getString(R.string.tag_more_search_nearby)
                summary = if(state.passiveModeEnabled) {
                    getString(R.string.tag_more_search_nearby_disabled)
                } else null
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search_nearby)
                isEnabled = !state.passiveModeEnabled
                onClick {
                    viewModel.onNearbyClicked()
                }
            }
        }
        if(state.swapLocationHistory) {
            preference {
                title = getString(R.string.map_location_history)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location_history)
                onClick {
                    viewModel.onLocationHistoryClicked()
                }
            }
        }
        if (state.navigationLocation != null && !state.isCloseBy) {
            preference {
                title = getString(R.string.tag_more_navigate)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_navigate)
                onClick {
                    requireContext().navigateTo(state.navigationLocation)
                }
            }
        }
        if(!state.requiresAgreement) {
            preferenceCategory("more_notifications") {
                title = getString(R.string.tag_more_category_notifications)
                if (!isSharedDevice && !state.isScannedOrConnected) {
                    switchPreference {
                        title = getString(R.string.tag_more_notify_when_found)
                        isChecked = state.deviceInfo.searchingStatus == SearchingStatus.SEARCHING
                        isEnabled = !state.isSending
                        onChange<Boolean> {
                            viewModel.onNotifyChanged(it)
                        }
                    }
                }
                if (state.notifyDisconnectEnabled || !state.notifyDisconnectHasWarning) {
                    switchPreferenceScreen {
                        title = getString(R.string.tag_more_notify_when_disconnected_title)
                        when {
                            state.notifyDisconnectHasWarning -> {
                                summary = getString(
                                    R.string.tag_more_notify_when_disconnected_content_warning
                                )
                                setSummaryAccented(true)
                            }

                            state.notifyDisconnectEnabled && state.safeAreaCount > 0 -> {
                                summary = resources.getQuantityString(
                                    R.plurals.tag_more_notify_when_disconnected_content_enabled,
                                    state.safeAreaCount,
                                    state.safeAreaCount
                                )
                                setSummaryAccented(true)
                            }

                            state.notifyDisconnectEnabled -> {
                                summary = getString(
                                    R.string.tag_more_notify_when_disconnected_content_enabled_none
                                )
                                setSummaryAccented(true)
                            }

                            else -> {
                                summary = getString(
                                    R.string.tag_more_notify_when_disconnected_content_disabled,
                                    state.deviceInfo.label
                                )
                                setSummaryAccented(false)
                            }
                        }
                        isChecked = state.notifyDisconnectEnabled
                        onClick { viewModel.onNotifyDisconnectClicked() }
                        onChange<Boolean> {
                            viewModel.onNotifyDisconnectChanged(it)
                        }
                    }
                } else {
                    preference {
                        title = getString(R.string.tag_more_notify_when_disconnected_title)
                        summary = getString(
                            R.string.tag_more_notify_when_disconnected_content_disabled,
                            state.deviceInfo.label
                        )
                        setSummaryAccented(false)
                        onClick {
                            viewModel.onNotifyDisconnectClicked()
                        }
                    }
                }
            }
        }
        if (!isSharedDevice) {
            preferenceCategory("more_automations") {
                title = getString(R.string.tag_more_category_automations)
                if ((state.findDeviceEnabled || !state.findDeviceHasWarning) && !state.passiveModeEnabled) {
                    switchPreferenceScreen {
                        title = getString(R.string.tag_more_find_device_title)
                        summary = when {
                            state.findDeviceHasWarning -> {
                                getString(R.string.tag_more_find_device_content_error)
                            }
                            state.findDeviceEnabled -> {
                                getString(R.string.tag_more_find_device_content_enabled)
                            }
                            else -> {
                                getString(R.string.tag_more_find_device_content)
                            }
                        }
                        isChecked = state.findDeviceEnabled
                        setSummaryAccented(state.findDeviceEnabled)
                        onClick {
                            viewModel.onFindDeviceClicked()
                        }
                        onChange<Boolean> {
                            viewModel.onFindDeviceChanged(it)
                        }
                    }
                } else {
                    preference {
                        title = getString(R.string.tag_more_find_device_title)
                        summary = when {
                            state.passiveModeEnabled -> {
                                getString(R.string.tag_more_find_device_content_disabled)
                            }
                            else -> {
                                getString(R.string.tag_more_find_device_content)
                            }
                        }
                        isEnabled = !state.passiveModeEnabled
                        setSummaryAccented(false)
                        onClick {
                            viewModel.onFindDeviceClicked()
                        }
                    }
                }
                preference {
                    title = getString(R.string.tag_more_automation_title)
                    summary = when {
                        state.passiveModeEnabled -> {
                            getString(R.string.tag_more_automation_content_disabled_passive)
                        }
                        state.isConnected -> {
                            getString(R.string.tag_more_automation_content)
                        }
                        else -> {
                            getString(R.string.tag_more_automation_content_disabled)
                        }
                    }
                    isEnabled = state.isConnected && !state.passiveModeEnabled
                    setSummaryAccented(false)
                    onClick {
                        viewModel.onAutomationsClicked()
                    }
                }
            }
        }
        if (!isSharedDevice && state.lostModeState != null) {
            preferenceCategory("more_protect") {
                title = getString(R.string.tag_more_category_protect)
                preference {
                    title = getString(R.string.tag_more_lost_mode_title)
                    summary = if (state.lostModeState.enabled) {
                        getString(R.string.tag_more_lost_mode_on)
                    } else {
                        getString(R.string.tag_more_lost_mode_off)
                    }
                    setSummaryAccented(state.lostModeState.enabled)
                    onClick {
                        viewModel.onLostModeClicked()
                    }
                }
                //E2E toggle requires a Tag & network connection, otherwise show without a toggle
                if(state.e2eEnabled != null && state.e2eAvailable != null) {
                    switchPreference {
                        title = getString(R.string.tag_more_end_to_end_title)
                        summary = if (state.e2eAvailable) {
                            getString(R.string.tag_more_end_to_end_content)
                        } else {
                            getString(R.string.tag_more_end_to_end_content_pin)
                        }
                        //Enabled if E2E is available (always), or if the user needs to disable it
                        isEnabled = (state.e2eAvailable || state.e2eEnabled) && !state.isSending
                        isChecked = state.e2eEnabled
                        onChange<Boolean> {
                            viewModel.one2eChanged(it)
                        }
                    }
                }else{
                    preference {
                        title = getString(R.string.tag_more_end_to_end_title)
                        summary = when {
                            state.e2eAvailable == null -> {
                                getString(R.string.tag_more_end_to_end_content_network)
                            }
                            state.passiveModeEnabled -> {
                                getString(R.string.tag_more_end_to_end_content_passive)
                            }
                            else -> {
                                getString(R.string.tag_more_end_to_end_content_disabled)
                            }
                        }
                        isEnabled = false
                    }
                }
            }
        }
        preferenceCategory("more_share") {
            title = getString(R.string.tag_more_category_share)
            switchPreference {
                if (isSharedDevice) {
                    title = getString(R.string.tag_more_share)
                } else {
                    title = getString(R.string.tag_more_shareable)
                    summary = getString(R.string.tag_more_shareable_content)
                }
                isChecked = state.deviceInfo.shareable
                isEnabled = !state.isSending
                onChange<Boolean> {
                    when {
                        isSharedDevice && it -> {
                            if(!showShareLocationDialog()) {
                                //Couldn't get consent info, just enable
                                viewModel.onShareableUserChanged(true)
                            }
                        }
                        isSharedDevice -> {
                            viewModel.onShareableUserChanged(false)
                        }
                        else -> {
                            viewModel.onShareableChanged(it)
                        }
                    }
                }
            }
        }
        if(!state.requiresAgreement) {
            preferenceCategory("more_advanced") {
                title = getString(R.string.tag_more_advanced)
                preference {
                    title = getString(R.string.tag_more_passive_mode_title)
                    summary = if (state.passiveModeEnabled) {
                        getString(R.string.tag_more_passive_mode_content_enabled)
                    } else {
                        getString(R.string.tag_more_passive_mode_content_disabled)
                    }
                    setSummaryAccented(state.passiveModeEnabled)
                    onClick {
                        viewModel.onPassiveModeClicked()
                    }
                }
            }
        }
        val batteryLevel = state.deviceInfo.batteryLevel
        val batteryUpdatedAt = state.deviceInfo.batteryUpdatedAt
        if (batteryLevel != null && batteryUpdatedAt != null) {
            preferenceCategory("more_battery") {
                title = getString(R.string.tag_more_battery)
                preference {
                    icon = ContextCompat.getDrawable(requireContext(), batteryLevel.icon)
                    title = getString(batteryLevel.label)
                    summary = getString(
                        R.string.battery_update,
                        batteryUpdatedAt.formatTimeSince()
                    )
                    onClick {
                        viewModel.onBatteryClicked()
                    }
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        viewModel.onBackPressed()
        return true
    }

    override fun getSubtitle(): CharSequence {
        return args.deviceName
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tag_more_error_title)
            setMessage(R.string.tag_more_error_content)
            setCancelable(false)
            setPositiveButton(R.string.tag_more_error_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.onBackPressed()
            }
        }.show()
    }

    private fun showShareableDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setMessage(R.string.tag_more_shareable_dialog)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showNotifyDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setMessage(R.string.tag_more_notify_when_found_dialog)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showShareLocationDialog(): Boolean {
        val region = (viewModel.state.value as? State.Loaded)?.region ?: return false
        val privacyUrl = FIND_PRIVACY_URL.format(region.lowercase())
        val content = Html.fromHtml(
            getString(R.string.map_allow_access_dialog_content, privacyUrl),
            Html.FROM_HTML_MODE_COMPACT
        )
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_allow_access_dialog_title)
            setMessage(content)
            setPositiveButton(R.string.map_allow_access_dialog_positive) { _, _ ->
                viewModel.onShareableUserChanged(true)
            }
            setNegativeButton(R.string.map_allow_access_dialog_negative) { dialog, _ ->
                dialog.dismiss()
            }
        }.create().apply {
            show()
            findViewById<TextView>(android.R.id.message)?.setupConsentText()
        }
        return true
    }

    private fun TextView.setupConsentText() {
        Linkify.addLinks(this, Linkify.WEB_URLS)
        setLinkTextColor(ContextCompat.getColor(requireContext(), R.color.oui_accent_color))
        highlightColor = ContextCompat.getColor(requireContext(), R.color.oui_accent_color_disabled)
        movementMethod = BetterLinkMovementMethod.newInstance().apply {
            setOnLinkClickListener { _, url ->
                val intent = CustomTabsIntent.Builder().apply {
                    setShowTitle(false) //Policy pages don't have a title, show the URL
                    setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                    setBookmarksButtonEnabled(false)
                    setDownloadButtonEnabled(false)
                }.build().intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    data = Uri.parse(url)
                }
                startActivity(intent)
                true
            }
        }
    }

}