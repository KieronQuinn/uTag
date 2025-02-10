package com.kieronquinn.app.utag.ui.screens.updates

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.ItemSettingsAboutBinding
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.base.CanShowBottomNavigation
import com.kieronquinn.app.utag.ui.base.Root
import com.kieronquinn.app.utag.ui.screens.updates.UpdatesViewModel.SmartThingsState
import com.kieronquinn.app.utag.ui.screens.updates.UpdatesViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.layoutPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import com.kieronquinn.app.utag.utils.preferences.tipsCardPreference
import dev.oneuiproject.oneui.widget.Toast
import org.koin.androidx.viewmodel.ext.android.viewModel

class UpdatesFragment: BaseSettingsFragment(), Root, CanShowBottomNavigation {

    private val viewModel by viewModel<UpdatesViewModel>()

    private val bottomNavPadding by lazy {
        resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
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
            is State.Loaded -> handleContent(state)
        }
    }

    private fun handleContent(state: State.Loaded) = setPreferences(bottomNavPadding) {
        if(state.uTagUpdate != null) {
            actionCardPreference {
                title = getString(R.string.updates_utag_available_title)
                summary = getString(
                    R.string.updates_utag_available_content, state.uTagUpdate.tag, state.uTagVersion
                )
                addButton(getString(R.string.updates_utag_available_action)) {
                    viewModel.onUTagUpdateClicked()
                }
            }
        }else{
            tipsCardPreference {
                title = getString(R.string.updates_utag_updated_title)
                summary = getString(R.string.updates_utag_updated_content, state.uTagVersion)
            }
        }
        when {
            state.smartThingsState == SmartThingsState.MODDED && state.smartThingsUpdate != null -> {
                //Update that we can handle is available
                actionCardPreference {
                    title = getString(R.string.updates_smartthings_available_title)
                    summary = getString(
                        R.string.updates_smartthings_available_content,
                        state.smartThingsUpdate.version,
                        state.smartThingsVersion
                    )
                    addButton(getString(R.string.updates_smartthings_available_action)) {
                        viewModel.onSmartThingsUpdateClicked()
                    }
                }
            }
            state.smartThingsState == SmartThingsState.MODDED -> {
                actionCardPreference {
                    title = getString(R.string.updates_smartthings_updated_title)
                    summary = getString(
                        R.string.updates_smartthings_updated_content,
                        state.smartThingsVersion
                    )
                }
            }
            state.smartThingsState == null -> {
                actionCardPreference {
                    title = getString(R.string.updates_smartthings_not_installed_title)
                    summary = getString(R.string.updates_smartthings_not_installed_content)
                    addButton(getString(R.string.updates_smartthings_not_installed_action)) {
                        onRerunSetupClicked()
                    }
                }
            }
            //Vanilla version is installed if version is null
            state.smartThingsState == SmartThingsState.EXTERNAL && state.smartThingsVersion == null -> {
                actionCardPreference {
                    title = getString(R.string.updates_smartthings_not_modded_title)
                    summary = getText(R.string.updates_smartthings_not_modded_content)
                    addButton(getString(R.string.updates_smartthings_not_modded_action)) {
                        onRerunSetupClicked()
                    }
                }
            }
            state.smartThingsState == SmartThingsState.EXTERNAL -> {
                tipsCardPreference {
                    title = getString(R.string.updates_smartthings_external_title)
                    summary = getString(R.string.updates_smartthings_external_content)
                }
            }
            state.smartThingsState == SmartThingsState.PLAY -> {
                tipsCardPreference {
                    title = getString(R.string.updates_smartthings_external_title)
                    summary = getString(R.string.updates_smartthings_external_content_play)
                }
            }
        }
        preferenceCategory("updates_options") {
            title = getString(R.string.updates_category_options)
            switchPreference {
                title = getString(R.string.updates_check_for_updates_title)
                summary = getString(R.string.updates_check_for_updates_content)
                isChecked = state.autoUpdatesEnabled
                onChange<Boolean> {
                    viewModel.onAutoUpdatesChanged(it)
                }
            }
        }
        preferenceCategory("updates_about") {
            title = ""
            layoutPreference(createAboutView(), "updates_about_card")
        }
    }

    private fun createAboutView(): View {
        val binding = ItemSettingsAboutBinding
            .inflate(layoutInflater, null, false)
        return binding.apply {
            itemAboutContent.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
            itemAboutContent.setOnLongClickListener {
                val result = viewModel.onDebugModeToggled()
                if(result != null) {
                    val message = if(result) {
                        R.string.about_debug_enabled
                    }else{
                        R.string.about_debug_disabled
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
                result != null
            }
            itemAboutContributors.setOnClickListener {
                viewModel.onContributorsClicked()
            }
            itemAboutDonate.setOnClickListener {
                viewModel.onDonateClicked()
            }
            itemAboutGithub.setOnClickListener {
                viewModel.onGitHubClicked()
            }
            itemAboutCrowdin.setOnClickListener {
                viewModel.onCrowdinClicked()
            }
            itemAboutLibraries.setOnClickListener {
                viewModel.onLibrariesClicked()
            }
            itemAboutBluesky.setOnClickListener {
                viewModel.onBlueskyClicked()
            }
            itemAboutXda.setOnClickListener {
                viewModel.onXdaClicked()
            }
        }.root
    }

    private fun onRerunSetupClicked() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.updates_rerun_setup_dialog_title)
            setMessage(R.string.updates_rerun_setup_dialog_content)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                viewModel.onRerunSetupClicked()
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

}