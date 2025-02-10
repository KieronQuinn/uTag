package com.kieronquinn.app.utag.ui.screens.tag.landing

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.kieronquinn.app.utag.databinding.FragmentTagLandingBinding
import com.kieronquinn.app.utag.ui.activities.MainActivity
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.whenResumed

class TagLandingFragment: BoundFragment<FragmentTagLandingBinding>(FragmentTagLandingBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOpen()
    }

    private fun setupOpen() = with(binding.landingOpen) {
        whenResumed {
            onClicked {
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }
        }
    }

}