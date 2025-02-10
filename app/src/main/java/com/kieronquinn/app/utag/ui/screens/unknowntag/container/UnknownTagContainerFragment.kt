package com.kieronquinn.app.utag.ui.screens.unknowntag.container

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.UnknownTagNavigation
import com.kieronquinn.app.utag.components.navigation.setupWithNavigation
import com.kieronquinn.app.utag.databinding.FragmentUnknownTagContainerBinding
import com.kieronquinn.app.utag.ui.base.BaseContainerFragment
import com.kieronquinn.app.utag.ui.screens.unknowntag.container.UnknownTagContainerViewModel.State
import com.kieronquinn.app.utag.utils.extensions.BiometricEvent
import com.kieronquinn.app.utag.utils.extensions.showBiometricPrompt
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class UnknownTagContainerFragment: BaseContainerFragment<FragmentUnknownTagContainerBinding>(FragmentUnknownTagContainerBinding::inflate) {

    private val viewModel by viewModel<UnknownTagContainerViewModel>()

    override val bottomNavigation = null
    override val bottomNavigationContainer = null

    override val fragment by lazy {
        binding.navHostFragmentUnknownTag
    }

    override val headerView by lazy {
        binding.headerView
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_unknown_tag) as NavHostFragment
    }

    override val navigation by inject<UnknownTagNavigation>()
    override val rootDestinationId = R.id.nav_graph_include_unknown_tag

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        setupState()
    }

    private fun setupNavigation() = whenCreated {
        navHostFragment.setupWithNavigation(navigation)
    }

    private fun setupState() = whenResumed {
        viewModel.state.collect {
            it.destination?.let { destination ->
                navigation.navigate(destination)
            }
            when (it) {
                is State.BiometricPrompt -> {
                    binding.root.isVisible = false
                    showBiometricPrompt()
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
            R.string.biometric_prompt_content_uts,
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

}