package com.kieronquinn.app.utag.ui.screens.safearea.wifi

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.ExitBuffer
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesBack
import com.kieronquinn.app.utag.ui.base.ProvidesOverflow
import com.kieronquinn.app.utag.ui.screens.safearea.wifi.SafeAreaWiFiViewModel.Event
import com.kieronquinn.app.utag.ui.screens.safearea.wifi.SafeAreaWiFiViewModel.State
import com.kieronquinn.app.utag.utils.edittext.MACAddressTextWatcher
import com.kieronquinn.app.utag.utils.edittext.SSIDTextWatcher
import com.kieronquinn.app.utag.utils.extensions.setTextToEnd
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.dropDownPreference
import com.kieronquinn.app.utag.utils.preferences.editTextPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SafeAreaWiFiFragment: BaseSettingsFragment(), BackAvailable, ProvidesOverflow, ProvidesBack {

    private val args by navArgs<SafeAreaWiFiFragmentArgs>()

    private val viewModel by viewModel<SafeAreaWiFiViewModel> {
        parametersOf(args.isSettings, args.addingDeviceId, args.currentId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupEvents()
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_safe_area, menu)
        menu.findItem(R.id.safe_area_delete).isVisible = args.currentId.isNotEmpty()
        menu.findItem(R.id.safe_area_padding).isVisible = args.currentId.isEmpty()
        menu.findItem(R.id.safe_area_save).actionView
            ?.findViewById<Button>(R.id.action_layout_save_button)
            ?.setOnClickListener { viewModel.onSaveClicked() }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.safe_area_delete -> showDeleteDialog()
        }
        return true
    }

    override fun onBackPressed(): Boolean {
        return if(viewModel.hasChanges()) {
            showSaveDialog()
            true
        }else false
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
        editTextPreference {
            title = getString(R.string.safe_area_wifi_name_title)
            summary = state.name ?: getString(R.string.safe_area_wifi_name_content_empty)
            dialogTitle = getString(R.string.safe_area_wifi_name_title)
            dialogMessage = getString(R.string.safe_area_wifi_name_dialog_message)
            setOnBindEditTextListener {
                it.setTextToEnd(state.name)
                it.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }
            onChange<String> {
                viewModel.onNameChanged(it.trim())
            }
        }
        editTextPreference {
            title = getString(R.string.safe_area_wifi_ssid_title)
            summary = state.ssid ?: getString(R.string.safe_area_wifi_ssid_content_empty)
            dialogTitle = getString(R.string.safe_area_wifi_ssid_title)
            dialogMessage = getString(R.string.safe_area_wifi_ssid_dialog_message)
            setOnBindEditTextListener {
                it.setTextToEnd(state.ssid)
                it.addTextChangedListener(SSIDTextWatcher(it))
                it.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }
            onChange<String> {
                viewModel.onSSIDChanged(it.trim())
            }
        }
        switchPreference {
            title = getString(R.string.safe_area_wifi_mac_toggle_title_title)
            summary = getText(R.string.safe_area_wifi_mac_toggle_title_content)
            isChecked = state.useMac
            onChange<Boolean> {
                viewModel.onUseMacChanged(it)
            }
        }
        if(state.useMac) {
            editTextPreference {
                title = getString(R.string.safe_area_wifi_mac_title)
                summary = state.mac ?: getString(R.string.safe_area_wifi_mac_content_empty)
                dialogTitle = getString(R.string.safe_area_wifi_mac_title)
                dialogMessage = getString(R.string.safe_area_wifi_mac_dialog_message)
                setOnBindEditTextListener {
                    it.setTextToEnd(state.mac)
                    it.addTextChangedListener(MACAddressTextWatcher(it))
                    it.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                }
                onChange<String> {
                    viewModel.onMACChanged(it)
                }
            }
        }
        dropDownPreference {
            title = getString(R.string.safe_area_wifi_exit_buffer_title)
            summary = getString(
                R.string.safe_area_wifi_exit_buffer_content,
                getString(state.exitBuffer.label)
            )
            entries = ExitBuffer.entries.map { getString(it.label) }.toTypedArray()
            entryValues = ExitBuffer.entries.map { it.name }.toTypedArray()
            value = state.exitBuffer.name
            onChange<String> {
                viewModel.onExitBufferChanged(ExitBuffer.valueOf(it))
            }
        }
    }

    private fun setupEvents() = whenResumed {
        viewModel.events.collect {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.INVALID_SSID -> {
                Toast.makeText(
                    requireContext(), R.string.safe_area_wifi_invalid_ssid, Toast.LENGTH_LONG
                ).show()
            }
            Event.INVALID_MAC -> {
                Toast.makeText(
                    requireContext(), R.string.safe_area_wifi_invalid_mac, Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showSaveDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.safe_area_dialog_save_title)
            setMessage(R.string.safe_area_dialog_save_content)
            setPositiveButton(R.string.safe_area_dialog_save_save) { _, _ ->
                viewModel.onSaveClicked()
            }
            setNegativeButton(R.string.safe_area_dialog_save_dont_save) { _, _ ->
                viewModel.onCloseClicked()
            }
        }.show()
    }

    private fun showDeleteDialog() {
        val name = (viewModel.state.value as? State.Loaded)?.name ?: return
        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.safe_area_dialog_delete_title)
            setMessage(getString(R.string.safe_area_dialog_delete_content, name))
            setPositiveButton(R.string.safe_area_dialog_delete_delete) { _, _ ->
                viewModel.onDeleteClicked()
            }
            setNegativeButton(R.string.safe_area_dialog_delete_cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
        dialog.getButton(Dialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.negative_red))
    }

}