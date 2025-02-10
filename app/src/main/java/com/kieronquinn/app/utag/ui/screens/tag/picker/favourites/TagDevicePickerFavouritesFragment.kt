package com.kieronquinn.app.utag.ui.screens.tag.picker.favourites

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesOverflow
import com.kieronquinn.app.utag.ui.screens.tag.picker.favourites.TagDevicePickerFavouritesViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.picker.favourites.TagDevicePickerFavouritesViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.glideCheckBoxPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TagDevicePickerFavouritesFragment: BaseSettingsFragment(), BackAvailable, ProvidesOverflow {

    private val args by navArgs<TagDevicePickerFavouritesFragmentArgs>()

    private val viewModel by viewModel<TagDevicePickerFavouritesViewModel> {
        parametersOf(args.knownDeviceIds)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupEvents()
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_tag_device_picker, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.menu_tag_device_picker_all -> viewModel.onFavouriteAllClicked()
            R.id.menu_tag_device_picker_none -> viewModel.onUnfavouriteAllClicked()
        }
        return true
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

    private fun setupEvents() = whenResumed {
        viewModel.events.collect {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.ERROR -> {
                Toast.makeText(requireContext(), R.string.tag_picker_error_toast, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun setContent(state: State.Loaded) = setPreferences {
        state.items.forEach {
            glideCheckBoxPreference {
                title = it.label
                url = it.icon
                isChecked = it.isFavourite
                isEnabled = !state.isSending
                onChange<Boolean> { checked ->
                    viewModel.onFavouriteChanged(it.deviceId, checked)
                }
            }
        }
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

}