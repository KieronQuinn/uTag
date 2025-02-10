package com.kieronquinn.app.utag.ui.screens.decision

import com.kieronquinn.app.utag.databinding.FragmentDecisionBinding
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.ProvidesTitle

class DecisionFragment: BoundFragment<FragmentDecisionBinding>(FragmentDecisionBinding::inflate), ProvidesTitle {

    override fun getTitle(): CharSequence {
        return ""
    }

}