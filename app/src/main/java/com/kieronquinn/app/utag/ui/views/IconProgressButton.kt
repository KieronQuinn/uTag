package com.kieronquinn.app.utag.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.ButtonProgressIconBinding
import com.kieronquinn.app.utag.ui.views.base.BoundView
import com.kieronquinn.app.utag.utils.extensions.isDarkMode
import com.kieronquinn.app.utag.utils.extensions.onAccentInverse

class IconProgressButton: BoundView<ButtonProgressIconBinding> {

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes)
    constructor(context: Context, attributeSet: AttributeSet?):
            this(context, attributeSet, 0)
    constructor(context: Context):
            this(context, null, 0)

    override fun inflate(
        inflater: LayoutInflater,
        viewGroup: ViewGroup?,
        attach: Boolean
    ) = ButtonProgressIconBinding.inflate(inflater, viewGroup, attach)

    var showProgress: Boolean
        get() = binding.buttonProgressIconProgress.isVisible
        set(value) {
            binding.buttonProgressIconProgress.isVisible = value
            binding.buttonProgressIconButton.setEnabledState(!value)
        }

    var buttonEnabled: Boolean
        get() = binding.buttonProgressIconButton.isEnabled
        set(value) {
            binding.buttonProgressIconButton.setEnabledState(value)
        }

    fun setIconResource(@DrawableRes drawableRes: Int) {
        binding.buttonProgressIconButton.setImageResource(drawableRes)
    }

    fun setIconDrawable(drawable: Drawable) {
        binding.buttonProgressIconButton.setImageDrawable(drawable)
    }

    fun setIconPadding(padding: Int) {
        binding.buttonProgressIconButton.setPadding(padding)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        return binding.buttonProgressIconButton.setOnClickListener(l)
    }

    private fun ImageButton.setEnabledState(enabled: Boolean) {
        val backgroundTint: Int
        val tint: Int
        if(enabled) {
            backgroundTint = ContextCompat.getColor(context, R.color.oui_accent_color)
            tint = context.onAccentInverse
            alpha = 1f
        }else{
            backgroundTint =
                ContextCompat.getColor(context, R.color.oui_accent_color_disabled)
            tint = if(context.isDarkMode) Color.WHITE else Color.BLACK
            alpha = 0.5f
        }
        isEnabled = enabled
        backgroundTintList = ColorStateList.valueOf(backgroundTint)
        imageTintList = ColorStateList.valueOf(tint)
    }

}