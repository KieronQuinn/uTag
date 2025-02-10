package com.kieronquinn.app.utag.utils.extensions

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.kieronquinn.app.utag.R

fun Snackbar.applyTheme() {
    val actionTv: TextView = view.findViewById(com.google.android.material.R.id.snackbar_action)
    actionTv.setTextColor(ContextCompat.getColor(context, R.color.sesl_primary_color_dark))
    val layout = view.findViewById<LinearLayout>(com.google.android.material.R.id.snackbar_layout)
    val colour = ContextCompat.getColor(context, R.color.oui_surface_color_variant_dark)
    layout.backgroundTintList = ColorStateList.valueOf(colour)
}

fun Snackbar.onLongClicked(block: () -> Unit) {
    val layout = view.findViewById<LinearLayout>(com.google.android.material.R.id.snackbar_layout)
    layout.setOnLongClickListener {
        block()
        true
    }
}