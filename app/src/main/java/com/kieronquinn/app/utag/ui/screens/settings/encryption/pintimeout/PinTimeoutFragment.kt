package com.kieronquinn.app.utag.ui.screens.settings.encryption.pintimeout

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.PinTimeout
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.xposed.extensions.getSerializableCompat
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.radioButtonPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class PinTimeoutFragment: BaseSettingsFragment(), BackAvailable {

    companion object {
        private const val KEY_PIN_TIMEOUT = "pin_timeout"
        private const val KEY_RESULT = "result"

        fun Fragment.setupPinTimeoutListener(callback: (result: PinTimeout) -> Unit) {
            setFragmentResultListener(KEY_PIN_TIMEOUT) { requestKey, bundle ->
                if(requestKey != KEY_PIN_TIMEOUT) return@setFragmentResultListener
                val result = bundle.getSerializableCompat(KEY_RESULT, PinTimeout::class.java)
                    ?: return@setFragmentResultListener
                callback.invoke(result)
            }
        }
    }

    private val args by navArgs<PinTimeoutFragmentArgs>()
    private val viewModel by viewModel<PinTimeoutViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selected = args.current as PinTimeout
        setPreferences {
            PinTimeout.entries.forEach { item ->
                radioButtonPreference {
                    title = getString(item.label)
                    summary = when(item) {
                        PinTimeout.NEVER -> {
                            getString(R.string.settings_encryption_pin_timeout_subtitle)
                        }
                        else -> null
                    }
                    isChecked = item == selected
                    onClick { onSelected(item) }
                }
            }
        }
    }

    private fun onSelected(pinTimeout: PinTimeout) {
        setFragmentResult(KEY_PIN_TIMEOUT, bundleOf(KEY_RESULT to pinTimeout))
        viewModel.back()
    }

}