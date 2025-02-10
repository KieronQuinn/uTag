package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class GetRulesResponse(
    @SerializedName("items")
    val items: List<Item>?
) {
    data class Item(
        @SerializedName("actions")
        val actions: List<Action>?,
        @SerializedName("status")
        val status: String
    ) {
        data class Action(
            @SerializedName("if")
            val `if`: If?
        ) {
            data class If(
                @SerializedName("equals")
                val equals: Equals?
            ) {
                data class Equals(
                    @SerializedName("left")
                    val left: Condition?,
                    @SerializedName("right")
                    val right: Condition?
                ) {
                    data class Condition(
                        @SerializedName("device")
                        val device: Device?,
                        @SerializedName("string")
                        val string: String?,
                        @SerializedName("type")
                        val type: String
                    ) {
                        data class Device(
                            @SerializedName("devices")
                            val deviceIds: List<String>?,
                            @SerializedName("component")
                            val component: String?,
                            @SerializedName("capability")
                            val capability: String?,
                            @SerializedName("attribute")
                            val attribute: String?,
                            @SerializedName("trigger")
                            val trigger: String?
                        )
                    }
                }
            }
        }
    }
}
