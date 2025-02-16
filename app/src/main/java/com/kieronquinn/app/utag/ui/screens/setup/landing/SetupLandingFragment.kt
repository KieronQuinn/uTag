package com.kieronquinn.app.utag.ui.screens.setup.landing

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentLandingBinding
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.LockCollapsed
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.ui.base.Root
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.onLongClicked
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupLandingFragment: BoundFragment<FragmentLandingBinding>(FragmentLandingBinding::inflate), Root, LockCollapsed, ProvidesTitle {

    private val viewModel by viewModel<SetupLandingViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGetStarted()
        setupDebug()
    }

    private fun setupGetStarted() = with(binding.landingGetStarted) {
        whenResumed {
            onClicked {
                viewModel.onGetStartedClicked()
            }
        }
    }

    private fun setupDebug() = with(binding.landingDebug) {
        onApplyInsets { view, insets ->
            val inset = insets.getInsets(SYSTEM_INSETS)
            view.updateLayoutParams<FrameLayout.LayoutParams> {
                updateMargins(right = inset.right, bottom = inset.bottom)
            }
        }
        whenResumed {
            onLongClicked(true).collect {
                viewModel.onDebugLongClicked()
                Toast.makeText(
                    requireContext(), R.string.about_debug_enabled, Toast.LENGTH_LONG
                ).show()
                isVisible = false
            }
        }
    }

    override fun getTitle() = ""

}