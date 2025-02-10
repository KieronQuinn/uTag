package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.model.D2DStatus
import com.kieronquinn.app.utag.model.DeviceType

data class GeoLocation(
    @SerializedName("latitude")
    val latitude: String,
    @SerializedName("longitude")
    val longitude: String,
    @SerializedName("method")
    val method: String,
    @SerializedName("accuracy")
    val accuracy: String,
    @SerializedName("speed")
    val speed: String?,
    @SerializedName("rssi")
    val rssi: String?,
    @SerializedName("battery")
    val battery: BatteryLevel,
    @SerializedName("lastUpdateTime")
    val lastUpdateTime: Long,
    @SerializedName("valid")
    val valid: Boolean,
    @SerializedName("nearby")
    val nearby: Boolean?,
    @SerializedName("onDemand")
    val onDemand: Boolean?,
    @SerializedName("d2dStatus")
    val d2dStatus: D2DStatus?,
    @SerializedName("findNode")
    val findNode: FindNode?,
    @SerializedName("connectedUser")
    val connectedUser: ConnectedUser?,
    @SerializedName("connectedDevice")
    val connectedDevice: ConnectedDevice?
) {

    data class FindNode(
        @SerializedName("host")
        val host: DeviceType?
    )

    data class ConnectedUser(
        @SerializedName("id")
        val id: String?
    )

    data class ConnectedDevice(
        @SerializedName("id")
        val id: String?
    )

}
