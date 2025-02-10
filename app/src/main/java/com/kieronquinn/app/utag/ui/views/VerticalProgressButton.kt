package com.kieronquinn.app.utag.ui.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.kieronquinn.app.utag.databinding.ButtonProgressVerticalBinding
import com.kieronquinn.app.utag.ui.views.base.BoundView
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded

class VerticalProgressButton: BoundView<ButtonProgressVerticalBinding> {

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
    ) = ButtonProgressVerticalBinding.inflate(inflater, viewGroup, attach)

    var text: CharSequence?
        get() = binding.buttonProgressVerticalText.text
        set(value) {
            binding.buttonProgressVerticalText.text = value
        }

    var icon: Drawable?
        get() = binding.buttonProgressVerticalIcon.drawable
        set(value) {
            binding.buttonProgressVerticalIcon.setImageDrawable(value)
        }

    var showProgress: Boolean
        get() = binding.buttonProgressFlipper.displayedChild == 1
        set(value) {
            binding.buttonProgressFlipper.setDisplayedChildIfNeeded(if(value) 1 else 0)
        }

    fun setIconResource(@DrawableRes id: Int) {
        icon = context.getDrawable(id)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        return binding.root.setOnClickListener(l)
    }

}