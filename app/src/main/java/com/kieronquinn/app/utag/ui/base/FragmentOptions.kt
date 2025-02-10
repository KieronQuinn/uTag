package com.kieronquinn.app.utag.ui.base

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

interface BackAvailable {
    val backIcon: Int
        get() = dev.oneuiproject.oneui.R.drawable.ic_oui_sysbar_back
}

interface LockCollapsed
interface StartExpanded

interface Root {
    fun isRoot() = true
}

interface CanShowSnackbar {
    fun setSnackbarVisible(visible: Boolean){
        //No-op by default
    }
}

interface CanShowBottomNavigation

interface ProvidesBack {
    fun onBackPressed(): Boolean
    fun interceptBack() = true
}

interface ProvidesTitle {
    fun getTitle(): CharSequence?
}

interface ProvidesSubtitle {
    fun getSubtitle(): CharSequence?
}

interface HideBottomNavigation {
    fun shouldHideBottomNavigation(): Boolean = true
}

interface ProvidesOverflow {
    fun inflateMenu(menuInflater: MenuInflater, menu: Menu)
    fun onMenuItemSelected(menuItem: MenuItem): Boolean
}