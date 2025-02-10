package com.kieronquinn.app.utag.utils.extensions

import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.layout.DrawerLayout

/**
 *  Disables the drawer and sets the back button up as required. This removes 90% of the
 *  functionality of the View, but there is seemingly no other way to get the expanding title bar.
 */
fun DrawerLayout.setupAsToolbar(backEnabled: Boolean) {
    setLocked(LOCK_MODE_LOCKED_CLOSED)
    if(backEnabled) {
        setNavigationButtonAsBack()
    }else{
        setNavigationButtonIcon(null)
        setNavigationButtonOnClickListener {
            //No-op
        }
    }
}

fun DrawerLayout.setLocked(state: Int) {
    findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerlayout_drawer)
        .setDrawerLockMode(state)
}