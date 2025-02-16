package com.kieronquinn.app.utag.ui.views.base

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding

abstract class BoundView<V: ViewBinding>: FrameLayout {

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes)
    constructor(context: Context, attributeSet: AttributeSet?):
            this(context, attributeSet, 0)
    constructor(context: Context):
            this(context, null, 0)

    abstract fun inflate(inflater: LayoutInflater, viewGroup: ViewGroup?, attach: Boolean): V

    private val layoutInflater = LayoutInflater.from(context)

    protected val binding by lazy {
        inflate(layoutInflater, this, false)
    }

    open fun V.setup() {}

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            removeAllViews()
            addView(binding.root)
            binding.setup()
        }catch (e: IllegalArgumentException) {
            //Detached, ignore
        }
    }

}