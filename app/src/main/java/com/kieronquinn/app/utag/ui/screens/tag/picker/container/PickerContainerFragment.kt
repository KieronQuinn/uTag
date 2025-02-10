package com.kieronquinn.app.utag.ui.screens.tag.picker.container

import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.TagPickerNavigation
import com.kieronquinn.app.utag.databinding.FragmentPickerContainerBinding
import com.kieronquinn.app.utag.ui.base.BaseContainerFragment
import org.koin.android.ext.android.inject

class PickerContainerFragment: BaseContainerFragment<FragmentPickerContainerBinding>(FragmentPickerContainerBinding::inflate) {

    override val bottomNavigation = null
    override val bottomNavigationContainer = null

    override val fragment by lazy {
        binding.navHostFragmentPicker
    }

    override val headerView by lazy {
        binding.headerView
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_picker) as NavHostFragment
    }

    override val navigation by inject<TagPickerNavigation>()
    override val rootDestinationId = R.id.nav_graph_tag_picker

}