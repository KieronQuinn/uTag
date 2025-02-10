package com.kieronquinn.app.utag.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.utils.extensions.dip

class RssiProgressView: View {

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes)
    constructor(context: Context, attributeSet: AttributeSet?):
            this(context, attributeSet, 0)
    constructor(context: Context):
            this(context, null, 0)

    private var animation: ValueAnimator? = null
    private var progress: Float = 0f
    private val strokeWidth = resources.dip(16).toFloat()
    private val animationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)

    private val background = ContextCompat.getColor(context, R.color.oui_surface_color_variant)
    private val arc = ContextCompat.getColor(context, R.color.oui_accent_color)

    private val backgroundPaint = Paint().apply {
        strokeWidth = this@RssiProgressView.strokeWidth
        style = Paint.Style.STROKE
        color = background
        isAntiAlias = true
    }

    private val arcPaint = Paint().apply {
        strokeWidth = this@RssiProgressView.strokeWidth
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        color = arc
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val angle = (progress * 180f)
        val padding = strokeWidth / 2f
        canvas.drawArc(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding,
            0f,
            360f,
            false,
            backgroundPaint
        )
        canvas.drawArc(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding,
            90f,
            angle,
            false,
            arcPaint
        )
        canvas.drawArc(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding,
            90f,
            -angle,
            false,
            arcPaint
        )
    }

    @Synchronized
    fun setProgress(progress: Float) {
        if(this.progress == progress) return
        animation?.cancel()
        val current = this.progress
        animation = ValueAnimator.ofFloat(current, progress).apply {
            duration = animationDuration.toLong()
            addUpdateListener {
                this@RssiProgressView.progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    init {
        if(isInEditMode) {
            progress = 0.5f
        }
    }

}