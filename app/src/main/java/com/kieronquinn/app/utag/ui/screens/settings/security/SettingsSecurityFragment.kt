package com.kieronquinn.app.utag.ui.screens.settings.security

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.security.SettingsSecurityViewModel.State
import com.kieronquinn.app.utag.utils.extensions.BiometricEvent
import com.kieronquinn.app.utag.utils.extensions.showBiometricPrompt
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsSecurityFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsSecurityViewModel>()

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
            is State.Loaded -> handleLoaded(state)
        }
    }

    private fun handleLoaded(state: State.Loaded) = setPreferences {
        preference {
            title = getString(R.string.settings_encryption_title)
            summary = getString(R.string.settings_encryption_content)
            onClick {
                viewModel.onEncryptionClicked()
            }
        }
        switchPreference {
            title = getString(R.string.settings_biometric_title)
            summary = if(state.biometricsAvailable) {
                getString(R.string.settings_biometric_content)
            }else{
                getString(R.string.settings_biometric_content_disabled)
            }
            isChecked = state.biometricsEnabled
            isEnabled = state.biometricsAvailable
            onChange<Boolean> {
                if(it) {
                    showBiometricPrompt()
                }else{
                    viewModel.onBiometricsDisabled()
                }
            }
        }
    }

    private fun showBiometricPrompt() = lifecycleScope.launch {
        showBiometricPrompt(
            R.string.biometric_prompt_title,
            R.string.biometric_prompt_content_enable,
            R.string.biometric_prompt_content_negative
        ).collect {
            when(it) {
                is BiometricEvent.Success -> viewModel.onBiometricsEnabled()
                else -> {
                    //No-op
                }
            }
        }
    }

}