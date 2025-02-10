package com.kieronquinn.app.utag.model

data class TagStateChangeEvent(
    val deviceId: String,
    val characteristics: String,
    val value: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagStateChangeEvent

        if (deviceId != other.deviceId) return false
        if (characteristics != other.characteristics) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + characteristics.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}