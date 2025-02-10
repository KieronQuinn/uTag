package com.kieronquinn.app.utag.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.R

enum class BatteryLevel(
    val level: String?,
    val intLevel: Int?,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    @StringRes val labelRaw: Int = label,
) {
    @SerializedName("VERY_LOW")
    VERY_LOW(
        "00",
        0,
        R.drawable.ic_battery1_very_low,
        R.string.battery_very_low
    ),
    @SerializedName("LOW")
    LOW(
        "01",
        1,
        R.drawable.ic_battery2_low,
        R.string.battery_low
    ),
    @SerializedName("MEDIUM")
    MEDIUM(
        "02",
        2,
        R.drawable.ic_battery3_sufficient,
        R.string.battery_sufficient
    ),
    @SerializedName("FULL")
    FULL(
        "03",
        3,
        //SmartThings maps full to sufficient for some reason, but has a full icon
        R.drawable.ic_battery4_full,
        R.string.battery_sufficient,
        R.string.battery_full
    ),
    @SerializedName("UNKNOWN")
    UNKNOWN(
        null,
        null,
        R.drawable.ic_battery_unknown,
        R.string.battery_unknown
    );

    companion object {
        fun fromLevel(level: String): BatteryLevel {
            return entries.firstOrNull { it.level == level } ?: UNKNOWN
        }

        fun fromIntLevel(level: Int): BatteryLevel {
            return entries.firstOrNull { it.intLevel == level } ?: UNKNOWN
        }
    }
}