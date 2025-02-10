package com.kieronquinn.app.utag.ui.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.utils.appbar.DragOptionalAppBarLayoutBehaviour
import com.kieronquinn.app.utag.utils.extensions.setupAsToolbar
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.design.R as OR

/**
 *  [DrawerLayout] which behaves just as a Toolbar - no drawer will be shown. The navigation button
 *  can either be back or not there at all depending on the value of
 *  [R.styleable.HeaderView_back_enabled]
 */
class HeaderView: DrawerLayout {

    private val behaviour = DragOptionalAppBarLayoutBehaviour()

    private val samsungSans = ResourcesCompat.getFont(context, R.font.oneui_sans)

    constructor(context: Context, attributeSet: AttributeSet? = null): super(context, attributeSet) {
        val attributes = attributeSet?.let {
            context.obtainStyledAttributes(attributeSet, R.styleable.HeaderView)
        }
        val backEnabled = attributes?.getBoolean(R.styleable.HeaderView_back_enabled, false)
            ?: false
        setupAsToolbar(backEnabled)
        appBarLayout.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            behavior = this@HeaderView.behaviour
        }
        findViewById<View>(OR.id.toolbarlayout_bottom_corners)
            .isVisible = false
        val hideExpandedTitle = attributes
            ?.getBoolean(R.styleable.HeaderView_hide_expanded_title, false) == true
        findViewById<CollapsingToolbarLayout>(OR.id.toolbarlayout_collapsing_toolbar)
            .setExpandedTitleTypeface(samsungSans)
        if(hideExpandedTitle) {
            findViewById<CollapsingToolbarLayout>(OR.id.toolbarlayout_collapsing_toolbar)
                .setExpandedTitleColor(Color.TRANSPARENT)
        }
        attributes?.recycle()
    }

    constructor(context: Context): this(context, null)

}