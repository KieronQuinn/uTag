package com.kieronquinn.app.utag.ui.screens.settings.container

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.databinding.FragmentSettingsContainerBinding
import com.kieronquinn.app.utag.ui.base.BaseContainerFragment
import com.kieronquinn.app.utag.ui.base.CanShowBottomNavigation
import com.kieronquinn.app.utag.ui.base.CanShowSnackbar
import com.kieronquinn.app.utag.utils.extensions.applyTheme
import com.kieronquinn.app.utag.utils.extensions.getTopFragment
import com.kieronquinn.app.utag.utils.extensions.onLongClicked
import com.kieronquinn.app.utag.utils.extensions.selectTab
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsContainerFragment: BaseContainerFragment<FragmentSettingsContainerBinding>(FragmentSettingsContainerBinding::inflate) {

    override val fragment by lazy {
        binding.navHostFragment
    }

    override val headerView by lazy {
        binding.headerView
    }

    override val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    override val bottomNavigation by lazy {
        binding.containerBottomNavigation
    }

    override val bottomNavigationContainer by lazy {
        binding.containerBottomNavigationContainer
    }

    private val updateSnackbar by lazy {
        Snackbar.make(
            binding.root, getString(R.string.snackbar_update), Snackbar.LENGTH_INDEFINITE
        ).apply {
            applyTheme()
            setAction(R.string.snackbar_update_button){
                onSnackbarClicked()
            }
            onLongClicked {
                viewModel.onUpdateDismissed()
                navHostFragment.getTopFragment()?.updateSnackbarState(false)
            }
            fixMargins()
        }
    }
    override val navigation by inject<SettingsNavigation>()
    override val rootDestinationId = R.id.nav_graph_settings

    private val viewModel by viewModel<SettingsContainerViewModel>()
    private var areTabsSetup = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() = with(binding.containerBottomNavigation) {
        val selectedTab = viewModel.getSelectedTabId()
        addTab(newTab().apply {
            id = R.id.nav_graph_settings
            text = getString(R.string.bottom_nav_settings)
        })
        addTab(newTab().apply {
            id = R.id.nav_graph_updates
            text = getString(R.string.bottom_nav_updates)
        })
        whenResumed {
            areTabsSetup = true
            selectTab(selectedTab ?: return@whenResumed)
        }
        handleBottomNavigation(viewModel.showBottomNavigation.value)
        whenResumed {
            viewModel.showBottomNavigation.collect {
                handleBottomNavigation(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        setupUpdateSnackbar()
    }

    override fun onPause() {
        super.onPause()
        handleUpdateSnackbar(show = false, multiple = false)
    }

    override fun onTabSelected(id: Int) {
        if(!areTabsSetup) return
        viewModel.onTabSelected(id)
    }

    private fun setupUpdateSnackbar() {
        val value = viewModel.showUpdateSnackbar.value
        handleUpdateSnackbar(value.first, value.second)
        whenResumed {
            viewModel.showUpdateSnackbar.collect {
                handleUpdateSnackbar(it.first, it.second)
            }
        }
    }

    override fun onTopFragmentChanged(topFragment: Fragment, currentDestination: NavDestination) {
        super.onTopFragmentChanged(topFragment, currentDestination)
        viewModel.setCanShowSnackbar(topFragment is CanShowSnackbar)
        viewModel.setCanShowBottomNavigation(topFragment is CanShowBottomNavigation)
        topFragment.updateSnackbarState(updateSnackbar.isShown)
    }

    private fun handleUpdateSnackbar(show: Boolean, multiple: Boolean) {
        if(show) {
            //Set state, allow animation time and then show
            navHostFragment.getTopFragment()?.updateSnackbarState(true)
            if(multiple) {
                updateSnackbar.setText(R.string.snackbar_update_multiple)
            }else{
                updateSnackbar.setText(R.string.snackbar_update)
            }
            updateSnackbar.show()
        }else{
            //Dismiss, allow animation time and then update state
            updateSnackbar.dismiss()
            navHostFragment.getTopFragment()?.updateSnackbarState(false)
        }
    }

    private fun handleBottomNavigation(show: Boolean) {
        binding.containerBottomNavigationContainer.isVisible = show
    }

    private fun Snackbar.fixMargins() = with(view) {
        translationY = -resources.getDimension(R.dimen.margin_16)
    }

    private fun Fragment.updateSnackbarState(isVisible: Boolean) {
        (this as? CanShowSnackbar)?.setSnackbarVisible(isVisible)
    }

    private fun onSnackbarClicked() {
        binding.containerBottomNavigation.selectTab(R.id.nav_graph_updates)
    }

}