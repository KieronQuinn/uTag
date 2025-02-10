package com.kieronquinn.app.utag.model

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.R

enum class DeviceType(@StringRes val label: Int) {
    @SerializedName("GALAXY_PHONE")
    GALAXY_PHONE(R.string.device_type_galaxy_phone),
    @SerializedName("GALAXY_TABLET")
    GALAXY_TABLET(R.string.device_type_galaxy_tablet),
    @SerializedName("NONE_GALAXY_PHONE")
    NONE_GALAXY_PHONE(R.string.device_type_none_galaxy_phone),
    @SerializedName("NONE_GALAXY_TABLET")
    NONE_GALAXY_TABLET(R.string.device_type_none_galaxy_tablet),
}