package com.kieronquinn.app.utag.ui.screens.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentAuthResponseBinding
import com.kieronquinn.app.utag.ui.activities.MainActivity
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.screens.login.AuthResponseViewModel.State
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthResponseFragment: BoundFragment<FragmentAuthResponseBinding>(FragmentAuthResponseBinding::inflate) {

    private val viewModel by viewModel<AuthResponseViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        viewModel.handleIntent(requireActivity().intent)
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
            is State.Error -> showErrorDialog()
            is State.Cancelled -> returnToMain()
            is State.Complete -> {
                viewModel.onSuccess()
                returnToMain()
            }
            else -> {
                //No-op, just show loading
            }
        }
    }

    private fun showErrorDialog() {
        binding.loading.isVisible = false
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.login_response_error_title)
            setMessage(R.string.login_response_error_content)
            setPositiveButton(R.string.login_response_error_close) { _, _ ->
                returnToMain()
            }
            setCancelable(false)
        }.show()
    }

    private fun returnToMain() {
        startActivity(Intent(requireContext(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        })
        requireActivity().finish()
    }

}