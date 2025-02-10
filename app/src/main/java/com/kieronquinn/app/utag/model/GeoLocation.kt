package com.kieronquinn.app.utag.model

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val speed: Double?,
    val rssi: Int?,
    val battery: BatteryLevel?,
    val time: Long,
    val method: String,
    val findHost: DeviceType?,
    val nearby: Boolean?,
    val onDemand: Boolean?,
    val connectedUserId: String?,
    val connectedDeviceId: String?,
    val d2dStatus: D2DStatus?,
    val wasEncrypted: Boolean
)
