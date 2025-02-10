package com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.customurl

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.customurl.LostModeCustomURLViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.customurl.LostModeCustomURLViewModel.SetInitialError
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.customurl.LostModeCustomURLViewModel.State
import com.kieronquinn.app.utag.utils.extensions.copyToClipboard
import com.kieronquinn.app.utag.utils.extensions.setTextToEnd
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.editTextPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LostModeCustomURLFragment: BaseSettingsFragment(), BackAvailable, ProvidesSubtitle {

    private val args by navArgs<LostModeCustomURLFragmentArgs>()
    private var isShowingError = false

    private val viewModel by viewModel<LostModeCustomURLViewModel> {
        parametersOf(args.deviceId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEvents()
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupEvents() = whenResumed {
        viewModel.events.collect {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.ERROR -> Toast.makeText(
                requireContext(),
                R.string.lost_mode_settings_custom_url_error_toast,
                Toast.LENGTH_LONG
            ).show()
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
            is State.Loaded -> handleContent(state)
            is State.Error -> showErrorDialog()
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences {
        if(!state.isLoading) {
            if (state.customUrl != null && state.savedUrl == null) {
                //Custom URL is set but the saved copy is gone, show the warning
                tipsCardPreference {
                    title = getString(R.string.lost_mode_settings_custom_url_lost_title)
                    summary = getText(R.string.lost_mode_settings_custom_url_lost_content)
                }
            } else {
                //Show the regular message, with an action if the URL can be saved
                actionCardPreference {
                    title = getString(R.string.lost_mode_settings_custom_url_warning_title)
                    summary = getText(R.string.lost_mode_settings_custom_url_warning_content)
                    if (state.saveableUrl != null) {
                        addButton(getString(R.string.lost_mode_settings_custom_url_warning_action)) {
                            copyUrlToClipboard(state.saveableUrl)
                        }
                    }
                }
            }
        }
        preferenceCategory("lost_mode_custom_url") {
            editTextPreference {
                title = getString(R.string.lost_mode_settings_custom_url_set_title)
                summary = state.customUrl
                    ?: getText(R.string.lost_mode_settings_custom_url_set_summary)
                setSummaryAccented(state.customUrl != null)
                dialogTitle = getString(R.string.lost_mode_settings_custom_url_set_title)
                dialogMessage = getString(R.string.lost_mode_settings_custom_url_set_dialog_message)
                isEnabled = !state.isLoading
                setOnBindEditTextListener {
                    it.setTextToEnd(state.customUrl)
                    it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                }
                onChange<String> {
                    val url = it.trim()
                    when(viewModel.onCustomUrlChanged(url)) {
                        SetInitialError.WARN -> showWarningDialog(url)
                        SetInitialError.ERROR -> showSetErrorDialog()
                        SetInitialError.INVALID_URL -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.lost_mode_settings_custom_url_invalid_url_toast,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        null -> {
                            //No-op
                        }
                    }
                }
            }
        }
        preferenceCategory("lost_mode_footer") {
            tipsCardPreference {
                title = getString(R.string.lost_mode_settings_custom_url_footer_title)
                summary = getText(R.string.lost_mode_settings_custom_url_footer_content)
            }
        }
    }

    private fun copyUrlToClipboard(url: String) {
        requireContext().copyToClipboard(
            url,
            getString(
                R.string.lost_mode_settings_custom_url_warning_action_label,
                args.deviceLabel
            )
        )
    }

    override fun getSubtitle(): CharSequence {
        return args.deviceLabel
    }

    private fun showErrorDialog() {
        if(isShowingError) return
        isShowingError = true
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.lost_mode_settings_error_dialog_title)
            setMessage(R.string.lost_mode_settings_error_dialog_content)
            setCancelable(false)
            setPositiveButton(R.string.lost_mode_settings_error_dialog_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.close()
            }
        }.show()
    }

    private fun showWarningDialog(url: String) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.lost_mode_settings_custom_url_set_warning_dialog_title)
            setMessage(R.string.lost_mode_settings_custom_url_set_warning_dialog_content)
            setPositiveButton(R.string.lost_mode_settings_custom_url_set_warning_dialog_confirm) { dialog, _ ->
                dialog.dismiss()
                viewModel.onCustomUrlChanged(url, true)
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showSetErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.lost_mode_settings_custom_url_set_error_dialog_title)
            setMessage(R.string.lost_mode_settings_custom_url_set_error_dialog_content)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

}