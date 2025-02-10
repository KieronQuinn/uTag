package com.kieronquinn.app.utag.ui.screens.update.smartthings

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.update.smartthings.SmartThingsUpdateViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import io.noties.markwon.Markwon
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SmartThingsUpdateFragment: BaseSettingsFragment(), BackAvailable {

    private val markwon by inject<Markwon>()
    private val args by navArgs<SmartThingsUpdateFragmentArgs>()

    private val viewModel by viewModel<SmartThingsUpdateViewModel> {
        parametersOf(args.release)
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
            is State.Info -> setupInfo(state)
            is State.Downloading -> {
                setLoading(
                    true,
                    getLoadingText(state.downloadState.progressText),
                    state.downloadState.percentage
                )
            }
            is State.SettingUpHooks -> setLoading(true, getSettingUpText())
            is State.StartInstall -> setupStartInstall()
            is State.Failed -> showErrorDialog()
            is State.Complete -> viewModel.onCloseClicked()
        }
    }

    private fun getLoadingText(progressText: String): CharSequence {
        return SpannableStringBuilder().apply {
            appendLine(getString(R.string.update_downloader_downloading_title))
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

    private fun setupInfo(state: State.Info) = setPreferences {
        tipsCardPreference {
            title = getString(R.string.update_heading_smartthings)
            summary = getString(
                R.string.update_subheading,
                BuildConfig.VERSION_NAME,
                state.release.version
            )
        }
        actionCardPreference {
            title = getString(R.string.update_changes)
            summary = markwon.toMarkdown(state.release.releaseNotes)
            addButton(getString(R.string.update_action)) {
                viewModel.startDownload()
            }
            addButton(getString(R.string.update_action_alt)) {
                viewModel.onDownloadBrowserClicked()
            }
        }
    }

    private fun setupStartInstall() = setPreferences {
        actionCardPreference {
            title = getString(R.string.update_done_title)
            summary = getText(R.string.update_done_content_smartthings)
            addButton(getString(R.string.update_done_action)) {
                viewModel.startInstall()
            }
        }
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.update_dialog_failed_title)
            setMessage(R.string.update_dialog_failed_content)
            setCancelable(false)
            setPositiveButton(R.string.update_dialog_failed_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.onCloseClicked()
            }
        }.show()
    }

}