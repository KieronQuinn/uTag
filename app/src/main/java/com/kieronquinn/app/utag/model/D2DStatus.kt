package com.kieronquinn.app.utag.model

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.R

enum class D2DStatus(@StringRes val label: Int) {
    @SerializedName("bleScanned")
    BLE_SCANNED(R.string.d2d_status_scanned),
    @SerializedName("gattConnected")
    GATT_CONNECTED(R.string.d2d_status_connected)
}