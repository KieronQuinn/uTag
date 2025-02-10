package com.kieronquinn.app.utag.model

enum class VolumeLevel(val value: String) {
    LOW("01"),
    HIGH("02");

    companion object {
        fun fromValue(value: String): VolumeLevel {
            return entries.firstOrNull { it.value == value } ?: LOW
        }
    }
}