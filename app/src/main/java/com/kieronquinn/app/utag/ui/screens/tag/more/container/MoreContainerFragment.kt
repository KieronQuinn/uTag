package com.kieronquinn.app.utag.ui.screens.tag.more.container

import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.databinding.FragmentMoreContainerBinding
import com.kieronquinn.app.utag.ui.base.BaseContainerFragment
import org.koin.android.ext.android.inject

class MoreContainerFragment: BaseContainerFragment<FragmentMoreContainerBinding>(FragmentMoreContainerBinding::inflate) {

    override val bottomNavigation = null
    override val bottomNavigationContainer = null

    override val fragment by lazy {
        binding.navHostFragmentMore
    }

    override val headerView by lazy {
        binding.headerView
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_more) as NavHostFragment
    }

    override val navigation by inject<TagMoreNavigation>()
    override val rootDestinationId = R.id.nav_graph_tag_more

}