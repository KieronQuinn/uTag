package com.kieronquinn.app.utag.ui.screens.setup.container

import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.databinding.FragmentSetupContainerBinding
import com.kieronquinn.app.utag.ui.base.BaseContainerFragment
import org.koin.android.ext.android.inject

class SetupContainerFragment: BaseContainerFragment<FragmentSetupContainerBinding>(FragmentSetupContainerBinding::inflate) {

    override val bottomNavigation = null
    override val bottomNavigationContainer = null

    override val fragment by lazy {
        binding.navHostFragment
    }

    override val headerView by lazy {
        binding.headerView
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    override val navigation by inject<SetupNavigation>()
    override val rootDestinationId = R.id.nav_graph_settings

}