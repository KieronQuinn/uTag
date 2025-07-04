package com.kieronquinn.app.utag.ui.screens.setup.mod

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.Formatter
import android.text.style.RelativeSizeSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.setup.mod.SetupModViewModel.State
import com.kieronquinn.app.utag.ui.screens.setup.mod.SetupModViewModel.State.Error.ErrorReason
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupModFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SetupModViewModel>()
    private var isShowingErrorDialog = false

    private val connectivityManager by lazy {
        requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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
        when(state) {
            is State.Loading -> setLoading(true)
            is State.Info -> handleInfo(state)
            is State.OneUI -> handleOneUI(state)
            is State.StartingDownload -> setLoading(true)
            is State.Downloading -> {
                setLoading(true, getLoadingText(state.progressText), state.progress)
            }
            is State.SettingUpHooks -> setLoading(true, getSettingUpText())
            is State.Uninstall -> handleUninstall()
            is State.Install -> handleInstall()
            is State.Setup -> handleSetup()
            is State.Complete -> viewModel.onNext()
            is State.Error -> showErrorDialog(state.reason)
        }
    }

    private fun getLoadingText(progressText: String): CharSequence {
        return SpannableStringBuilder().apply {
            appendLine(getString(R.string.setup_mod_downloading))
            append(
                progressText,
                RelativeSizeSpan(0.75f),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
    }

    private fun getSettingUpText(): CharSequence {
        return SpannableStringBuilder().apply {
            appendLine(getString(R.string.setup_mod_setting_up))
            append(
                getString(R.string.setup_mod_setting_up_subtitle),
                RelativeSizeSpan(0.75f),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
    }

    private fun handleInfo(state: State.Info) = setPreferences {
        actionCardPreference {
            title = getString(R.string.setup_mod_info_title)
            summary = getText(R.string.setup_mod_info_content)
            addButton(getString(R.string.setup_mod_info_action)) {
                if(connectivityManager.isActiveNetworkMetered) {
                    showMeteredConnectionDialog(state.fileSize)
                }else{
                    viewModel.onStartDownloadClicked()
                }
            }
        }
    }

    private fun handleOneUI(state: State.OneUI) = setPreferences {
        actionCardPreference {
            title = if(state.compatible) {
                getString(R.string.setup_mod_oneui_title)
            }else{
                getString(R.string.setup_mod_oneui_incompatible_title)
            }
            summary = if(state.compatible) {
                getText(R.string.setup_mod_oneui_desc)
            }else{
                getText(R.string.setup_mod_oneui_incompatible_desc)
            }
            if(state.compatible) {
                addButton(getString(R.string.setup_mod_oneui_continue)) {
                    viewModel.onAcceptOneUIClicked()
                }
            }
        }
    }

    private fun handleUninstall() = setPreferences {
        actionCardPreference {
            title = getString(R.string.setup_mod_uninstall_title)
            summary = getText(R.string.setup_mod_uninstall_content)
            addButton(getString(R.string.setup_mod_uninstall_action)) {
                viewModel.onUninstallClicked()
            }
        }
    }

    private fun handleInstall() = setPreferences {
        actionCardPreference {
            title = getString(R.string.setup_mod_install_title)
            summary = getText(R.string.setup_mod_install_content)
            addButton(getString(R.string.setup_mod_install_action)) {
                viewModel.onInstallClicked()
            }
        }
    }

    private fun handleSetup() = setPreferences {
        actionCardPreference {
            title = getString(R.string.setup_mod_setup_title)
            summary = getString(R.string.setup_mod_setup_content)
            addButton(getString(R.string.setup_mod_setup_action)) {
                viewModel.onSetupClicked()
            }
        }
    }

    private fun showErrorDialog(errorReason: ErrorReason) {
        if(isShowingErrorDialog) return
        isShowingErrorDialog = true
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.setup_mod_error_title)
            setMessage(errorReason.message)
            setCancelable(false)
            setPositiveButton(R.string.setup_mod_error_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.onCloseClicked()
            }
        }.show()
    }

    private fun showMeteredConnectionDialog(fileSize: Long) {
        val formattedFileSize = Formatter.formatFileSize(requireContext(), fileSize)
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.setup_mod_metered_dialog_title)
            setMessage(getString(R.string.setup_mod_metered_dialog_content, formattedFileSize))
            setPositiveButton(R.string.setup_mod_metered_dialog_positive) { dialog, _ ->
                dialog.dismiss()
                viewModel.onStartDownloadClicked()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

}