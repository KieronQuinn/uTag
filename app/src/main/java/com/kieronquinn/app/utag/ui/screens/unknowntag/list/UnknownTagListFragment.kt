package com.kieronquinn.app.utag.ui.screens.unknowntag.list

import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.activities.UnknownTagActivity
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.ProvidesOverflow
import com.kieronquinn.app.utag.ui.base.Root
import com.kieronquinn.app.utag.ui.screens.unknowntag.list.UnknownTagListViewModel.State
import com.kieronquinn.app.utag.utils.extensions.formatDateTime
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.glidePreference
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class UnknownTagListFragment: BaseSettingsFragment(), BackAvailable, Root, ProvidesOverflow {

    private val viewModel by viewModel<UnknownTagListViewModel> {
        parametersOf(isStandalone)
    }

    private val dateFormat by lazy {
        DateFormat.getDateFormat(requireContext())
    }

    private val timeFormat by lazy {
        DateFormat.getTimeFormat(requireContext())
    }

    private val isStandalone by lazy {
        UnknownTagActivity.isStandalone(this)
    }

    override val backIcon: Int
        get() = if(isStandalone) {
            dev.oneuiproject.oneui.R.drawable.ic_oui_close
        }else{
            dev.oneuiproject.oneui.R.drawable.ic_oui_back
        }

    override fun isRoot(): Boolean {
        return isStandalone
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_unknown_tag_list, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.menu_unknown_tag_list_safe -> showClearSafeDialog()
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
            is State.Loaded -> handleLoaded(state)
            is State.Empty -> handleEmpty()
        }
    }

    private fun handleLoaded(state: State.Loaded) = setPreferences {
        tipsCardPreference {
            title = getString(R.string.settings_uts_list_info_title)
            summary = getString(R.string.settings_uts_list_info_content)
        }
        preferenceCategory("uts_list") {
            val now = LocalDate.now()
            state.items.forEach {
                val time = Instant.ofEpochMilli(it.detections.last().timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime()
                val serviceData = it.detections.first().serviceData.encodedServiceData
                val imageUrl = state.tagImages[serviceData]
                val summary = if(time.toLocalDate() == now) {
                    getString(
                        R.string.settings_uts_list_unknown_tag_content_at,
                        timeFormat.formatDateTime(time)
                    )
                }else{
                    getString(
                        R.string.settings_uts_list_unknown_tag_content,
                        dateFormat.formatDateTime(time)
                    )
                }
                if(imageUrl != null) {
                    glidePreference {
                        url = imageUrl
                        title =
                            getString(R.string.settings_uts_list_unknown_tag_title, it.privacyId)
                        this.summary = summary
                        onClick {
                            viewModel.onUnknownTagClicked(it)
                        }
                    }
                }else{
                    preference {
                        title =
                            getString(R.string.settings_uts_list_unknown_tag_title, it.privacyId)
                        this.summary = summary
                        onClick {
                            viewModel.onUnknownTagClicked(it)
                        }
                    }
                }
            }
        }
    }

    private fun handleEmpty() = setPreferences {
        tipsCardPreference {
            title = getString(R.string.settings_uts_list_empty_title)
            summary = getString(R.string.settings_uts_list_empty_content)
        }
    }

    private fun showClearSafeDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.settings_uts_list_tag_clear_safe_dialog_title)
            setMessage(getText(R.string.settings_uts_list_tag_clear_safe_dialog_content))
            setPositiveButton(R.string.settings_uts_list_tag_clear_safe_dialog_positive) { dialog, _ ->
                dialog.dismiss()
                viewModel.onResetSafeTagsClicked()
            }
            setNegativeButton(R.string.settings_uts_list_tag_clear_safe_dialog_negative) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

}