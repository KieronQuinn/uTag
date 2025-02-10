package com.kieronquinn.app.utag.utils.extensions

import java.util.Locale

fun String.removeSuffixRecursively(suffix: String): String {
    var result = this
    while(result.endsWith(suffix)) {
        result = result.removeSuffix(suffix)
    }
    return result
}

fun String.capitalise(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}