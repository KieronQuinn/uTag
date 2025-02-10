package com.kieronquinn.app.utag.ui.screens.tag.more.battery

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentTagMoreBatteryDialogBinding
import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.ui.base.BaseDialogFragment
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.whenResumed

class TagMoreBatteryDialogFragment: BaseDialogFragment<FragmentTagMoreBatteryDialogBinding>(FragmentTagMoreBatteryDialogBinding::inflate) {

    private val args by navArgs<TagMoreBatteryDialogFragmentArgs>()

    private val accentColour by lazy {
        ContextCompat.getColor(requireContext(), R.color.oui_accent_color)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupContent()
        setupOk()
    }

    private fun setupContent() {
        when(args.level) {
            BatteryLevel.FULL, BatteryLevel.MEDIUM -> {
                binding.tagMoreBatteryDialogBull1.setTextColor(accentColour)
            }
            BatteryLevel.LOW -> {
                binding.tagMoreBatteryDialogBull2.setTextColor(accentColour)
            }
            BatteryLevel.VERY_LOW -> {
                binding.tagMoreBatteryDialogBull3.setTextColor(accentColour)
            }
            BatteryLevel.UNKNOWN -> {
                //No-op
            }
        }
    }

    private fun setupOk() = with(binding.tagMoreBatteryDialogOk) {
        whenResumed {
            onClicked {
                dismiss()
            }
        }
    }

}