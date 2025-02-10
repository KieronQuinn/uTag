package com.kieronquinn.app.utag.ui.screens.settings.location.chaser

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.location.chaser.SettingsChaserViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.switchBarPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsChaserFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsChaserViewModel>()

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
            is State.Error -> showErrorDialog()
            is State.Loaded -> handleSettings(state)
        }
    }

    private fun handleSettings(state: State.Loaded) = setPreferences {
        switchBarPreference {
            title = getString(R.string.settings_chaser_switch)
            isChecked = state.enabled
            onChange<Boolean> {
                viewModel.onEnabledChanged(it)
            }
        }
        preferenceCategory("chaser_info") {
            tipsCardPreference {
                title = getString(R.string.settings_chaser_info_title)
                summary = getText(R.string.settings_chaser_info_content)
            }
        }
        preferenceCategory("chaser_settings") {
            preference {
                title = getString(R.string.settings_chaser_count_title)
                summary = resources.getQuantityString(
                    R.plurals.settings_chaser_count_content,
                    state.count,
                    state.count
                )
            }
        }
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setCancelable(false)
            setTitle(R.string.settings_chaser_error_title)
            setMessage(R.string.settings_chaser_error_content)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

}