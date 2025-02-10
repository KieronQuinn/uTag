package com.kieronquinn.app.utag.model

import android.os.Parcelable
import androidx.room.TypeConverter
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import javax.crypto.Cipher

/**
 *  ByteArray which is stored in the Room database in encrypted form, and encrypted/decrypted based
 *  on a key and IV stored in the encrypted shared preferences. Automatically creates and stores
 *  a key and IV on first use.
 */
object EncryptedValueConverter: KoinComponent {

    const val ENCRYPTION_TRANSFORMATION = "AES/CBC/PKCS5PADDING"

    private val encryptedSettings by inject<EncryptedSettingsRepository>()
    private val key = encryptedSettings.getDatabaseEncryptionKey()
    private val iv = encryptedSettings.getDatabaseEncryptionIV()

    private val encryptionCipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION).apply {
        init(Cipher.ENCRYPT_MODE, key, this@EncryptedValueConverter.iv)
    }

    private val decryptionCipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION).apply {
        init(Cipher.DECRYPT_MODE, key, this@EncryptedValueConverter.iv)
    }

    @Synchronized
    @TypeConverter
    fun fromBytes(value: ByteArray?): EncryptedValue? {
        return value?.let { EncryptedValue(decryptionCipher.doFinal(it)) }
    }

    @Synchronized
    @TypeConverter
    fun fromEncryptedValue(value: EncryptedValue?): ByteArray? {
        return value?.bytes?.let { encryptionCipher.doFinal(it) }
    }

}

@Parcelize
data class EncryptedValue(val bytes: ByteArray): Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedValue

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}