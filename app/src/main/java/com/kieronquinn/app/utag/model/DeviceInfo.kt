package com.kieronquinn.app.utag.model

import android.graphics.Bitmap
import com.kieronquinn.app.utag.networking.model.smartthings.SetSearchingStatusRequest.SearchingStatus

data class DeviceInfo(
    val ownerId: String,
    val deviceId: String,
    val name: String,
    val label: String,
    val icon: String,
    val mnId: String,
    val setupId: String,
    val modelName: String,
    val disconnectedIcon: String,
    val markerIcons: Pair<Bitmap, Bitmap>,
    val isOwner: Boolean,
    val batteryLevel: BatteryLevel?,
    val batteryUpdatedAt: Long?,
    val shareable: Boolean,
    val isPossiblySharable: Boolean,
    val searchingStatus: SearchingStatus,
    val e2eEncrypted: Boolean,
    val supportsUwb: Boolean,
    val petWalkingEnabled: Boolean,
    val remoteRingEnabled: Boolean
)
