package com.kieronquinn.app.utag.ui.screens.settings.advanced

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.advanced.SettingsAdvancedViewModel.State
import com.kieronquinn.app.utag.utils.extensions.capitalise
import com.kieronquinn.app.utag.utils.extensions.getSelectedLanguage
import com.kieronquinn.app.utag.utils.extensions.getSupportedLocales
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsAdvancedFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsAdvancedViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences {
        preference {
            val supported = context.getSupportedLocales().sortedBy { it.displayName.lowercase() }
            val selected = context.getSelectedLanguage(supported.map { it.toLanguageTag() })
            title = getString(R.string.settings_advanced_language)
            summary = selected?.displayName?.capitalise()
                ?: getString(R.string.settings_language_default)
            onClick {
                viewModel.onLanguageClicked()
            }
        }
        preference {
            title = getString(R.string.settings_advanced_content_creator_title)
            summary = if(state.contentCreatorMode) {
                getString(R.string.settings_advanced_content_creator_enabled)
            }else{
                getString(R.string.settings_advanced_content_creator_content)
            }
            setSummaryAccented(state.contentCreatorMode)
            onClick { viewModel.onContentCreatorModeClicked() }
        }
        switchPreference {
            title = getString(R.string.settings_advanced_analytics_title)
            summary = getString(R.string.settings_advanced_analytics_content)
            isChecked = state.analyticsEnabled
            onChange<Boolean> {
                viewModel.onAnalyticsChanged(it)
            }
        }
        switchPreference {
            title = getString(R.string.settings_advanced_auto_dismiss_notifications_title)
            summary = getText(R.string.settings_advanced_auto_dismiss_notifications_content)
            isChecked = state.autoDismissNotifications
            onChange<Boolean> {
                viewModel.onAutoDismissNotificationsChanged(it)
            }
        }
        switchPreference {
            title = getString(R.string.settings_advanced_prevent_overmature_title)
            summary = getText(R.string.settings_advanced_prevent_overmature_content)
            isChecked = state.preventOvermature
            onChange<Boolean> {
                viewModel.onPreventOvermatureChanged(it)
            }
        }
        preference {
            title = getString(R.string.settings_advanced_clear_addresses_title)
            summary = getString(R.string.settings_advanced_clear_addresses_content)
            onClick {
                showClearAddressDialog()
            }
        }
        if(state.debugVisible) {
            switchPreference {
                title = getString(R.string.settings_advanced_debug_title)
                summary = getString(R.string.settings_advanced_debug_content)
                isChecked = state.debugEnabled
                onChange<Boolean> {
                    viewModel.onDebugChanged(it)
                }
            }
        }
    }

    private fun showClearAddressDialog() {
        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(getString(R.string.settings_advanced_clear_addresses_dialog_title))
            setMessage(getString(R.string.settings_advanced_clear_addresses_dialog_content))
            setPositiveButton(R.string.settings_advanced_clear_addresses_dialog_positive) { dialog, _ ->
                dialog.dismiss()
                viewModel.onAddressCacheCleared()
                Toast.makeText(
                    requireContext(),
                    R.string.settings_advanced_clear_addresses_toast,
                    Toast.LENGTH_LONG
                ).show()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
        dialog.getButton(Dialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.negative_red))
    }

}