package com.kieronquinn.app.utag.ui.screens.safearea.list

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceGroup
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.safearea.list.SafeAreaListViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import org.koin.androidx.viewmodel.ext.android.viewModel
import dev.oneuiproject.oneui.R as OneuiR

class SafeAreaListFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SafeAreaListViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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
            is State.Loaded -> handleContent(state)
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences {
        if(state.location.isNotEmpty()) {
            preferenceCategory("safe_area_location") {
                title = getString(R.string.safe_area_type_location_title)
                state.location.forEach {
                    preference {
                        title = it.name
                        summary = if(it.isActive) {
                            getString(R.string.safe_area_active)
                        }else{
                            getString(R.string.safe_area_inactive)
                        }
                        setSummaryAccented(it.isActive)
                        onClick {
                            viewModel.onSafeAreaClicked(it)
                        }
                    }
                }
            }
        }
        if(state.wifi.isNotEmpty()) {
            preferenceCategory("safe_area_wifi") {
                title = getString(R.string.safe_area_type_wifi_title)
                state.wifi.forEach {
                    preference {
                        title = it.name
                        summary = if(it.isActive) {
                            getString(R.string.safe_area_active)
                        }else{
                            getString(R.string.safe_area_inactive)
                        }
                        setSummaryAccented(it.isActive)
                        onClick {
                            viewModel.onSafeAreaClicked(it)
                        }
                    }
                }
            }
        }
        val addItem: PreferenceGroup.() -> Unit = {
            preference {
                title = getString(R.string.safe_area_add)
                icon = ContextCompat.getDrawable(requireContext(), OneuiR.drawable.ic_oui_add_filled)
                onClick {
                    viewModel.onAddClicked()
                }
            }
        }
        //Only space if there's already items
        if(state.location.isEmpty() && state.wifi.isEmpty()) {
            addItem()
        }else{
            preferenceCategory("safe_area_add") {
                addItem()
            }
        }
    }

}