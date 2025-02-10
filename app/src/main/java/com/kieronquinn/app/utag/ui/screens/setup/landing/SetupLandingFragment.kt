package com.kieronquinn.app.utag.ui.screens.setup.landing

import android.os.Bundle
import android.view.View
import com.kieronquinn.app.utag.databinding.FragmentLandingBinding
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.LockCollapsed
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.ui.base.Root
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupLandingFragment: BoundFragment<FragmentLandingBinding>(FragmentLandingBinding::inflate), Root, LockCollapsed, ProvidesTitle {

    private val viewModel by viewModel<SetupLandingViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGetStarted()
    }

    private fun setupGetStarted() = with(binding.landingGetStarted) {
        whenResumed {
            onClicked {
                viewModel.onGetStartedClicked()
            }
        }
    }

    override fun getTitle() = ""

}