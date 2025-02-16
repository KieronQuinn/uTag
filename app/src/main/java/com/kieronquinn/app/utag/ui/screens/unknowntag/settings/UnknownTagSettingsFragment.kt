package com.kieronquinn.app.utag.ui.screens.unknowntag.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.UtsSensitivity
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.unknowntag.settings.UnknownTagSettingsViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.seekbarPreference
import com.kieronquinn.app.utag.utils.preferences.switchBarPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class UnknownTagSettingsFragment: BaseSettingsFragment(), BackAvailable {
    
    private val viewModel by viewModel<UnknownTagSettingsViewModel>()

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
            is State.Loaded -> handleSettings(state)
        }
    }

    @SuppressLint("WrongConstant")
    private fun handleSettings(state: State.Loaded) {
        setPreferences {
            switchBarPreference {
                title = getString(R.string.settings_uts_switch)
                isChecked = state.enabled
                onChange<Boolean> {
                    viewModel.onEnabledChanged(it)
                }
            }
            preferenceCategory("uts_settings_info") {
                tipsCardPreference {
                    title = getString(R.string.settings_uts_info_title)
                    summary = getString(R.string.settings_uts_info_content)
                }
            }
            preferenceCategory("uts_settings_list") {
                preference {
                    title = getString(R.string.settings_uts_view_unknown_title)
                    onClick {
                        viewModel.onViewUnknownTagsClicked()
                    }
                }
            }
            preferenceCategory("uts_settings_options") {
                title = getString(R.string.settings_uts_category_settings)
                seekbarPreference {
                    isStepped = true
                    title = getString(R.string.settings_uts_sensitivity_title)
                    summary = getString(R.string.settings_uts_sensitivity_content)
                    min = 0
                    max = UtsSensitivity.entries.size - 1
                    value = state.sensitivity.ordinal
                    updatesContinuously = false
                    seekBarMode = 0 //MODE_STANDARD
                    description = getString(state.sensitivity.label)
                    onChange<Int> {
                        val newValue = UtsSensitivity.entries[it]
                        viewModel.onSensitivityChanged(newValue)
                        value = it
                    }
                }
            }
        }
    }

}