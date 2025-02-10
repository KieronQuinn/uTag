package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.model.D2DStatus

data class SendLocationRequest(
    @SerializedName("nearby")
    val nearby: Boolean = true,
    @SerializedName("d2dStatus")
    val d2dStatus: D2DStatus = D2DStatus.GATT_CONNECTED,
    @SerializedName("connectedDevice")
    val connectedDevice: ConnectedDevice,
    @SerializedName("connectedUser")
    val connectedUser: ConnectedUser,
    @SerializedName("geolocation")
    val geolocation: GeoLocation,
    @SerializedName("onDemand")
    val onDemand: Boolean
) {
    data class ConnectedDevice(
        @SerializedName("commandable")
        val commandable: Boolean = true,
        @SerializedName("id")
        val id: String,
        @SerializedName("name")
        val name: String
    )

    data class ConnectedUser(
        @SerializedName("id")
        val id: String,
        @SerializedName("name")
        val name: String
    )

    data class GeoLocation(
        @SerializedName("latitude")
        val latitude: String,
        @SerializedName("longitude")
        val longitude: String,
        @SerializedName("accuracy")
        val accuracy: String,
        @SerializedName("valid")
        val valid: Boolean = true,
        @SerializedName("timeStamp")
        val timestamp: Long,
        @SerializedName("method")
        val method: String,
        @SerializedName("battery")
        val battery: BatteryLevel
    )
}
