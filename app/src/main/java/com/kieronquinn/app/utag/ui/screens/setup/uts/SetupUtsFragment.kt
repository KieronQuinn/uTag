package com.kieronquinn.app.utag.ui.screens.setup.chaser

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentSetupUtsBinding
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.ui.screens.setup.uts.SetupUtsViewModel
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupUtsFragment: BoundFragment<FragmentSetupUtsBinding>(FragmentSetupUtsBinding::inflate), BackAvailable, ProvidesTitle {

    private val viewModel by viewModel<SetupUtsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupButtons()
    }

    override fun getTitle() = ""

    private fun setupInsets() = with(binding) {
        val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
        root.onApplyInsets { _, insets ->
            setupUtsButtonsContainer.updatePadding(
                bottom = insets.getInsets(SYSTEM_INSETS).bottom + padding
            )
        }
    }

    private fun setupButtons() = with(binding) {
        whenResumed {
            setupUtsAgree.onClicked {
                viewModel.onAgreeClicked()
            }
        }
        whenResumed {
            setupUtsDisagree.onClicked {
                viewModel.onDisagreeClicked()
            }
        }
    }

}