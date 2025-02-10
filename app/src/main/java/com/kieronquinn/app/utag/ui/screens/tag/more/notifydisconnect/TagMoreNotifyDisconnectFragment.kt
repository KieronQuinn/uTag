package com.kieronquinn.app.utag.ui.screens.tag.more.notifydisconnect

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.ui.screens.tag.more.notifydisconnect.TagMoreNotifyDisconnectViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import com.kieronquinn.app.utag.utils.preferences.switchBarPreference
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import com.kieronquinn.app.utag.utils.preferences.switchPreferenceScreen
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import dev.oneuiproject.oneui.R as OneuiR

class TagMoreNotifyDisconnectFragment: BaseSettingsFragment(), BackAvailable, ProvidesSubtitle {

    private val args by navArgs<TagMoreNotifyDisconnectFragmentArgs>()

    private val viewModel by viewModel<TagMoreNotifyDisconnectViewModel> {
        parametersOf(args.deviceId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun getSubtitle(): CharSequence {
        return args.deviceLabel
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
        val available = state.warning == null || !state.warning.isCritical
        if(available) {
            switchBarPreference {
                title = getString(R.string.tag_more_notify_when_disconnected_switch)
                isChecked = state.enabled
                onChange<Boolean> {
                    viewModel.onEnabledChanged(it)
                }
            }
        }
        preferenceCategory("notify_disconnect_header") {
            if(state.warning != null) {
                actionCardPreference {
                    title = getString(state.warning.title)
                    summary = getString(state.warning.content)
                    addButton(getString(state.warning.action.label)) {
                        viewModel.onWarningActionClicked(state.warning.action)
                    }
                }
            }else{
                tipsCardPreference {
                    title =
                        getString(R.string.tag_more_notify_when_disconnected_safe_areas_title)
                    summary =
                        getString(R.string.tag_more_notify_when_disconnected_safe_areas_content)
                }
            }
        }
        if(state.locationSafeAreas.isNotEmpty()) {
            preferenceCategory("notify_disconnect_location") {
                title = getString(R.string.safe_area_type_location_title)
                state.locationSafeAreas.forEach {
                    switchPreferenceScreen {
                        title = it.name
                        summary = if(it.isActive) {
                            getString(R.string.safe_area_active)
                        }else{
                            getString(R.string.safe_area_inactive)
                        }
                        setSummaryAccented(it.isActive)
                        isChecked = it.activeDeviceIds.contains(args.deviceId)
                        isEnabled = state.enabled && available
                        onChange<Boolean> { enabled ->
                            viewModel.onSafeAreaChanged(it, enabled)
                        }
                        onClick {
                            viewModel.onSafeAreaClicked(it)
                        }
                    }
                }
            }
        }
        if(state.wifiSafeAreas.isNotEmpty()) {
            preferenceCategory("notify_disconnect_wifi") {
                title = getString(R.string.safe_area_type_wifi_title)
                state.wifiSafeAreas.forEach {
                    switchPreferenceScreen {
                        title = it.name
                        summary = if(it.isActive) {
                            getString(R.string.safe_area_active)
                        }else{
                            getString(R.string.safe_area_inactive)
                        }
                        isChecked = it.activeDeviceIds.contains(args.deviceId)
                        isEnabled = state.enabled && available
                        setSummaryAccented(it.isActive)
                        onChange<Boolean> { enabled ->
                            viewModel.onSafeAreaChanged(it, enabled)
                        }
                        onClick {
                            viewModel.onSafeAreaClicked(it)
                        }
                    }
                }
            }
        }
        preferenceCategory("notify_disconnect_add") {
            preference {
                title = getString(R.string.safe_area_add)
                icon = ContextCompat.getDrawable(requireContext(), OneuiR.drawable.ic_oui_add_filled)
                isEnabled = state.enabled && available
                onClick {
                    viewModel.onAddSafeAreaClicked()
                }
            }
        }
        preferenceCategory("notify_disconnect_settings") {
            title = getString(R.string.safe_area_category_settings)
            switchPreference {
                title = getString(R.string.safe_area_show_image_title)
                summary = getImageSummary(state.areBiometricsEnabled)
                isEnabled = state.enabled
                isChecked = state.showImage
                onChange<Boolean> {
                    viewModel.onShowImageChanged(it)
                }
            }
        }
    }

    private fun getImageSummary(biometricsEnabled: Boolean): CharSequence {
        return SpannableStringBuilder().apply {
            append(getString(R.string.safe_area_show_image_content))
            if(biometricsEnabled) {
                appendLine()
                appendLine()
                append(getText(R.string.safe_area_show_image_content_biometrics))
            }
        }
    }

}