package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.networking.model.smartthings.SetSearchingStatusRequest.SearchingStatus

data class GetDeviceResponse(
    @SerializedName("ownerId")
    val ownerId: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("label")
    val label: String,
    @SerializedName("icons")
    val icons: Icons,
    @SerializedName("bleD2D")
    val bleD2D: BleD2D,
    @SerializedName("components")
    val components: List<Components>,
    @SerializedName("ocfDeviceType")
    val ocfDeviceType: String
) {
    data class Icons(
        @SerializedName("coloredIcon")
        val colouredIcon: String,
        @SerializedName("disconnectedIcon")
        val disconnectedIcon: String
    )

    data class BleD2D(
        @SerializedName("metadata")
        val metadata: Metadata
    ) {
        data class Metadata(
            @SerializedName("battery")
            val battery: Battery?,
            @SerializedName("shareable")
            val shareable: Shareable,
            @SerializedName("searchingStatus")
            val searchingStatus: SearchingStatus,
            @SerializedName("e2eEncryption")
            val e2eEncryption: BooleanField,
            @SerializedName("remoteRing")
            val remoteRing: BooleanField,
            @SerializedName("petWalking")
            val petWalking: BooleanField,
            @SerializedName("onboardedBy")
            val onboardedBy: OnboardedBy,
            @SerializedName("vendor")
            val vendor: Vendor
        ) {
            data class Battery(
                @SerializedName("level")
                val level: BatteryLevel,
                @SerializedName("updated")
                val updated: Long
            )
            data class BooleanField(
                @SerializedName("enabled")
                val enabled: Boolean
            )
            data class OnboardedBy(
                @SerializedName("saGuid")
                val saGuid: String
            )
            data class Vendor(
                @SerializedName("mnId")
                val mnId: String,
                @SerializedName("setupId")
                val setupId: String,
                @SerializedName("modelName")
                val modelName: String
            )
            data class Shareable(
                @SerializedName("enabled")
                val enabled: Boolean,
                @SerializedName("members")
                val members: List<ShareableMember>?
            )
        }
    }

    data class Components(
        @SerializedName("capabilities")
        val capabilities: List<Capability>
    ) {

        data class Capability(
            @SerializedName("id")
            val id: String,
            @SerializedName("version")
            val version: Int?
        )

    }
}