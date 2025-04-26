package com.kieronquinn.app.utag.utils.extensions

import android.content.res.Resources
import android.util.LayoutDirection
import androidx.core.text.layoutDirection
import java.util.Locale

fun isRtl() = Locale.getDefault().layoutDirection == LayoutDirection.RTL

/**
 *  Gets the default Locale that has a country attached. This ensures that [Locale.getCountry]
 *  always returns something, and is retrieved in the order [Locale.getDefault] ->
 *  [Resources.getSystem] locale -> [Locale.US]
 */
fun Locale_getDefaultWithCountry(): Locale {
    val default = Locale.getDefault()
    if(default.country.isNotEmpty()) return default
    val system = Resources.getSystem().configuration.locales.get(0)
    if(default.country.isNotEmpty()) return system
    return Locale.US
}

private val localeMap by lazy {
    Locale.getISOCountries().associate { country ->
        val locale = Locale("", country)
        Pair(locale.isO3Country, locale.country)
    }
}

fun String.iso3Toiso2Country(): String? {
    return localeMap[this]
}