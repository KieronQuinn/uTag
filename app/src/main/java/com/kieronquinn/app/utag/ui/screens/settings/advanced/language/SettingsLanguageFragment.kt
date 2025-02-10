package com.kieronquinn.app.utag.ui.screens.settings.advanced.language

import android.os.Bundle
import android.view.View
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.advanced.language.SettingsLanguageViewModel.State
import com.kieronquinn.app.utag.utils.extensions.capitalise
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.radioButtonPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsLanguageFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsLanguageViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
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
            is State.Loaded -> state.setList()
        }
    }

    private fun State.Loaded.setList() = setPreferences {
        tipsCardPreference {
            title = getString(R.string.settings_language_info_title)
            summary = getString(R.string.settings_language_info_content)
        }
        preferenceCategory("languages") {
            title = ""
            radioButtonPreference {
                title = getString(R.string.settings_language_default)
                isChecked = selectedLocale == null
                onClick {
                    viewModel.setLanguage(null)
                }
            }
            supportedLocales.forEach {
                radioButtonPreference {
                    title = it.displayName.capitalise()
                    isChecked = it == selectedLocale
                    onClick {
                        viewModel.setLanguage(it)
                    }
                }
            }
        }
    }

}