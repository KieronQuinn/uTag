package com.kieronquinn.app.utag.utils.extensions

import android.text.format.DateUtils
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date

fun LocalDateTime.toEpochMilli(offset: ZoneOffset = ZonedDateTime.now().offset): Long {
    return toInstant(offset).toEpochMilli()
}

fun LocalDate.atEndOfDay(): LocalDateTime {
    return plusDays(1).atStartOfDay()
}

fun Long.fromEpochMilli(zoneId: ZoneId): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zoneId)
}

fun DateFormat.formatDateTime(dateTime: LocalDateTime): String {
    return format(Date.from(dateTime.toInstant(ZonedDateTime.now().offset)))
}

fun Long.formatTimeSince(): CharSequence {
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.SECOND_IN_MILLIS
    )
}

fun ZonedDateTime.getUtcOffset(): String {
    val id = offset.id
    val zoneId = if(id == "Z") "+00:00" else id
    return "UTC$zoneId"
}