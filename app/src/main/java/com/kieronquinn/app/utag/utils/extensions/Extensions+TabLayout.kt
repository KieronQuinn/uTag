package com.kieronquinn.app.utag.utils.extensions

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab

fun TabLayout.addOnTabSelectedListener(block: Tab.() -> Unit) {
    addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: Tab) {
            block(tab)
        }

        override fun onTabUnselected(tab: Tab?) {

        }

        override fun onTabReselected(tab: Tab?) {

        }
    })
}

fun TabLayout.selectTab(id: Int) {
    for(i in 0 until tabCount) {
        val tab = getTabAt(i) ?: continue
        if(tab.id == id) {
            selectTab(tab)
            return
        }
    }
}