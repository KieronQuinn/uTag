package com.kieronquinn.app.utag.utils.extensions

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.kieronquinn.app.utag.R

val SYSTEM_INSETS = setOf(
    WindowInsetsCompat.Type.systemBars(),
    WindowInsetsCompat.Type.ime(),
    WindowInsetsCompat.Type.statusBars(),
    WindowInsetsCompat.Type.displayCutout()
).or()

fun View.onApplyInsets(block: (view: View, insets: WindowInsetsCompat) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        block(view, insets)
        insets
    }
}

fun View.applyBottomNavigationInset(extraPadding: Float = 0f) {
    val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height).toInt()
    updatePadding(bottom = bottomNavHeight + extraPadding.toInt())
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                or WindowInsetsCompat.Type.ime()).bottom
        updatePadding(bottom = bottomInsets + bottomNavHeight + extraPadding.toInt())
    }
}

fun View.applyBottomNavigationMargin(extraPadding: Float = 0f) {
    val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height_margins).toInt()
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(bottom = bottomInsets + bottomNavHeight + extraPadding.toInt())
        }
    }
}

fun View.applyBottomNavigationMarginShort(extraPadding: Float = 0f) {
    val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height).toInt()
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(bottom = bottomInsets + bottomNavHeight + extraPadding.toInt())
        }
    }
}

fun Context.getLegacyWorkaroundNavBarHeight(): Int {
    val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else 0
}