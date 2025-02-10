package com.kieronquinn.app.utag.model

sealed class EncryptionKey {
    data object Unset: EncryptionKey()
    data class Set(
        val privateKey: String,
        val publicKey: String,
        val iv: String,
        val timeUpdated: Long
    ): EncryptionKey()
}