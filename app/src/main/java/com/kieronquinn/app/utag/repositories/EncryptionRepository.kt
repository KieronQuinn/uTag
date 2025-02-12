package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Base64
import com.kieronquinn.app.utag.Application.Companion.isMainProcess
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.model.GeoLocation
import com.kieronquinn.app.utag.networking.model.smartthings.GeoLocationResponse.KeyPair
import com.kieronquinn.app.utag.providers.PinProvider
import com.kieronquinn.app.utag.repositories.EncryptionRepository.Companion.ACTION_PIN_STAGE_CHANGED
import com.kieronquinn.app.utag.repositories.EncryptionRepository.Companion.ENCRYPTION_REG_DATE_FORMAT
import com.kieronquinn.app.utag.repositories.EncryptionRepository.DecryptionResult
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.xposed.extensions.applySecurity
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.kieronquinn.app.utag.networking.model.smartthings.GeoLocation as ServerGeoLocation

interface EncryptionRepository {

    companion object {
        internal const val ACTION_PIN_STAGE_CHANGED =
            "${BuildConfig.APPLICATION_ID}.action.PIN_STATE_CHANGED"
        val ENCRYPTION_REG_DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        fun notifyPinStateChanged(context: Context) {
            context.sendBroadcast(Intent(ACTION_PIN_STAGE_CHANGED).apply {
                applySecurity(context)
                `package` = context.packageName
            })
        }
    }

    /**
     *  Flow which is triggered when a PIN is entered or cleared
     */
    val pinStateChangeBus: MutableSharedFlow<Long>

    /**
     *  Saves the PIN in memory, and if [savePin] is specified, also commits it to encrypted
     *  storage
     */
    fun setPIN(pin: String, savePin: Boolean)

    /**
     *  Clears the PIN from memory
     */
    fun clearPin()

    /**
     *  Returns if there's a valid, in-memory PIN available
     */
    fun hasPin(): Boolean

    /**
     *  Attempts to decrypt a given location if required, using the specified [cipher]
     */
    suspend fun decryptLocationIfNeeded(
        location: ServerGeoLocation, cipher: Cipher?, hasKeyPair: Boolean
    ): DecryptionResult

    /**
     *  Gets the in memory or stored PIN or null if it's not set
     */
    fun getPin(): String?

    /**
     *  Loads the [KeyPair] and returns the [Cipher] for it
     */
    suspend fun getDecryptionCipher(pin: String?, keyPair: KeyPair): Cipher?

    /**
     *  Generates a [KeyPair], based on the PIN and user ID suitable for sending to the server to
     *  update the user's encryption keys
     */
    suspend fun generateEncryptionData(pin: String, userId: String): KeyPair?

    sealed class DecryptionResult {
        data class Success(val location: GeoLocation): DecryptionResult()
        data object Error: DecryptionResult()
        data object PINRequired: DecryptionResult()
        data object NoKeys: DecryptionResult()
    }

}

class EncryptionRepositoryImpl(
    private val context: Context,
    encryptedSettingsRepository: EncryptedSettingsRepository
): EncryptionRepository {

    private val savedPin = encryptedSettingsRepository.savedPin
    private val random = SecureRandom()
    private val scope = MainScope()

    private val pinTimeout = encryptedSettingsRepository.pinTimeout.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private var pin: String? = null
    private var pinTime: Long? = null
    private var savePin: Boolean? = null

    private val pinStateChanged = context.broadcastReceiverAsFlow(
        IntentFilter(ACTION_PIN_STAGE_CHANGED)
    ).map {
        it.verifySecurity(context.packageName)
    }

    override val pinStateChangeBus = MutableStateFlow(System.currentTimeMillis())

    override fun setPIN(pin: String, savePin: Boolean) {
        if(isMainProcess()) {
            PinProvider.setPin(context, pin, savePin)
        }else {
            this.pin = pin
            this.pinTime = System.currentTimeMillis()
            this.savePin = savePin
            EncryptionRepository.notifyPinStateChanged(context)
        }
    }

    override fun clearPin() {
        if(isMainProcess()) {
            PinProvider.clearPin(context)
        }else{
            this.pin = null
            this.pinTime = null
            this.savePin = false
            EncryptionRepository.notifyPinStateChanged(context)
        }
    }

    override fun hasPin(): Boolean {
        return if(isMainProcess()) {
            PinProvider.hasPin(context)
        }else{
            runBlocking {
                getInMemoryPinOrNull() != null
            }
        }
    }

    override suspend fun decryptLocationIfNeeded(
        location: ServerGeoLocation,
        cipher: Cipher?,
        hasKeyPair: Boolean
    ): DecryptionResult = withContext(Dispatchers.IO) {
        val unencryptedLatitude = location.latitude.toDoubleOrNull()
        val unencryptedLongitude = location.longitude.toDoubleOrNull()
        val findHost = location.findNode?.host
        when {
            unencryptedLatitude != null && unencryptedLongitude != null -> {
                DecryptionResult.Success(GeoLocation(
                    unencryptedLatitude,
                    unencryptedLongitude,
                    location.accuracy.toDoubleOrNull()
                        ?: return@withContext DecryptionResult.Error,
                    location.speed?.toDoubleOrNull(),
                    location.rssi?.toIntOrNull(),
                    location.battery,
                    location.lastUpdateTime,
                    location.method,
                    findHost,
                    location.nearby,
                    location.onDemand,
                    location.connectedUser?.id,
                    location.connectedDevice?.id,
                    location.d2dStatus,
                    false
                ))
            }
            !hasKeyPair -> DecryptionResult.NoKeys
            cipher != null -> {
                val decryptedLatitude = decrypt(location.latitude, cipher)?.toDoubleOrNull()
                    ?: return@withContext DecryptionResult.PINRequired
                val decryptedLongitude = decrypt(location.longitude, cipher)?.toDoubleOrNull()
                    ?: return@withContext DecryptionResult.PINRequired
                DecryptionResult.Success(GeoLocation(
                    decryptedLatitude,
                    decryptedLongitude,
                    location.accuracy.toDoubleOrNull()
                        ?: return@withContext DecryptionResult.Error,
                    location.speed?.toDoubleOrNull(),
                    location.rssi?.toIntOrNull(),
                    location.battery,
                    location.lastUpdateTime,
                    location.method,
                    findHost,
                    location.nearby,
                    location.onDemand,
                    location.connectedUser?.id,
                    location.connectedDevice?.id,
                    location.d2dStatus,
                    true
                ))
            }
            else -> {
                DecryptionResult.PINRequired
            }
        }
    }

    override fun getPin(): String? {
        return if(isMainProcess()) {
            PinProvider.getPin(context)
        }else{
            runBlocking {
                getInMemoryPinOrNull() ?: savedPin.getOrNull()
            }
        }
    }

    override suspend fun getDecryptionCipher(pin: String?, keyPair: KeyPair): Cipher? {
        if(keyPair.privateKey == null || keyPair.iv == null || pin == null) return null
        val pinUserId: String
        val privateKey: ByteArray
        // "v2" key spec appends _v2 to the end of the Base64 and adds the user ID to the PIN
        if(keyPair.privateKey.endsWith("_v2")) {
            pinUserId = "$pin${keyPair.userId}"
            privateKey = try {
                Base64.decode(keyPair.privateKey.removeSuffix("_v2"), Base64.NO_WRAP)
            }catch (e: IllegalArgumentException) {
                return null
            }
        }else{
            pinUserId = pin
            privateKey = try {
                Base64.decode(keyPair.privateKey, Base64.NO_WRAP)
            }catch (e: IllegalArgumentException) {
                return null
            }
        }
        val iv = try {
            Base64.decode(keyPair.iv, Base64.NO_WRAP)
        }catch (e: IllegalArgumentException) {
            return null
        }
        val decryptedKey = encryptDecryptKey(Cipher.DECRYPT_MODE, pinUserId, privateKey, iv)
            ?: run {
                clearPin()
                return null
            }
        return getCipher(decryptedKey).also {
            commitPinIfNeeded(pin)
        }
    }

    override suspend fun generateEncryptionData(pin: String, userId: String): KeyPair? {
        return withContext(Dispatchers.IO) {
            try {
                val pinUserId = "$pin$userId"
                val generator = KeyPairGenerator
                    .getInstance("EC", BouncyCastleProvider())
                val keys = generator.genKeyPair()
                val privateKey = keys.private.encoded
                val publicKey = keys.public.encoded
                val iv = ByteArray(16).apply {
                    random.nextBytes(this)
                }
                val encryptedPrivateKey = encryptDecryptKey(
                    Cipher.ENCRYPT_MODE,
                    pinUserId,
                    privateKey,
                    iv
                )
                val suffixedPrivateKey =
                    "${Base64.encodeToString(encryptedPrivateKey, Base64.NO_WRAP)}_v2"
                KeyPair(
                    userId = userId,
                    privateKey = suffixedPrivateKey,
                    publicKey = Base64.encodeToString(publicKey, Base64.NO_WRAP),
                    iv = Base64.encodeToString(iv, Base64.NO_WRAP),
                    regDate = ZonedDateTime.now()
                        .withZoneSameInstant(ZoneId.of("UTC"))
                        .format(ENCRYPTION_REG_DATE_FORMAT)
                )
            }catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun getInMemoryPinOrNull(): String? {
        pinTime?.let {
            val now = System.currentTimeMillis()
            val timeout = pinTimeout.firstNotNull().minutes * 60_000L
            if(now - it > timeout) {
                //In memory PIN has expired, ask again
                pin = null
                pinTime = null
            }
        }
        return pin
    }

    private fun decrypt(
        input: String,
        cipher: Cipher?
    ): String? {
        if(cipher == null) return null
        return try {
            val inputBytes = Base64.decode(input, Base64.NO_WRAP)
            decryptData(inputBytes, cipher)?.let {
                String(it)
            }
        }catch (e: Exception) {
            null
        }
    }

    private fun encryptDecryptKey(
        mode: Int,
        pinUserId: String,
        input: ByteArray,
        iv: ByteArray
    ): ByteArray? {
        val messageDigest = MessageDigest.getInstance("SHA-256").apply {
            update(pinUserId.toByteArray(Charsets.UTF_8))
        }
        val keySpec = SecretKeySpec(messageDigest.digest(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(mode, keySpec, IvParameterSpec(iv))
        return try {
            cipher.doFinal(input)
        }catch (e: Exception) {
            null
        }
    }

    private fun getCipher(key: ByteArray): Cipher? {
        val privateKey = KeyFactory.getInstance("EC", BouncyCastleProvider())
            .generatePrivate(PKCS8EncodedKeySpec(key))
        return Cipher.getInstance("ECIES", BouncyCastleProvider()).apply {
            init(Cipher.DECRYPT_MODE, privateKey)
        }
    }

    private fun decryptData(input: ByteArray, cipher: Cipher): ByteArray? {
        return cipher.doFinal(input)
    }

    private suspend fun commitPinIfNeeded(pin: String) {
        //Pin has not changed if savePin has not been set
        val savePin = savePin ?: return
        if(!savePin) {
            savedPin.clear()
        }else{
            savedPin.set(pin)
        }
    }

    private fun setupChangeListener() = scope.launch {
        pinStateChanged.collect {
            pinStateChangeBus.emit(System.currentTimeMillis())
        }
    }

    init {
        setupChangeListener()
    }

}