package com.kieronquinn.app.utag.ui.screens.tag.picker

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.PreferenceScreen
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesBack
import com.kieronquinn.app.utag.ui.screens.tag.picker.TagDevicePickerViewModel.Category
import com.kieronquinn.app.utag.ui.screens.tag.picker.TagDevicePickerViewModel.State
import com.kieronquinn.app.utag.ui.screens.tag.picker.container.PickerContainerFragmentArgs
import com.kieronquinn.app.utag.utils.extensions.containerParent
import com.kieronquinn.app.utag.utils.extensions.containerParentNavArgs
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.glidePreference
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.setSummaryAccented
import com.kieronquinn.app.utag.xposed.extensions.TagActivity_createIntent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TagDevicePickerFragment: BaseSettingsFragment(), BackAvailable, ProvidesBack {

    companion object {
        private const val KEY_TAG_PICKER = "tag_picker"
        private const val KEY_RESULT = "result"

        fun Fragment.setupTagPickerListener(callback: (result: String) -> Unit) {
            setFragmentResultListener(KEY_TAG_PICKER) { requestKey, bundle ->
                if(requestKey != KEY_TAG_PICKER) return@setFragmentResultListener
                val result = bundle.getString(KEY_RESULT)
                callback.invoke(result ?: return@setFragmentResultListener)
            }
        }
    }

    private val args by containerParentNavArgs<PickerContainerFragmentArgs>()

    private val viewModel by viewModel<TagDevicePickerViewModel> {
        parametersOf(args.selectedDeviceId, args.knownDeviceIds)
    }

    private val shortcutManager by lazy {
        requireContext().getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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
        if(state.favouritesAvailable) {
            actionCardPreference {
                title = getString(R.string.tag_picker_favourites_tip_title)
                summary = getString(R.string.tag_picker_favourites_tip_content)
                addButton(getString(R.string.tag_picker_favourites_tip_action)) {
                    viewModel.onFavouritesClicked()
                }
            }
        }
        state.categories.forEach {
            when(it) {
                is Category.Favourites -> addFavourites(it, state.selected)
                is Category.Mine -> addMine(it, state.selected)
                is Category.Shared -> addShared(it, state.selected)
            }
        }
    }

    private fun PreferenceScreen.addFavourites(favourites: Category.Favourites, selected: String) {
        if(favourites.items.isEmpty()) return
        preferenceCategory("device_picker_favourites") {
            title = getString(R.string.tag_picker_category_favourites)
            favourites.items.forEach {
                val isSelected = it.deviceId == selected
                glidePreference {
                    title = it.label
                    url = it.icon
                    summary = when {
                        isSelected && it.isDifferentOwner -> {
                            getString(R.string.tag_picker_shared_and_selected, it.deviceOwner)
                        }
                        isSelected -> {
                            getString(R.string.tag_picker_selected)
                        }
                        it.isAgreementNeeded -> getString(R.string.tag_picker_agreement_required)
                        !it.isAllowed -> getString(R.string.tag_picker_not_allowed)
                        it.isDifferentOwner -> {
                            getString(R.string.tag_picker_shared, it.deviceOwner)
                        }
                        else -> null
                    }
                    isEnabled = it.isAllowed
                    setSummaryAccented(isSelected && it.isAllowed)
                    onClick { onItemSelected(it.deviceId) }
                    longClickListener = View.OnLongClickListener { _ ->
                        onItemLongClicked(
                            it.deviceId,
                            it.shortcutIcon ?: return@OnLongClickListener true,
                            it.label
                        )
                        true
                    }.takeIf { _ -> it.isShortcutAvailable && it.shortcutIcon != null }
                }
            }
        }
    }

    private fun PreferenceScreen.addMine(mine: Category.Mine, selected: String) {
        if(mine.items.isEmpty()) return
        preferenceCategory("device_picker_mine") {
            title = getString(R.string.tag_picker_category_mine)
            mine.items.forEach {
                val isSelected = it.deviceId == selected
                glidePreference {
                    title = it.label
                    url = it.icon
                    summary = when {
                        isSelected -> getString(R.string.tag_picker_selected)
                        it.isAgreementNeeded -> getString(R.string.tag_picker_agreement_required)
                        !it.isAllowed -> getString(R.string.tag_picker_not_allowed)
                        else -> null
                    }
                    isEnabled = it.isAllowed
                    setSummaryAccented(isSelected)
                    onClick { onItemSelected(it.deviceId) }
                    longClickListener = View.OnLongClickListener { _ ->
                        onItemLongClicked(
                            it.deviceId,
                            it.shortcutIcon ?: return@OnLongClickListener true,
                            it.label
                        )
                        true
                    }.takeIf { _ -> it.isShortcutAvailable && it.shortcutIcon != null }
                }
            }
        }
    }

    private fun PreferenceScreen.addShared(shared: Category.Shared, selected: String) {
        preferenceCategory("device_picker_shared_${shared.ownerName}") {
            title = if(shared.ownerName != null) {
                getString(R.string.tag_picker_category_shared, shared.ownerName)
            }else{
                getString(R.string.tag_picker_category_shared_unknown)
            }
            shared.items.forEach {
                val isSelected = it.deviceId == selected
                glidePreference {
                    title = it.label
                    url = it.icon
                    summary = when {
                        isSelected -> getString(R.string.tag_picker_selected)
                        it.isAgreementNeeded -> getString(R.string.tag_picker_agreement_required)
                        !it.isAllowed -> getString(R.string.tag_picker_not_allowed)
                        else -> null
                    }
                    isEnabled = it.isAllowed || it.isAgreementNeeded
                    setSummaryAccented(isSelected)
                    onClick { onItemSelected(it.deviceId) }
                    longClickListener = View.OnLongClickListener { _ ->
                        onItemLongClicked(
                            it.deviceId,
                            it.shortcutIcon ?: return@OnLongClickListener true,
                            it.label
                        )
                        true
                    }.takeIf { _ -> it.isShortcutAvailable && it.shortcutIcon != null }
                }
            }
        }
    }

    private fun onItemSelected(deviceId: String) {
        containerParent?.setFragmentResult(KEY_TAG_PICKER, bundleOf(KEY_RESULT to deviceId))
        viewModel.close()
    }

    private fun onItemLongClicked(deviceId: String, icon: Bitmap, label: String) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(label)
            setItems(arrayOf(getString(R.string.tag_picker_add_to_home))) { _, _ ->
                val shortcutInfo = ShortcutInfo.Builder(requireContext(), deviceId)
                    .setIntent(TagActivity_createIntent(deviceId))
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(Icon.createWithBitmap(icon)).build()
                shortcutManager.requestPinShortcut(shortcutInfo, null)
            }
        }.show()
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
        viewModel.close()
        return true
    }

}