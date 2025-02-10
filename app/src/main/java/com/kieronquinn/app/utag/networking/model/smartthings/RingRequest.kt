package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class RingRequest(
    @SerializedName("command")
    val command: RingCommand
) {
    enum class RingCommand {
        @SerializedName("start")
        START,
        @SerializedName("stop")
        STOP
    }
}
