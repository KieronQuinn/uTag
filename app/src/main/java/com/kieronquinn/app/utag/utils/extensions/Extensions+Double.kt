package com.kieronquinn.app.utag.utils.extensions

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun Double.round(decimalPlaces: Int): Double {
    val symbols = DecimalFormatSymbols(Locale.US)
    val format = DecimalFormat("#.${"#".repeat(decimalPlaces)}", symbols)
    format.roundingMode = RoundingMode.HALF_UP
    return format.format(this).toDouble()
}