package com.kieronquinn.app.utag.ui.screens.widget.picker

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceScreen
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesBack
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.ui.screens.widget.picker.WidgetDevicePickerViewModel.Category
import com.kieronquinn.app.utag.ui.screens.widget.picker.WidgetDevicePickerViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.glideCheckBoxPreference
import com.kieronquinn.app.utag.utils.preferences.glidePreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WidgetDevicePickerFragment: BaseSettingsFragment(), BackAvailable, ProvidesBack, ProvidesTitle {

    companion object {
        private const val KEY_RESULT = "result"

        fun Fragment.setupSingleTagPickerListener(key: String, callback: (result: String) -> Unit) {
            setFragmentResultListener(key) { requestKey, bundle ->
                if(requestKey != key) return@setFragmentResultListener
                val result = bundle.getString(KEY_RESULT)
                callback.invoke(result ?: return@setFragmentResultListener)
            }
        }

        fun Fragment.setupMultipleTagPickerListener(key: String, callback: (result: Array<String>) -> Unit) {
            setFragmentResultListener(key) { requestKey, bundle ->
                if(requestKey != key) return@setFragmentResultListener
                val result = bundle.getStringArray(KEY_RESULT)
                callback.invoke(result ?: return@setFragmentResultListener)
            }
        }
    }

    private val args by navArgs<WidgetDevicePickerFragmentArgs>()

    private val viewModel by viewModel<WidgetDevicePickerViewModel> {
        parametersOf(args.selectedDeviceIds, args.popUpTo, args.knownDeviceIds)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun getTitle(): CharSequence {
        return getString(args.title)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
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
            is State.Loaded -> setContent(state)
            is State.Error -> showErrorDialog()
        }
    }

    private fun setContent(state: State.Loaded) = setPreferences {
        state.categories.forEach {
            when(it) {
                is Category.Mine -> addMine(it, state.selected)
                is Category.Shared -> addShared(it, state.selected)
            }
        }
    }

    private fun PreferenceScreen.addMine(mine: Category.Mine, selected: List<String>) {
        if(mine.items.isEmpty()) return
        preferenceCategory("device_picker_mine") {
            title = getString(R.string.tag_picker_category_mine)
            mine.items.forEach {
                val deviceId = it.deviceId
                val isSelected = selected.contains(deviceId)
                if(args.isMultiSelect) {
                    glideCheckBoxPreference {
                        title = it.label
                        url = it.icon
                        isChecked = isSelected
                        onChange<Boolean> { checked ->
                            viewModel.onSelectedChanged(deviceId, checked)
                        }
                    }
                }else{
                    glidePreference {
                        title = it.label
                        url = it.icon
                        summary = if (isSelected) {
                            getString(R.string.tag_picker_selected)
                        } else null
                        setSummaryAccented(isSelected)
                        onClick { onItemSelected(it.deviceId) }
                    }
                }
            }
        }
    }

    private fun PreferenceScreen.addShared(shared: Category.Shared, selected: List<String>) {
        preferenceCategory("device_picker_shared_${shared.ownerName}") {
            title = if(shared.ownerName != null) {
                getString(R.string.tag_picker_category_shared, shared.ownerName)
            }else{
                getString(R.string.tag_picker_category_shared_unknown)
            }
            shared.items.forEach {
                val deviceId = it.deviceId
                val isSelected = selected.contains(deviceId)
                if(args.isMultiSelect) {
                    glideCheckBoxPreference {
                        title = it.label
                        url = it.icon
                        isChecked = isSelected
                        onChange<Boolean> { checked ->
                            viewModel.onSelectedChanged(deviceId, checked)
                        }
                    }
                }else{
                    glidePreference {
                        title = it.label
                        url = it.icon
                        summary = if (isSelected) {
                            getString(R.string.tag_picker_selected)
                        } else null
                        setSummaryAccented(isSelected)
                        onClick { onItemSelected(it.deviceId) }
                    }
                }
            }
        }
    }

    private fun onItemSelected(deviceId: String) {
        setFragmentResult(args.key, bundleOf(KEY_RESULT to deviceId))
        viewModel.close()
    }

    private fun onItemsSelected() {
        val selected = (viewModel.state.value as? State.Loaded)?.selected?.toTypedArray() ?: return
        setFragmentResult(args.key, bundleOf(KEY_RESULT to selected))
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tag_picker_error_dialog_title)
            setMessage(R.string.tag_picker_error_dialog_content)
            setPositiveButton(R.string.tag_picker_error_dialog_close) { _, _ ->
                viewModel.close()
            }
            setCancelable(false)
        }.show()
    }

    override fun onBackPressed(): Boolean {
        if(args.isMultiSelect) {
            onItemsSelected()
        }
        viewModel.close()
        return true
    }

}