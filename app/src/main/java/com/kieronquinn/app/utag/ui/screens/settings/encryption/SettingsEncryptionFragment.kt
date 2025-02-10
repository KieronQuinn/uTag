package com.kieronquinn.app.utag.ui.screens.settings.encryption

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager.Authenticators
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceGroup
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.PinTimeout
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.settings.encryption.SettingsEncryptionViewModel.State
import com.kieronquinn.app.utag.ui.screens.settings.encryption.pintimeout.PinTimeoutFragment.Companion.setupPinTimeoutListener
import com.kieronquinn.app.utag.utils.extensions.BiometricEvent
import com.kieronquinn.app.utag.utils.extensions.or
import com.kieronquinn.app.utag.utils.extensions.showBiometricPrompt
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class SettingsEncryptionFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsEncryptionViewModel>()

    private val timestampFormatSameYear by lazy {
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "d MMM, HH:mm")
        )
    }

    private val timestampFormatDifferentYear by lazy {
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "d MMM yyyy, HH:mm")
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupListener()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupListener() {
        setupPinTimeoutListener {
            viewModel.onPinTimeoutChanged(it)
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
            is State.Loaded -> setContent(state)
            is State.Error -> showErrorDialog()
        }
    }

    private fun setContent(state: State.Loaded) = setPreferences {
        preferenceCategory("encryption_pin") {
            title = getString(R.string.settings_encryption_category_pin)
            if(state.encryptionEnabled) {
                if (state.keyTimestamp != null) {
                    val time = state.keyTimestamp.format()
                    preference {
                        title = getString(R.string.settings_encryption_reset_pin_title)
                        summary = getString(R.string.settings_encryption_reset_pin_content, time)
                        onClick {
                            showResetWarningDialog()
                        }
                    }
                } else {
                    preference {
                        title = getString(R.string.settings_encryption_set_pin_title)
                        summary = getString(R.string.settings_encryption_set_pin_content)
                        onClick {
                            if (state.biometricsAvailable) {
                                showBiometricPromptForPINChange(state.requireBiometrics)
                            } else {
                                viewModel.onSetClicked()
                            }
                        }
                    }
                }
                if(state.biometricsAvailable) {
                    switchPreference {
                        title = getString(R.string.settings_encryption_require_biometrics_title)
                        summary = getString(R.string.settings_encryption_require_biometrics_content)
                        isChecked = state.requireBiometrics
                        onChange<Boolean> {
                            showBiometricPromptForBiometricRequirement(it)
                        }
                    }
                }
            }
            addClearPreference(state.hasSavedPin)
            if(!state.hasSavedPin) {
                addTimeoutPreference(state.pinTimeout)
            }
        }
        if(state.encryptionEnabled) {
            preferenceCategory("encryption_footer") {
                tipsCardPreference {
                    title = getString(R.string.settings_encryption_footer_title)
                    summary = getString(R.string.settings_encryption_footer_content)
                }
            }
        }
    }

    private fun PreferenceGroup.addClearPreference(enabled: Boolean) {
        preference {
            title = getString(R.string.settings_encryption_clear_saved_pins_title)
            summary = if(enabled) {
                getString(R.string.settings_encryption_clear_saved_pins_content)
            }else{
                getString(R.string.settings_encryption_clear_saved_pins_content_disabled)
            }
            isEnabled = enabled
            onClick {
                showClearWarningDialog()
            }
        }
    }

    private fun PreferenceGroup.addTimeoutPreference(timeout: PinTimeout) {
        preference {
            title = getString(R.string.settings_encryption_pin_timeout_title)
            summary = getString(
                R.string.settings_encryption_pin_timeout_content, getString(timeout.label)
            )
            onClick {
                viewModel.onPinTimeoutClicked()
            }
        }
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.settings_encryption_error_dialog_title)
            setMessage(R.string.settings_encryption_error_dialog_content)
            setPositiveButton(R.string.settings_encryption_error_dialog_close) { _, _ ->
                viewModel.onBackPressed()
            }
            setCancelable(false)
        }.show()
    }

    private fun showResetWarningDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.settings_encryption_reset_warning_dialog_title)
            setMessage(R.string.settings_encryption_reset_warning_dialog_content)
            setPositiveButton(R.string.settings_encryption_reset_warning_dialog_positive) { _, _ ->
                viewModel.onSetClicked()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showClearWarningDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.settings_encryption_clear_saved_pins_title)
            setMessage(R.string.settings_encryption_clear_saved_pins_dialog_content)
            setPositiveButton(R.string.settings_encryption_clear_saved_pins_dialog_positive) { _, _ ->
                viewModel.onClearClicked()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showBiometricPromptForPINChange(
        requireBiometrics: Boolean
    ) = lifecycleScope.launch {
        val authenticationMethods = if(requireBiometrics) {
            Authenticators.BIOMETRIC_STRONG
        }else{
            listOf(
                Authenticators.BIOMETRIC_STRONG,
                Authenticators.BIOMETRIC_WEAK,
                Authenticators.DEVICE_CREDENTIAL
            ).or()
        }
        showBiometricPrompt(
            R.string.biometric_prompt_title,
            R.string.biometric_prompt_content_change_pin,
            R.string.biometric_prompt_content_negative,
            allowedAuthenticators = authenticationMethods
        ).collect {
            when(it) {
                is BiometricEvent.Success -> viewModel.onSetClicked()
                is BiometricEvent.Error -> if(requireBiometrics) {
                    showBiometricErrorDialog()
                }
                else -> {
                    //No-op
                }
            }
        }
    }

    private fun showBiometricPromptForBiometricRequirement(
        stateAfter: Boolean
    ) = lifecycleScope.launch {
        val message = if(stateAfter) {
            R.string.biometric_prompt_content_enable
        }else{
            R.string.biometric_prompt_content_disable
        }
        showBiometricPrompt(
            R.string.biometric_prompt_title,
            message,
            R.string.biometric_prompt_content_negative
        ).collect {
            when(it) {
                is BiometricEvent.Success -> viewModel.onBiometricsRequiredChanged(stateAfter)
                else -> {
                    //No-op
                }
            }
        }
    }

    private fun showBiometricErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.settings_encryption_biometrics_failed_title)
            setMessage(getString(R.string.settings_encryption_biometrics_failed_content))
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }


    private fun LocalDateTime.format(): String {
        val now = LocalDateTime.now()
        return if(now.year == year) {
            format(timestampFormatSameYear)
        }else{
            format(timestampFormatDifferentYear)
        }
    }

}