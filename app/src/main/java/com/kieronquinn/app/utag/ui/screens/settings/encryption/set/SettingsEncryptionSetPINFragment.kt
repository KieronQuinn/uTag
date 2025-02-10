package com.kieronquinn.app.utag.ui.screens.settings.encryption.set

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.updatePadding
import com.kieronquinn.app.utag.databinding.FragmentSettingsEncryptionSetPinBinding
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.utils.extensions.OTPEvent
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onEvent
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsEncryptionSetPINFragment: BoundFragment<FragmentSettingsEncryptionSetPinBinding>(FragmentSettingsEncryptionSetPinBinding::inflate), BackAvailable, ProvidesTitle {

    private val viewModel by viewModel<SettingsEncryptionSetPINViewModel>()

    private val inputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPin()
        setupInsets()
        showKeyboard()
    }

    override fun getTitle(): CharSequence {
        return ""
    }

    override fun onDestroy() {
        hideKeyboard()
        super.onDestroy()
    }

    @Suppress("DEPRECATION") //No alternative
    private fun showKeyboard() = whenResumed {
        binding.settingsEncryptionSetPinPin.requestFocusOTP()
        delay(250L)
        inputMethodManager.toggleSoftInput(
            InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }

    private fun hideKeyboard() {
        requireActivity().currentFocus?.windowToken?.let {
            inputMethodManager.hideSoftInputFromWindow(it, 0)
        }
    }

    private fun setupPin() = with(binding.settingsEncryptionSetPinPin) {
        setOTP(viewModel.pin)
        whenResumed {
            onEvent().collect {
                when(it) {
                    is OTPEvent.Changed -> viewModel.onPinChanged(it.value)
                    is OTPEvent.Complete -> viewModel.onPinComplete(it.value)
                }
            }
        }
    }

    private fun setupInsets() = with(binding.root) {
        onApplyInsets { view, insets ->
            val inset = insets.getInsets(SYSTEM_INSETS)
            view.updatePadding(bottom = inset.bottom)
        }
    }

}