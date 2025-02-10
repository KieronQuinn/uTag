package com.kieronquinn.app.utag.ui.screens.settings.encryption.confirm

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentSettingsEncryptionConfirmPinBinding
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.ui.screens.settings.encryption.confirm.SettingsEncryptionConfirmPINViewModel.State
import com.kieronquinn.app.utag.utils.extensions.OTPEvent
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onEvent
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SettingsEncryptionConfirmPINFragment: BoundFragment<FragmentSettingsEncryptionConfirmPinBinding>(FragmentSettingsEncryptionConfirmPinBinding::inflate), BackAvailable, ProvidesTitle {

    private val args by navArgs<SettingsEncryptionConfirmPINFragmentArgs>()

    private val viewModel by viewModel<SettingsEncryptionConfirmPINViewModel> {
        parametersOf(args.pin)
    }

    private val inputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupPin()
        setupInsets()
        showKeyboard()
    }

    override fun onDestroy() {
        hideKeyboard()
        super.onDestroy()
    }

    override fun getTitle(): CharSequence {
        return ""
    }

    @Suppress("DEPRECATION") //No alternative
    private fun showKeyboard() = whenResumed {
        binding.settingsEncryptionConfirmPinPin.requestFocusOTP()
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

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) = with(binding) {
        when(state) {
            State.IDLE -> settingsEncryptionConfirmPinError.isVisible = false
            State.INVALID -> settingsEncryptionConfirmPinError.isVisible = true
            State.LOADING -> {
                hideKeyboard()
                root.setDisplayedChildIfNeeded(1)
            }
            State.COMPLETE -> {
                Toast.makeText(
                    requireContext(), R.string.settings_encryption_success, Toast.LENGTH_LONG
                ).show()
                viewModel.close()
            }
            State.ERROR -> {
                Toast.makeText(
                    requireContext(), R.string.settings_encryption_error, Toast.LENGTH_LONG
                ).show()
                viewModel.close()
            }
        }
    }

    private fun setupPin() = with(binding.settingsEncryptionConfirmPinPin) {
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