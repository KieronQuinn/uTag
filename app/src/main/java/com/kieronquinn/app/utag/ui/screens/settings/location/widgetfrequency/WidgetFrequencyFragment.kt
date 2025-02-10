package com.kieronquinn.app.utag.ui.screens.settings.location.widgetfrequency

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.WidgetRefreshPeriod
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.radioButtonPreference
import com.kieronquinn.app.utag.xposed.extensions.getSerializableCompat
import org.koin.androidx.viewmodel.ext.android.viewModel

class WidgetFrequencyFragment: BaseSettingsFragment(), BackAvailable {

    companion object {
        private const val KEY_WIDGET_FREQUENCY = "widget_frequency"
        private const val KEY_RESULT = "result"

        fun Fragment.setupWidgetFrequencyListener(callback: (result: WidgetRefreshPeriod) -> Unit) {
            setFragmentResultListener(KEY_WIDGET_FREQUENCY) { requestKey, bundle ->
                if(requestKey != KEY_WIDGET_FREQUENCY) return@setFragmentResultListener
                val result = bundle.getSerializableCompat(KEY_RESULT, WidgetRefreshPeriod::class.java)
                    ?: return@setFragmentResultListener
                callback.invoke(result)
            }
        }
    }

    private val args by navArgs<WidgetFrequencyFragmentArgs>()
    private val viewModel by viewModel<WidgetFrequencyViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selected = args.current as WidgetRefreshPeriod
        setPreferences {
            WidgetRefreshPeriod.entries.forEach { item ->
                radioButtonPreference {
                    title = getString(item.label)
                    summary = when(item) {
                        WidgetRefreshPeriod.ONE_HOUR -> {
                            getString(R.string.settings_location_location_period_summary_high)
                        }
                        WidgetRefreshPeriod.NEVER -> {
                            getString(R.string.settings_location_widget_period_never_content)
                        }
                        else -> null
                    }
                    isChecked = item == selected
                    onClick { onSelected(item) }
                }
            }
        }
    }

    private fun onSelected(refreshPeriod: WidgetRefreshPeriod) {
        setFragmentResult(KEY_WIDGET_FREQUENCY, bundleOf(KEY_RESULT to refreshPeriod))
        viewModel.back()
    }

}