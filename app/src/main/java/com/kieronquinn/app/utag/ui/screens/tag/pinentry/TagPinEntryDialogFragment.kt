package com.kieronquinn.app.utag.ui.screens.tag.pinentry

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentTagPinEntryDialogBinding
import com.kieronquinn.app.utag.ui.base.BaseDialogFragment
import com.kieronquinn.app.utag.utils.extensions.onChanged
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import `in`.aabhasjindal.otptextview.OTPListener
import `in`.aabhasjindal.otptextview.OtpTextView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

class TagPinEntryDialogFragment: BaseDialogFragment<FragmentTagPinEntryDialogBinding>(FragmentTagPinEntryDialogBinding::inflate) {

    companion object {
        private const val KEY_PIN_ENTRY = "pin_entry"
        private const val KEY_PIN = "pin"
        private const val KEY_SAVE = "save"

        fun Fragment.setupPinEntryResultListener(callback: (result: PinEntryResult) -> Unit) {
            setFragmentResultListener(KEY_PIN_ENTRY) { requestKey, bundle ->
                if(requestKey != KEY_PIN_ENTRY) return@setFragmentResultListener
                val pin = bundle.getString(KEY_PIN)
                val save = bundle.getBoolean(KEY_SAVE, false)
                val result = when {
                    pin != null -> PinEntryResult.Success(pin, save)
                    else -> PinEntryResult.Failed
                }
                callback.invoke(result)
            }
        }
    }

    private val viewModel by viewModel<TagPinEntryDialogViewModel>()
    private val args by navArgs<TagPinEntryDialogFragmentArgs>()

    private val inputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override val isDismissable = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupContent()
        setupError()
        setupPin()
        setupSave()
        setupCancel()
        setupOk()
        setupPin()
        showKeyboard()
    }

    override fun onDestroy() {
        hideKeyboard()
        super.onDestroy()
    }

    @Suppress("DEPRECATION") //No alternative
    private fun showKeyboard() = whenResumed {
        binding.tagPinEntryEntry.requestFocusOTP()
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

    private fun setupContent() = with(binding.tagPinEntryContent) {
        text = if(args.isHistory) {
            getString(R.string.tag_pin_entry_content_alt, args.deviceName)
        }else{
            getString(R.string.tag_pin_entry_content, args.deviceName)
        }
    }

    private fun setupError() = with(binding.tagPinEntryError) {
        isVisible = args.isError
    }

    private fun setupPin() = with(binding.tagPinEntryEntry) {
        setOTP(viewModel.pin)
        whenResumed {
            onChanged().collect {
                viewModel.pin = it
            }
        }
    }

    private fun setupSave() = with(binding.tagPinEntrySave) {
        isChecked = viewModel.saveChecked
        whenResumed {
            onChanged().collect {
                viewModel.saveChecked = it
            }
        }
    }

    private fun setupCancel() = with(binding.tagPinEntryCancel) {
        whenResumed {
            onClicked().collect {
                setResult()
                dismiss()
            }
        }
    }

    private fun setupOk() = with(binding.tagPinEntryOk) {
        whenResumed {
            onClicked().collect {
                val pin = binding.tagPinEntryEntry.otp
                val save = binding.tagPinEntrySave.isChecked
                setResult(save, pin)
                dismiss()
            }
        }
    }

    private fun setResult(save: Boolean = false, pin: String? = null) {
        setFragmentResult(
            KEY_PIN_ENTRY,
            bundleOf(KEY_PIN to pin, KEY_SAVE to save)
        )
    }

    private fun OtpTextView.onChanged() = callbackFlow {
        otpListener = object: OTPListener {
            override fun onInteractionListener() {
                trySend(this@onChanged.otp ?: "")
            }

            override fun onOTPComplete(otp: String) {
                //No-op
            }
        }
        awaitClose {
            otpListener = null
        }
    }

    sealed class PinEntryResult {
        data class Success(
            val pin: String,
            val save: Boolean
        ): PinEntryResult()
        data object Failed: PinEntryResult()
    }

}