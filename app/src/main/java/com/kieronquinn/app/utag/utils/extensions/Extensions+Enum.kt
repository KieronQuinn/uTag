package com.kieronquinn.app.utag.utils.extensions

import kotlin.enums.enumEntries

inline fun <reified E : Enum<E>> E.next(): E? {
    return enumEntries<E>().getOrNull(ordinal + 1)
}

inline fun <reified E : Enum<E>> E.previous(): E? {
    return enumEntries<E>().getOrNull(ordinal - 1)
}