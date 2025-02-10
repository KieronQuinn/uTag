package com.kieronquinn.app.utag.ui.screens.tag.lostmode.guide

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentTagLostModeGuideBinding
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.ProvidesSubtitle
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LostModeGuideFragment: BoundFragment<FragmentTagLostModeGuideBinding>(FragmentTagLostModeGuideBinding::inflate), BackAvailable, ProvidesSubtitle {

    private val args by navArgs<LostModeGuideFragmentArgs>()

    private val viewModel by viewModel<LostModeGuideViewModel> {
        parametersOf(args.deviceId, args.deviceLabel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEmergency()
        setupNotify()
        setupParing()
        setupButtons()
    }

    override fun getSubtitle(): CharSequence {
        return args.deviceLabel
    }

    private fun setupEmergency() = with(binding.tagEmergency) {
        lostModeGuideIcon.setImageResource(R.drawable.ic_emergency)
        lostModeGuideTitle.setText(R.string.emergency_contact_and_message)
        lostModeGuideSummary.setText(R.string.emergency_contact_and_message_description)
        lostModeGuideSubSummary.isVisible = false
        lostModeGuideWarningSummary.isVisible = false
        lostModeWarningIcon.isVisible = false
    }

    private fun setupNotify() = with(binding.tagNotifyFound) {
        lostModeGuideIcon.setImageResource(R.drawable.ic_noti)
        lostModeGuideTitle.setText(R.string.notify_me_when_its_found)
        lostModeGuideSummary.setText(R.string.notify_found_description)
        lostModeGuideSubSummary.isVisible = false
        lostModeGuideWarningSummary.setText(R.string.device_is_already_connected_to_your_device)
        lostModeGuideWarningSummary.isVisible = args.isConnected
        lostModeWarningIcon.isVisible = args.isConnected
    }

    private fun setupParing() = with(binding.tagPairingLock) {
        lostModeGuideIcon.setImageResource(R.drawable.ic_lock)
        lostModeGuideTitle.setText(R.string.pairing_lock)
        lostModeGuideSummary.setText(R.string.pairing_lock_description)
        lostModeGuideSubSummary.isVisible = false
        lostModeGuideWarningSummary.isVisible = false
        lostModeWarningIcon.isVisible = false
    }

    private fun setupButtons() = with(binding.bottomNavigation) {
        whenResumed {
            binding.lostModeGuideCancel.onClicked {
                viewModel.onCancelClicked()
            }
        }
        whenResumed {
            binding.lostModeGuideNext.onClicked {
                viewModel.onNextClicked()
            }
        }
        onApplyInsets { view, insets ->
            val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
            val inset = insets.getInsets(SYSTEM_INSETS)
            view.updateLayoutParams<MarginLayoutParams> {
                updateMargins(bottom = inset.bottom + padding)
            }
        }
    }

}