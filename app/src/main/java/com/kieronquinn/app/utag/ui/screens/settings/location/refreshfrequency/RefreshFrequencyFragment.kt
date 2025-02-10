package com.kieronquinn.app.utag.ui.screens.settings.location.refreshfrequency

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.RefreshPeriod
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.radioButtonPreference
import com.kieronquinn.app.utag.xposed.extensions.getSerializableCompat
import org.koin.androidx.viewmodel.ext.android.viewModel

class RefreshFrequencyFragment: BaseSettingsFragment(), BackAvailable {

    companion object {
        private const val KEY_REFRESH_FREQUENCY = "refresh_frequency"
        private const val KEY_RESULT = "result"

        fun Fragment.setupRefreshFrequencyListener(callback: (result: RefreshPeriod) -> Unit) {
            setFragmentResultListener(KEY_REFRESH_FREQUENCY) { requestKey, bundle ->
                if(requestKey != KEY_REFRESH_FREQUENCY) return@setFragmentResultListener
                val result = bundle.getSerializableCompat(KEY_RESULT, RefreshPeriod::class.java)
                    ?: return@setFragmentResultListener
                callback.invoke(result)
            }
        }
    }

    private val args by navArgs<RefreshFrequencyFragmentArgs>()
    private val viewModel by viewModel<RefreshFrequencyViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selected = args.selected as RefreshPeriod
        setPreferences {
            RefreshPeriod.entries.forEach { item ->
                radioButtonPreference {
                    title = getString(item.label)
                    summary = when(item) {
                        RefreshPeriod.FIVE_MINUTES -> {
                            getString(R.string.settings_location_location_period_summary_high)
                        }
                        RefreshPeriod.SIXTY_MINUTES -> {
                            getString(R.string.settings_location_location_period_summary_low)
                        }
                        RefreshPeriod.NEVER -> {
                            getText(R.string.settings_location_location_period_summary_never)
                        }
                        else -> null
                    }
                    isChecked = item == selected
                    onClick { onSelected(item) }
                }
            }
        }
    }

    private fun onSelected(refreshPeriod: RefreshPeriod) {
        setFragmentResult(KEY_REFRESH_FREQUENCY, bundleOf(KEY_RESULT to refreshPeriod))
        viewModel.back()
    }

}