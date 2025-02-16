package com.kieronquinn.app.utag.ui.screens.tag.root

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagRootNavigation
import com.kieronquinn.app.utag.components.navigation.setupWithNavigation
import com.kieronquinn.app.utag.databinding.FragmentTagRootBinding
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.screens.tag.root.TagRootViewModel.State
import com.kieronquinn.app.utag.utils.extensions.BiometricEvent
import com.kieronquinn.app.utag.utils.extensions.repeatWhenResumed
import com.kieronquinn.app.utag.utils.extensions.showBiometricPrompt
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TagRootFragment: BoundFragment<FragmentTagRootBinding>(FragmentTagRootBinding::inflate) {

    private val viewModel by viewModel<TagRootViewModel>()

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_tag_root) as NavHostFragment
    }

    private val navigation by inject<TagRootNavigation>()

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
            if(it is State.BiometricPrompt) {
                binding.tagOverlay.isVisible = true
                showBiometricPrompt()
            }else{
                binding.tagOverlay.isVisible = false
            }
        }
    }

    private fun showBiometricPrompt() = lifecycleScope.launch {
        showBiometricPrompt(
            R.string.biometric_prompt_title,
            R.string.biometric_prompt_content_map,
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