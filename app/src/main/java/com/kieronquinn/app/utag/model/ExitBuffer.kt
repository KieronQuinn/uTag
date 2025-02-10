package com.kieronquinn.app.utag.model

import androidx.annotation.StringRes
import com.kieronquinn.app.utag.R

enum class ExitBuffer(val minutes: Int, @StringRes val label: Int) {
    NONE(0, R.string.safe_area_buffer_none),
    ONE_MINUTE(1, R.string.safe_area_buffer_one),
    TWO_MINUTES(2, R.string.safe_area_buffer_two),
    THREE_MINUTES(3, R.string.safe_area_buffer_three),
    FOUR_MINUTES(4, R.string.safe_area_buffer_four),
    FIVE_MINUTES(5, R.string.safe_area_buffer_five),
    TEN_MINUTES(10, R.string.safe_area_buffer_ten),
    FIFTEEN_MINUTES(15, R.string.safe_area_buffer_fifteen)
}