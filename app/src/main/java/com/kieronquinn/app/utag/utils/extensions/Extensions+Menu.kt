package com.kieronquinn.app.utag.utils.extensions

import android.view.Menu

fun Menu.setVisible(visible: Boolean) {
    for(i in 0 until size()) {
        getItem(i).isVisible = visible
    }
}