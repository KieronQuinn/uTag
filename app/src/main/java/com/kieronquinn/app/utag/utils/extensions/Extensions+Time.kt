package com.kieronquinn.app.utag.utils.extensions

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.StringRes
import com.kieronquinn.app.utag.R
import java.util.Locale

fun Context.formatTime(
    timestamp: Long?,
    raw: Boolean = false,
    @StringRes template: Int = R.string.map_timestamp_last_updated
): String {
    val now = System.currentTimeMillis()
    return when {
        timestamp == null || timestamp <= 0L -> getString(R.string.map_timestamp_unknown)
        !raw && timestamp < now && DateUtils.isToday(timestamp) -> {
            val time = now / 1000L - timestamp / 1000L
            when {
                time < 60L -> resources.getString(R.string.map_timestamp_now)
                time < 3600L -> {
                    val minutes = (time / 60L).toInt()
                    resources.getQuantityString(R.plurals.map_timestamp_minute, minutes, minutes)
                }
                else -> {
                    val hours = (time / 3600L).toInt()
                    resources.getQuantityString(R.plurals.map_timestamp_hour, hours, hours)
                }
            }
        }
        raw && DateUtils.isToday(timestamp) -> {
            val timeFormat = if(DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
            val dateTimeFormat = SimpleDateFormat(timeFormat, Locale.getDefault())
            dateTimeFormat.format(timestamp)
        }
        else -> {
            val timeFormat = if(DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
            val dateFormat = DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                "yyyy.MM.dd $timeFormat"
            )
            val dateTimeFormat = SimpleDateFormat(dateFormat, Locale.getDefault())
            dateTimeFormat.format(timestamp)
        }
    }.let {
        getString(template, it)
    }
}