package com.kieronquinn.app.utag.utils.extensions

import org.apache.commons.lang3.exception.ExceptionUtils

fun Throwable.asString(): String {
    return ExceptionUtils.getMessage(this) + "\n" + ExceptionUtils.getStackTrace(this)
}