package com.kieronquinn.app.utag.ui.screens.widget.container

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.WidgetContainerNavigation
import com.kieronquinn.app.utag.components.navigation.setupWithNavigation
import com.kieronquinn.app.utag.databinding.FragmentWidgetContainerBinding
import com.kieronquinn.app.utag.ui.activities.BaseWidgetConfigurationActivity
import com.kieronquinn.app.utag.ui.activities.MainActivity
import com.kieronquinn.app.utag.ui.base.BaseContainerFragment
import com.kieronquinn.app.utag.ui.screens.widget.container.WidgetContainerViewModel.State
import com.kieronquinn.app.utag.utils.extensions.BiometricEvent
import com.kieronquinn.app.utag.utils.extensions.repeatWhenResumed
import com.kieronquinn.app.utag.utils.extensions.showBiometricPrompt
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WidgetContainerFragment: BaseContainerFragment<FragmentWidgetContainerBinding>(FragmentWidgetContainerBinding::inflate) {

    private val viewModel by viewModel<WidgetContainerViewModel> {
        parametersOf(BaseWidgetConfigurationActivity.getWidgetType(requireActivity()))
    }

    override val bottomNavigation = null
    override val bottomNavigationContainer = null

    override val fragment by lazy {
        binding.navHostFragmentWidget
    }

    override val headerView by lazy {
        binding.headerView
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_widget) as NavHostFragment
    }

    override val navigation by inject<WidgetContainerNavigation>()
    override val rootDestinationId = R.id.nav_graph_widget

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        setupState()
    }

    private fun setupNavigation() = whenCreated {
        navHostFragment.setupWithNavigation(navigation)
    }

    private fun setupState() = repeatWhenResumed {
        viewModel.state.collect {
            it.destination?.let { destination ->
                navigation.navigate(destination)
            }
            when (it) {
                is State.BiometricPrompt -> {
                    binding.root.isVisible = false
                    showBiometricPrompt()
                }
                is State.NotSetup -> {
                    binding.root.isVisible = false
                    showSetupDialog()
                }
                else -> {
                    binding.root.isVisible = true
                }
            }
        }
    }

    private fun showBiometricPrompt() = lifecycleScope.launch {
        showBiometricPrompt(
            R.string.biometric_prompt_title,
            R.string.biometric_prompt_content_widget,
            R.string.biometric_prompt_content_negative
        ).collect {
            when(it) {
                is BiometricEvent.Success -> viewModel.onBiometricSuccess()
                is BiometricEvent.Error -> showBiometricErrorDialog()
                is BiometricEvent.Failed -> {
                    //No-op
                }
            }
        }
    }

    private fun showBiometricErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.biometric_prompt_error_title)
            setMessage(getString(R.string.biometric_prompt_error_content))
            setCancelable(false)
            setPositiveButton(R.string.biometric_prompt_error_close) { _, _ ->
                requireActivity().finish()
            }
        }.show()
    }

    private fun showSetupDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.widget_dialog_not_setup_title)
            setMessage(getString(R.string.widget_dialog_not_setup_content))
            setCancelable(false)
            setPositiveButton(R.string.widget_dialog_not_setup_open) { _, _ ->
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }
            setNegativeButton(android.R.string.cancel) {_, _ ->
                requireActivity().finish()
            }
        }.show()
    }

}