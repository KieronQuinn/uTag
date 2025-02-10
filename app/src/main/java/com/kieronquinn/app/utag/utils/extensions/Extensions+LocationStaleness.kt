package com.kieronquinn.app.utag.utils.extensions

import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.LocationStaleness

val LocationStaleness.label
    get() = when(this) {
        LocationStaleness.NONE -> R.string.location_staleness_none
        LocationStaleness.THIRTY_SECONDS -> R.string.location_staleness_thirty_seconds
        LocationStaleness.ONE_MINUTE -> R.string.location_staleness_one_minute
        LocationStaleness.TWO_MINUTES -> R.string.location_staleness_two_minutes
        LocationStaleness.THREE_MINUTES -> R.string.location_staleness_three_minutes
    }