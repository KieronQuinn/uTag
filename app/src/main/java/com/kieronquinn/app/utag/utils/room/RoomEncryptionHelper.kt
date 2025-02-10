package com.kieronquinn.app.utag.utils.room

class RoomEncryptionHelper(
    private val failedCallbacks: () -> List<RoomEncryptionFailedCallback>
) {

    interface RoomEncryptionFailedCallback {
        fun onEncryptionFailed()
    }

    fun onEncryptionFailed() {
        failedCallbacks().forEach {
            it.onEncryptionFailed()
        }
    }

}