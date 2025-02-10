package com.kieronquinn.app.utag.ui.screens.settings.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState.Companion.isFatal
import com.kieronquinn.app.utag.service.UTagForegroundService
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.CanShowBottomNavigation
import com.kieronquinn.app.utag.ui.base.CanShowSnackbar
import com.kieronquinn.app.utag.ui.base.Root
import com.kieronquinn.app.utag.ui.screens.settings.main.SettingsMainViewModel.Event
import com.kieronquinn.app.utag.ui.screens.settings.main.SettingsMainViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsMainFragment: BaseSettingsFragment(), Root, CanShowSnackbar, CanShowBottomNavigation {

    private val viewModel by viewModel<SettingsMainViewModel>()

    private val bottomNavPadding by lazy {
        resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
    }

    private val snackbarPadding by lazy {
        resources.getDimensionPixelSize(R.dimen.snackbar_padding)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEvents()
        setupState()
        UTagForegroundService.startIfNeeded(requireContext())
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
            Event.LOGOUT_ERROR -> {
                Toast.makeText(
                    requireContext(), R.string.settings_main_logout_error, Toast.LENGTH_LONG
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
            is State.Loaded -> setList(state)
        }
    }

    private fun setList(state: State.Loaded) = setPreferences(state.getPadding()) {
        when {
            state.moduleState == null -> {
                tipsCardPreference {
                    title = getString(R.string.settings_main_mod_error_title)
                    summary = getString(R.string.settings_main_mod_no_smartthings_content)
                }
            }
            state.moduleState is ModuleState.NotModded -> {
                tipsCardPreference {
                    title = getString(R.string.settings_main_mod_error_title)
                    summary = if(state.moduleState.isUTagBuild) {
                        getString(R.string.settings_main_mod_not_activated_patch_content)
                    }else{
                        getString(R.string.settings_main_mod_not_activated_xposed_content)
                    }
                }
            }
            !state.hasPermissions -> {
                actionCardPreference {
                    title = getString(R.string.settings_main_permissions_title)
                    summary = getString(R.string.settings_main_permissions_content)
                    addButton(getString(R.string.settings_main_permissions_action)) {
                        viewModel.onSmartThingsPermissionClicked()
                    }
                }
            }
            state.moduleState is ModuleState.Outdated -> {
                tipsCardPreference {
                    title = getString(R.string.settings_main_mod_warning_title)
                    summary = if(state.moduleState.isUTagBuild) {
                        getString(R.string.settings_main_mod_outdated_patch_content)
                    }else{
                        getString(R.string.settings_main_mod_outdated_xposed_content)
                    }
                }
            }
            state.moduleState is ModuleState.Newer -> {
                tipsCardPreference {
                    title = getString(R.string.settings_main_mod_warning_title)
                    summary = if(state.moduleState.isUTagBuild) {
                        getString(R.string.settings_main_mod_newer_patch_content)
                    }else{
                        getString(R.string.settings_main_mod_newer_xposed_content)
                    }
                }
            }
            state.moduleState is ModuleState.Installed -> {
                actionCardPreference {
                    title = getString(R.string.settings_main_smartthings_title)
                    summary = getString(R.string.settings_main_smartthings_content)
                    addButton(getString(R.string.settings_main_smartthings_open)) {
                        viewModel.onSmartThingsClicked()
                    }
                }
            }
        }
        if(!state.moduleState.isFatal()) {
            preferenceCategory("main_settings") {
                title = getString(R.string.settings_main_category_settings)
                preference {
                    title = getString(R.string.settings_main_location_title)
                    summary = getString(R.string.settings_main_location_content)
                    icon = ContextCompat.getDrawable(
                        requireContext(),
                        dev.oneuiproject.oneui.R.drawable.ic_oui_search
                    )
                    onClick {
                        viewModel.onLocationClicked()
                    }
                }
                preference {
                    title = getString(R.string.settings_main_map_title)
                    summary = getString(R.string.settings_main_map_content)
                    icon = ContextCompat.getDrawable(
                        requireContext(),
                        dev.oneuiproject.oneui.R.drawable.ic_oui_location_outline
                    )
                    onClick {
                        viewModel.onMapClicked()
                    }
                }
                preference {
                    title = getString(R.string.settings_main_security_title)
                    summary = getString(R.string.settings_main_security_content)
                    icon = ContextCompat.getDrawable(
                        requireContext(),
                        dev.oneuiproject.oneui.R.drawable.ic_oui_lock_outline
                    )
                    onClick {
                        viewModel.onSecurityClicked()
                    }
                }
                preference {
                    icon = ContextCompat.getDrawable(
                        requireContext(),
                        dev.oneuiproject.oneui.R.drawable.ic_oui_settings_outline
                    )
                    title = getString(R.string.settings_main_advanced_title)
                    summary = getString(R.string.settings_main_advanced_content)
                    onClick {
                        viewModel.onAdvancedClicked()
                    }
                }
                preference {
                    icon = ContextCompat.getDrawable(
                        requireContext(),
                        dev.oneuiproject.oneui.R.drawable.ic_oui_accounts_backup
                    )
                    title = getString(R.string.settings_main_backup_restore_title)
                    summary = getString(R.string.settings_main_backup_restore_content)
                    onClick {
                        viewModel.onBackupRestoreClicked()
                    }
                }
            }
        }
        preferenceCategory("main_help") {
            title = getString(R.string.settings_main_category_help)
            preference {
                icon = ContextCompat.getDrawable(
                    requireContext(), dev.oneuiproject.oneui.R.drawable.ic_oui_help_outline
                )
                title = getString(R.string.settings_main_faq_title)
                summary = getString(R.string.settings_main_faq_content)
                onClick {
                    viewModel.onFaqClicked()
                }
            }
            preference {
                icon = ContextCompat.getDrawable(
                    requireContext(), dev.oneuiproject.oneui.R.drawable.ic_oui_reading_mode
                )
                title = getString(R.string.settings_main_wiki_title)
                summary = getString(R.string.settings_main_wiki_content)
                onClick {
                    viewModel.onWikiClicked()
                }
            }
        }
        preferenceCategory("main_account") {
            title = getString(R.string.settings_main_category_account)
            preference {
                icon = ContextCompat.getDrawable(
                    requireContext(), dev.oneuiproject.oneui.R.drawable.ic_oui_contact_outline
                )
                title = getString(R.string.settings_main_category_samsung_account_title)
                summary = state.accountInfo?.let { info ->
                    getString(
                        R.string.settings_main_category_samsung_account_content,
                        info.fullName,
                        info.email
                    )
                } ?: getString(R.string.settings_main_category_samsung_account_content_error)
                onClick {
                    onSignOutClicked()
                }
            }
        }
    }

    override fun setSnackbarVisible(visible: Boolean) {
        viewModel.onUpdateSnackbarVisiblityChanged(visible)
    }

    private fun onSignOutClicked() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.settings_main_sign_out_title)
            setMessage(getText(R.string.settings_main_sign_out_content))
            setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onSignOutClicked()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun State.Loaded.getPadding(): Int {
        return if(updateSnackbarVisible) {
            snackbarPadding + bottomNavPadding
        } else {
            bottomNavPadding
        }
    }

}