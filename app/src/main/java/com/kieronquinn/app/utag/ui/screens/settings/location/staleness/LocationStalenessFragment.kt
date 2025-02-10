package com.kieronquinn.app.utag.ui.screens.settings.location.staleness

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.LocationStaleness
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.utils.extensions.label
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.radioButtonPreference
import com.kieronquinn.app.utag.xposed.extensions.getSerializableCompat
import org.koin.androidx.viewmodel.ext.android.viewModel

class LocationStalenessFragment: BaseSettingsFragment(), BackAvailable {

    companion object {
        private const val KEY_LOCATION_STALENESS = "location_staleness"
        private const val KEY_RESULT = "result"

        fun Fragment.setupLocationStalenessListener(callback: (result: LocationStaleness) -> Unit) {
            setFragmentResultListener(KEY_LOCATION_STALENESS) { requestKey, bundle ->
                if(requestKey != KEY_LOCATION_STALENESS) return@setFragmentResultListener
                val result = bundle.getSerializableCompat(KEY_RESULT, LocationStaleness::class.java)
                    ?: return@setFragmentResultListener
                callback.invoke(result)
            }
        }
    }

    private val args by navArgs<LocationStalenessFragmentArgs>()
    private val viewModel by viewModel<LocationStalenessViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selected = args.current as LocationStaleness
        setPreferences {
            LocationStaleness.entries.forEach { item ->
                radioButtonPreference {
                    title = getString(item.label)
                    summary = when(item) {
                        LocationStaleness.NONE -> {
                            getString(R.string.settings_location_location_staleness_none)
                        }
                        LocationStaleness.THREE_MINUTES -> {
                            getString(R.string.settings_location_location_staleness_max)
                        }
                        else -> null
                    }
                    isChecked = item == selected
                    onClick { onSelected(item) }
                }
            }
        }
    }

    private fun onSelected(locationStaleness: LocationStaleness) {
        setFragmentResult(KEY_LOCATION_STALENESS, bundleOf(KEY_RESULT to locationStaleness))
        viewModel.back()
    }

}