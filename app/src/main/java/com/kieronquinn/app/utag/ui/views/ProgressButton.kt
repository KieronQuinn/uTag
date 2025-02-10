package com.kieronquinn.app.utag.ui.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kieronquinn.app.utag.databinding.ButtonProgressBinding
import com.kieronquinn.app.utag.ui.views.base.BoundView
import com.kieronquinn.app.utag.utils.extensions.onAccent
import com.kieronquinn.app.utag.utils.extensions.onAccentInverse

class ProgressButton: BoundView<ButtonProgressBinding> {

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
    ) = ButtonProgressBinding.inflate(inflater, viewGroup, attach).apply {
        buttonProgressButton.setTextColor(context.onAccent)
    }

    var text: CharSequence?
        get() = binding.buttonProgressButton.text
        set(value) {
            binding.buttonProgressButton.text = value
        }

    var showProgress: Boolean
        get() = binding.buttonProgressProgress.isVisible
        set(value) {
            binding.buttonProgressProgress.isVisible = value
            binding.buttonProgressButton.isEnabled = !value
            binding.buttonProgressButton.setTextColor(
                if(value) Color.TRANSPARENT else context.onAccentInverse
            )
        }

    override fun setOnClickListener(l: OnClickListener?) {
        return binding.buttonProgressButton.setOnClickListener(l)
    }

}