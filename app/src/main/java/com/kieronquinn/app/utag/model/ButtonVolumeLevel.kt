package com.kieronquinn.app.utag.model

import androidx.annotation.StringRes
import com.kieronquinn.app.utag.R

enum class ButtonVolumeLevel(val value: String, @StringRes val label: Int) {
    MUTED("00", R.string.button_volume_level_off),
    LOW("01", R.string.button_volume_level_low),
    HIGH("02", R.string.button_volume_level_high);

    companion object {
        fun fromValue(value: String): ButtonVolumeLevel {
            return ButtonVolumeLevel.entries.firstOrNull { it.value == value } ?: LOW
        }
    }
}