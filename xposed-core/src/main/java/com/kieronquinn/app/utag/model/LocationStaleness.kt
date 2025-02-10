package com.kieronquinn.app.utag.model

enum class LocationStaleness(val millis: Long) {
    NONE(Long.MAX_VALUE),
    THIRTY_SECONDS(30_000L),
    ONE_MINUTE(60_000L),
    TWO_MINUTES(120_000L),
    THREE_MINUTES(180_000L);

    companion object {
        fun getOrNull(name: String): LocationStaleness? {
            return entries.firstOrNull { it.name == name }
        }
    }
}