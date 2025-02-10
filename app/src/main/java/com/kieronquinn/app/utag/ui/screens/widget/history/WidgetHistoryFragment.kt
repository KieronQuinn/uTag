package com.kieronquinn.app.utag.ui.screens.widget.history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.ItemWidgetPreviewBinding
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.ui.activities.BaseWidgetConfigurationActivity.Companion.getAppWidgetId
import com.kieronquinn.app.utag.ui.activities.BaseWidgetConfigurationActivity.Companion.getCallingPackage
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesBack
import com.kieronquinn.app.utag.ui.base.ProvidesOverflow
import com.kieronquinn.app.utag.ui.screens.widget.history.WidgetHistoryViewModel.Event
import com.kieronquinn.app.utag.ui.screens.widget.history.WidgetHistoryViewModel.PreviewState
import com.kieronquinn.app.utag.ui.screens.widget.history.WidgetHistoryViewModel.State
import com.kieronquinn.app.utag.ui.screens.widget.picker.WidgetDevicePickerFragment.Companion.setupSingleTagPickerListener
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.dropDownPreference
import com.kieronquinn.app.utag.utils.preferences.layoutPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WidgetHistoryFragment: BaseSettingsFragment(), BackAvailable, ProvidesBack, ProvidesOverflow {

    companion object {
        const val KEY_TAG_PICKER = "tag_picker"
    }

    private val viewModel by viewModel<WidgetHistoryViewModel> {
        parametersOf(getAppWidgetId(requireActivity()), getCallingPackage(requireActivity()))
    }

    private val preview by lazy {
        //We need to use a LayoutInflater that doesn't use AppCompat components
        val vanillaLayoutInflater = LayoutInflater.from(requireContext().applicationContext)
        ItemWidgetPreviewBinding.inflate(vanillaLayoutInflater)
    }

    override val backIcon: Int
        get() = dev.oneuiproject.oneui.R.drawable.ic_oui_close

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEvents()
        setupListeners()
        setupState()
    }

    override fun onBackPressed(): Boolean {
        if(viewModel.hasChanges) {
            showSaveDialog()
        }else{
            requireActivity().finish()
        }
        return true
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_widget_configuration, menu)
        menu.findItem(R.id.widget_configuration_save).actionView
            ?.findViewById<Button>(R.id.action_layout_save_button)
            ?.setOnClickListener { viewModel.onSaveClicked() }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        //Handled with custom listener
        return true
    }

    private fun setupEvents() = whenResumed {
        viewModel.events.collect {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.NO_DEVICE -> {
                Toast.makeText(
                    requireContext(), R.string.widget_configuration_set_device, Toast.LENGTH_LONG
                ).show()
            }
            Event.SET_RESULT_AND_CLOSE -> {
                requireActivity().setResult(Activity.RESULT_OK, Intent())
                requireActivity().finish()
            }
        }
    }

    private fun setupListeners() {
        setupSingleTagPickerListener(KEY_TAG_PICKER) {
            viewModel.onDeviceChanged(it)
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
            is State.Loaded -> handleContent(state)
            is State.Error -> showErrorDialog()
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences {
        preview.setup(state.previewState)
        preferenceCategory("preview_category") {
            title = getString(R.string.widget_configuration_preview_label)
            layoutPreference(preview.root, "preview")
        }
        if(state.biometricsEnabled) {
            preferenceCategory("biometric_warning") {
                tipsCardPreference {
                    title = getString(R.string.widget_configuration_biometric_warning_title)
                    summary = getString(R.string.widget_configuration_biometric_warning_content)
                }
            }
        }
        preferenceCategory("options") {
            title = getString(R.string.widget_configuration_options_label)
            preference {
                title = getString(R.string.widget_configuration_history_device_title)
                summary = if(state.device == null) {
                    getString(R.string.widget_configuration_history_device_content)
                }else{
                    state.device.label
                }
                onClick { viewModel.onDeviceClicked() }
            }
            dropDownPreference {
                title = getString(R.string.settings_map_style_title)
                value = state.mapStyle.name
                summary = getString(state.mapStyle.label)
                entries = MapStyle.entries.toTypedArray().map { getString(it.label) }.toTypedArray()
                entryValues = MapStyle.entries.toTypedArray().map { it.name }.toTypedArray()
                isEnabled = state.device != null
                onChange<String> {
                    val option = MapStyle.valueOf(it)
                    viewModel.onMapStyleChanged(option)
                }
            }
            dropDownPreference {
                title = getString(R.string.settings_map_theme_title)
                value = state.mapTheme.name
                summary = if(state.mapStyle == MapStyle.NORMAL) {
                    getString(state.mapTheme.label)
                }else{
                    getString(R.string.settings_map_theme_disabled)
                }
                entries = MapTheme.entries.toTypedArray()
                    .map { getString(it.label) }.toTypedArray()
                entryValues = MapTheme.entries.toTypedArray().map { it.name }.toTypedArray()
                isEnabled = state.mapStyle == MapStyle.NORMAL && state.device != null
                onChange<String> {
                    val option = MapTheme.valueOf(it)
                    viewModel.onMapThemeChanged(option)
                }
            }
        }
        preferenceCategory("footer") {
            tipsCardPreference {
                title = getString(R.string.widget_configuration_location_footer_title)
                summary = getString(R.string.widget_configuration_location_footer_content)
            }
        }
    }

    private fun ItemWidgetPreviewBinding.setup(previewState: PreviewState) {
        when(previewState) {
            is PreviewState.None -> root.setDisplayedChildIfNeeded(0)
            is PreviewState.Error -> root.setDisplayedChildIfNeeded(1)
            is PreviewState.Loaded -> {
                widgetPreviewContainer.removeAllViews()
                widgetPreviewContainer.addView(
                    previewState.remoteViews.apply(root.context, widgetPreviewContainer)
                )
                root.setDisplayedChildIfNeeded(2)
            }
            is PreviewState.Loading -> root.setDisplayedChildIfNeeded(3)
        }
    }

    private fun showSaveDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.widget_configuration_dialog_save_title)
            setMessage(R.string.widget_configuration_dialog_save_content)
            setPositiveButton(R.string.widget_configuration_dialog_save_save) { _, _ ->
                viewModel.onSaveClicked()
            }
            setNegativeButton(R.string.widget_configuration_dialog_save_dont_save) { _, _ ->
                requireActivity().finish()
            }
        }.show()
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.widget_configuration_error_dialog_title)
            setMessage(R.string.widget_configuration_error_dialog_content)
            setCancelable(false)
            setPositiveButton(R.string.widget_configuration_error_dialog_close) { dialog, _ ->
                dialog.dismiss()
                requireActivity().finish()
            }
        }.show()
    }

}