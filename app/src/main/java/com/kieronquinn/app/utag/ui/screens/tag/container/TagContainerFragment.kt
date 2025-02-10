package com.kieronquinn.app.utag.ui.screens.tag.container

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagContainerNavigation
import com.kieronquinn.app.utag.components.navigation.setupWithNavigation
import com.kieronquinn.app.utag.databinding.FragmentTagContainerBinding
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import org.koin.android.ext.android.inject

class TagContainerFragment: BoundFragment<FragmentTagContainerBinding>(FragmentTagContainerBinding::inflate) {

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_tag_container) as NavHostFragment
    }

    private val navigation by inject<TagContainerNavigation>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() = whenCreated {
        navHostFragment.setupWithNavigation(navigation)
    }

}