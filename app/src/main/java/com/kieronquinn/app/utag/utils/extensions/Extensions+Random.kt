package com.kieronquinn.app.utag.utils.extensions

import org.apache.commons.lang3.RandomStringUtils
import java.security.SecureRandom

fun secureRandom(length: Int) = RandomStringUtils.random(
    length, 0, 0, true, true, null, SecureRandom()
)

fun getRandomString(length: Int) : String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}