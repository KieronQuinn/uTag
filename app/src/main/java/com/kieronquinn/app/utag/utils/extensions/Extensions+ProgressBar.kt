package com.kieronquinn.app.utag.utils.extensions

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.appcompat.widget.SeslProgressBar

fun SeslProgressBar.animateToProgress(progress: Int) {
    (tag as? Animator)?.cancel()
    tag = ValueAnimator.ofInt(this.progress, progress).apply {
        addUpdateListener {
            this@animateToProgress.progress = it.animatedValue as Int
            invalidate()
        }
        start()
    }
}