package com.kieronquinn.app.utag.utils.extensions

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan

fun SpannableStringBuilder.appendBullet() {
    append(
        " ",
        BulletSpan(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}