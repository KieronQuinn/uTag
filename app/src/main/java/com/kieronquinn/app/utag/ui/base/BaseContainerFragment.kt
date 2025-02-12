package com.kieronquinn.app.utag.ui.base

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.window.OnBackInvokedCallback
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.tabs.TabLayout
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.navigation.BaseNavigation
import com.kieronquinn.app.utag.components.navigation.setupWithNavigation
import com.kieronquinn.app.utag.ui.views.HeaderView
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.addOnTabSelectedListener
import com.kieronquinn.app.utag.utils.extensions.collapsedState
import com.kieronquinn.app.utag.utils.extensions.getRememberedAppBarCollapsed
import com.kieronquinn.app.utag.utils.extensions.getTopFragment
import com.kieronquinn.app.utag.utils.extensions.isLandscape
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onDestinationChanged
import com.kieronquinn.app.utag.utils.extensions.onNavigationIconClicked
import com.kieronquinn.app.utag.utils.extensions.rememberAppBarCollapsed
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import dev.oneuiproject.oneui.R as OneuiR

abstract class BaseContainerFragment<V: ViewBinding>(inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): BoundFragment<V>(inflate) {

    abstract val navigation: BaseNavigation
    abstract val bottomNavigation: TabLayout?
    abstract val bottomNavigationContainer: ViewGroup?
    abstract val headerView: HeaderView?
    abstract val fragment: FragmentContainerView
    abstract val navHostFragment: NavHostFragment

    open val handleInsets = true
    open val rootDestinationId: Int? = null

    private val navController by lazy {
        navHostFragment.navController
    }

    private var backDispatcherCallbackNative: Any? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStack()
        setupCollapsedState()
        setupNavigation()
        setupToolbar()
        setupBack()
        setupAppBar()
        setupInsets()
        setupBottomNavigation()
        bottomNavigation?.let {
            it.addOnTabSelectedListener {
                onTabSelected(id)
            }
        }
    }

    private fun setupBottomNavigation() = bottomNavigationContainer?.run {
        val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
        val paddingBottom = resources.getDimensionPixelSize(R.dimen.margin_8)
        onApplyInsets { view, insets ->
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            val bottomInsets = systemInsets.bottom
            val leftInsets = systemInsets.left
            val rightInsets = systemInsets.right
            view.updateLayoutParams<MarginLayoutParams> {
                updateMargins(
                    bottom = bottomInsets + paddingBottom,
                    left = padding + leftInsets,
                    right = padding + rightInsets
                )
            }
        }
    }

    open fun onTabSelected(id: Int) {
        //No-op by default
    }

    private fun setupStack() {
        whenResumed {
            navController.onDestinationChanged().collect {
                onTopFragmentChanged(
                    navHostFragment.getTopFragment() ?: return@collect, it
                )
            }
        }
        whenCreated {
            onTopFragmentChanged(
                navHostFragment.getTopFragment() ?: return@whenCreated,
                navController.currentDestination ?: return@whenCreated
            )
        }
    }

    private fun setupCollapsedState() = whenResumed {
        headerView?.appBarLayout?.collapsedState()?.collect {
            navHostFragment.getTopFragment()?.rememberAppBarCollapsed(it)
        }
    }

    private fun setupToolbar() = headerView?.run {
        whenResumed {
            onNavigationIconClicked().collect {
                (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@collect
                }
                (this@BaseContainerFragment as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@collect
                }
                if(!navController.popBackStack()) {
                    requireActivity().finish()
                }
            }
        }
    }

    /**
     *  The OneUI library seems to not support predictive back using the compat library. Use the
     *  native dispatcher where possible, and where predictive back is not used, we can use compat.
     */
    @SuppressLint("RestrictedApi")
    private fun setupBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupBackNative()
        }else{
            setupBackCompat()
        }
    }

    private fun setupBackCompat() = whenResumed {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBack()
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupBackNative() = whenResumed {
        val callback = OnBackInvokedCallback {
            onBack()
        }
        backDispatcherCallbackNative = callback
        navController.onDestinationChanged().collect {
            if(shouldBackDispatcherBeEnabled()) {
                requireActivity().onBackInvokedDispatcher
                    .registerOnBackInvokedCallback(100, callback)
            }else{
                requireActivity().onBackInvokedDispatcher
                    .unregisterOnBackInvokedCallback(callback)
            }
        }
    }

    override fun onDestroy() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Unregister the back callback if it exists
            (backDispatcherCallbackNative as? OnBackInvokedCallback)?.let {
                requireActivity().onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
                backDispatcherCallbackNative = null
            }
        }
        super.onDestroy()
    }

    private fun onBack() {
        (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
            if(it.onBackPressed()) return
        }
        if(!navController.navigateUp()) {
            try {
                requireActivity().finish()
            }catch (e: IllegalStateException) {
                //Activity is gone, do nothing
            }
        }
    }

    private fun getBackNavDestination(): NavDestination? {
        return navController.previousBackStackEntry?.destination
    }

    private fun getCurrentNavDestination(): NavDestination? {
        return navController.currentBackStackEntry?.destination
    }

    private fun shouldBackDispatcherBeEnabled(): Boolean {
        val top = navHostFragment.getTopFragment()
        return (top is ProvidesBack && top.interceptBack()) || (top !is Root || !top.isRoot())
    }

    private fun setupAppBar() = headerView?.appBarLayout?.run {
        addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            fragment.updatePadding(bottom = appBarLayout.totalScrollRange + verticalOffset)
        }
    }

    open fun onTopFragmentChanged(topFragment: Fragment, currentDestination: NavDestination){
        val backIcon = if(topFragment is BackAvailable || this is BackAvailable){
            val icon = (topFragment as? BackAvailable)?.backIcon
                ?: (this as? BackAvailable)?.backIcon ?: OneuiR.drawable.ic_oui_sysbar_back
            ContextCompat.getDrawable(requireContext(), icon)
        } else null
        if(topFragment is ProvidesOverflow){
            setupMenu(topFragment)
        }else{
            setupMenu(null)
        }
        if(topFragment is LockCollapsed || requireContext().isLandscape()) {
            headerView?.isExpanded = false
        }else {
            headerView?.isExpanded = !topFragment.getRememberedAppBarCollapsed(topFragment !is StartExpanded)
        }
        bottomNavigation?.let {
            it.isVisible = !(topFragment is HideBottomNavigation && topFragment.shouldHideBottomNavigation())
        }
        updateTitle(topFragment, currentDestination)
        headerView?.setNavigationButtonIcon(backIcon)
    }

    private fun updateTitle(fragment: Fragment, destination: NavDestination) {
        (fragment as? ProvidesTitle)?.let {
            val label = it.getTitle() ?: return@let
            headerView?.setTitle(label)
        } ?: run {
            val label = destination.label
            if(label.isNullOrBlank()) return@run
            headerView?.setTitle(label)
        }
        val subtitle = (fragment as? ProvidesSubtitle)?.getSubtitle()
        headerView?.setExpandedSubtitle(subtitle)
        headerView?.setCollapsedSubtitle(subtitle)
    }

    private fun setupNavigation() = whenCreated {
        navHostFragment.setupWithNavigation(navigation)
    }

    private fun setupMenu(menuProvider: ProvidesOverflow?){
        val menu = headerView?.toolbar?.menu ?: return
        val menuInflater = MenuInflater(requireContext())
        menu.clear()
        menuProvider?.inflateMenu(menuInflater, menu)
        headerView?.toolbar?.setOnMenuItemClickListener {
            menuProvider?.onMenuItemSelected(it) ?: false
        }
    }

    private fun setupInsets() = with(fragment) {
        if(!handleInsets) return@with
        onApplyInsets { view, insets ->
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            val leftInsets = systemInsets.left
            val rightInsets = systemInsets.right
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(left = leftInsets, right = rightInsets)
            }
            headerView?.toolbar?.updatePadding(left = leftInsets, right = rightInsets)
            headerView?.updatePadding(top = systemInsets.top)
        }
    }

}