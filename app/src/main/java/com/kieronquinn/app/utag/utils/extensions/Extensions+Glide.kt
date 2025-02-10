package com.kieronquinn.app.utag.utils.extensions

import android.graphics.drawable.Drawable
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

fun RequestManager.url(url: String) = asDrawable().url(url)

fun <T> RequestBuilder<T>.url(url: String): RequestBuilder<T> {
    val glideUrl = GlideUrl(url, LazyHeaders.Builder().addHeader("Accept", "*/*").build())
    return load(glideUrl)
}

fun <T: Drawable> RequestBuilder<T>.fade(): RequestBuilder<T> {
    return transition(DrawableTransitionOptions.withCrossFade())
}