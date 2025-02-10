package com.kieronquinn.app.utag.utils.extensions

fun Collection<Int>.or(): Int {
    var current = 0
    forEach {
        current = current.or(it)
    }
    return current
}

/**
 *  Removes duplicates from a list, but only if they're consecutive (keeping the first). Matching
 *  logic can be overridden by specifying [by], which defaults to `a != b`
 */
fun <T> Collection<T>.removeConsecutiveDuplicates(
    by: (T, T) -> Boolean = { a, b -> a != b }
): List<T> {
    return this.zipWithNext().filter { (a, b) -> by(a, b) }.map { it.first }
}

fun <T> Iterable<T>.groupConsecutiveBy(groupIdentifier: (T, T) -> Boolean) =
    if (!this.any())
        emptyList()
    else this
        .drop(1)
        .fold(mutableListOf(mutableListOf(this.first()))) { groups, t ->
            groups.last().apply {
                if (groupIdentifier.invoke(last(), t)) {
                    add(t)
                } else {
                    groups.add(mutableListOf(t))
                }
            }
            groups
        }