package com.kieronquinn.app.utag.utils.extensions

import androidx.annotation.ColorInt
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath

fun LottieAnimationView.replaceColour(
    vararg keyPath: String,
    @ColorInt replaceWith: Int,
    property: Int = LottieProperty.COLOR
) {
    addValueCallback(KeyPath(*keyPath), property) {
        replaceWith
    }
}