package com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse.PredefinedMessage
import com.kieronquinn.app.utag.networking.model.smartthings.SetSearchingStatusRequest.SearchingStatus
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.LostModeSettingsViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.LostModeSettingsViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.editTextPreference
import com.kieronquinn.app.utag.utils.preferences.listPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.setInputType
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import com.kieronquinn.app.utag.utils.preferences.switchBarPreference
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LostModeSettingsFragment: BaseSettingsFragment(), BackAvailable, ProvidesSubtitle {

    private val args by navArgs<LostModeSettingsFragmentArgs>()

    private val viewModel by viewModel<LostModeSettingsViewModel> {
        parametersOf(args.deviceId, args.deviceLabel)
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
                setLoading(false)
                handleContent(state)
            }
            is State.Error -> showErrorDialog()
        }
    }

    private fun setupEvents() {
        whenResumed {
            viewModel.events.collect {
                handleEvent(it)
            }
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.ERROR -> Toast.makeText(
                requireContext(), R.string.lost_mode_settings_error_toast, Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences {
        val enabled = !state.isSaving && state.lostModeState.enabled && !state.isCustom
        if(!state.isCustom || state.isConnected) {
            switchBarPreference {
                title = getString(R.string.lost_mode_settings_switch)
                summary = if(state.isCustom){
                    getString(R.string.lost_mode_settings_disabled)
                }else null
                isChecked = state.lostModeState.enabled || state.isCustom
                isEnabled = !state.isSaving && !state.isCustom
                onChange(viewModel::onEnabledChanged)
            }
            preferenceCategory("lost_mode_personalisation") {
                title = getString(R.string.lost_mode_settings_category)
                editTextPreference {
                    title = getString(R.string.lost_mode_settings_email)
                    summary = when {
                        state.contentCreatorModeEnabled -> "@"
                        !state.isCustom -> {
                            state.lostModeState.email ?: state.email
                        }
                        else -> {
                            getString(R.string.lost_mode_settings_disabled)
                        }
                    }
                    text = state.lostModeState.email ?: state.email
                    dialogTitle = getString(R.string.lost_mode_settings_email)
                    setInputType(
                        EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    )
                    isEnabled = enabled
                    onChange<String> {
                        if (it.isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                R.string.lost_mode_settings_email_cannot_be_empty,
                                Toast.LENGTH_LONG
                            ).show()
                            return@onChange
                        }
                        viewModel.onEmailChanged(it)
                    }
                }
                editTextPreference {
                    title = getString(R.string.lost_mode_settings_phone_number)
                    summary = if(!state.isCustom) {
                        state.lostModeState.phoneNumber
                            ?: getString(R.string.lost_mode_settings_phone_number_default)
                    }else{
                        getString(R.string.lost_mode_settings_disabled)
                    }
                    text = state.lostModeState.phoneNumber
                    dialogTitle = getString(R.string.lost_mode_settings_phone_number)
                    setInputType(
                        EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_CLASS_PHONE
                    )
                    isEnabled = enabled
                    onChange<String> {
                        viewModel.onPhoneNumberChanged(it)
                    }
                }
                listPreference {
                    title = getString(R.string.lost_mode_settings_message)
                    dialogTitle = getString(R.string.lost_mode_settings_message)
                    summary = if(!state.isCustom){
                        state.lostModeState.getMessageContent()
                    }else{
                        getString(R.string.lost_mode_settings_disabled)
                    }
                    isEnabled = enabled
                    entries = getListEntries().toTypedArray()
                    entryValues = getListEntryValues().toTypedArray()
                    value = state.lostModeState.getListEntryValue()
                    onChange<CharSequence> {
                        PredefinedMessage.fromMessage(it.toString())?.let { message ->
                            viewModel.onPredefinedMessageChanged(message)
                        }
                    }
                }
            }
        } else {
            tipsCardPreference {
                title = getString(R.string.lost_mode_custom_url_set_title)
                summary = getText(R.string.lost_mode_custom_url_set_content)
            }
        }
        preferenceCategory("lost_mode_advanced") {
            title = getString(R.string.lost_mode_settings_category_advanced)
            preference {
                title = getString(R.string.lost_mode_settings_custom_url_title)
                summary = when {
                    state.passiveModeEnabled -> {
                        getString(R.string.lost_mode_settings_custom_url_content_passive)
                    }
                    !state.isConnected -> {
                        getString(R.string.lost_mode_settings_custom_url_content_disconnected)
                    }
                    !state.isCustom -> {
                        getString(R.string.lost_mode_settings_custom_url_content_unset)
                    }
                    else -> state.lostModeUrl
                }
                isEnabled = state.isConnected && !state.passiveModeEnabled
                setSummaryAccented(state.isCustom && state.isConnected && !state.passiveModeEnabled)
                onClick {
                    viewModel.onCustomUrlClicked()
                }
            }
        }
        if(!state.isScannedOrConnected) {
            preferenceCategory("lost_mode_notifications") {
                title = getString(R.string.tag_more_category_notifications)
                switchPreference {
                    title = getString(R.string.tag_more_notify_when_found)
                    isChecked = state.searchingStatus == SearchingStatus.SEARCHING
                    isEnabled = !state.isSaving
                    onChange<Boolean> {
                        viewModel.onNotifyChanged(it)
                    }
                }
            }
        }
    }

    private fun LostModeRequestResponse.getMessageContent(): String {
        return getPredefinedMessage()?.displayContent?.let {
            getString(it)
        } ?: getString(R.string.this_smarttag_has_been_lost)
    }

    private fun getListEntries(): List<String> {
        return PredefinedMessage.entries.map {
            getString(it.displayContent)
        }
    }

    private fun LostModeRequestResponse.getListEntryValue(): String {
        return getPredefinedMessage()?.name
            ?: PredefinedMessage.DREAM_SACP_BODY_THIS_SMARTTAG_HAS_BEEN_LOST.name
    }

    private fun getListEntryValues(): List<String> {
        return PredefinedMessage.entries.map { it.name }
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.lost_mode_settings_error_dialog_title)
            setMessage(R.string.lost_mode_settings_error_dialog_content)
            setCancelable(false)
            setPositiveButton(R.string.lost_mode_settings_error_dialog_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.onBackPressed()
            }
        }.show()
    }

}