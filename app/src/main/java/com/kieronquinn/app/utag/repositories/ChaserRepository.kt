package com.kieronquinn.app.utag.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.model.ChaserRegion
import com.kieronquinn.app.utag.networking.model.chaser.ChaserAccessTokenResponse
import com.kieronquinn.app.utag.networking.model.chaser.ChaserLocationsRequest
import com.kieronquinn.app.utag.networking.model.chaser.ChaserLocationsRequest.ChaserTag
import com.kieronquinn.app.utag.networking.model.chaser.ChaserLocationsRequest.ChaserTag.ChaserGeoLocation
import com.kieronquinn.app.utag.networking.model.chaser.ChaserLocationsRequest.ChaserTag.TagAdvertisement
import com.kieronquinn.app.utag.networking.model.chaser.ChaserPublicKeyResponse
import com.kieronquinn.app.utag.networking.model.chaser.ChaserTagDataRequest
import com.kieronquinn.app.utag.networking.services.AuthenticatedChaserService
import com.kieronquinn.app.utag.networking.services.ChaserService
import com.kieronquinn.app.utag.networking.services.ChaserService.Companion.getAccessToken
import com.kieronquinn.app.utag.networking.services.ChaserService.Companion.getNonce
import com.kieronquinn.app.utag.networking.services.ChaserService.Companion.getPublicKeys
import com.kieronquinn.app.utag.networking.services.ChaserService.Companion.sendLocations
import com.kieronquinn.app.utag.repositories.ChaserRepository.ChaserCertificate
import com.kieronquinn.app.utag.repositories.ChaserRepository.RawCertificate
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepository.NonOwnerTag
import com.kieronquinn.app.utag.utils.chaser.FmmContext
import com.kieronquinn.app.utag.utils.extensions.get
import com.samsung.android.fmm.maze.FmmFontJNI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import retrofit2.Retrofit
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStore.PrivateKeyEntry
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal


interface ChaserRepository {

    /**
     *  Current certificate state, loaded from prefs
     */
    val certificate: StateFlow<ChaserCertificate?>

    /**
     *  Tries to send a [NonOwnerTag]'s [Location] to the chaser server, if the user has it set up
     *  correctly.
     */
    suspend fun sendLocations(tags: List<NonOwnerTag>, location: Location)

    /**
     *  Gets Tag image URLs from server for given [serviceData]s. Returned as a Map of service data
     *  -> image URL
     */
    suspend fun getTagImages(vararg serviceData: String): Map<String, String>

    sealed class ChaserCertificate {
        data class Certificate(
            val certificate: String,
            val privateKey: PrivateKey
        ): ChaserCertificate()
        data object None: ChaserCertificate()
    }

    data class RawCertificate(
        @SerializedName("keystorePassword")
        val keystorePassword: String,
        @SerializedName("certificate")
        val certificate: String,
        @SerializedName("keyPass")
        val keyPass: String,
        @SerializedName("alias")
        val alias: String
    )

}

class ChaserRepositoryImpl(
    private val context: Context,
    private val gson: Gson,
    private val settingsRepository: SettingsRepository,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    retrofit: Retrofit
): ChaserRepository {

    companion object {
        private const val FMM_VERSION = 731802100L
        private const val POLICY_VERSION = "4"
        private const val KEYSTORE_ANDROID = "AndroidKeyStore"
        private const val KEYSTORE_CHASER = "ChaserKeyStore"
    }

    private val scope = MainScope()
    private val chaserService = ChaserService.createService(retrofit)
    private val authenticatedChaserService = AuthenticatedChaserService.createService(context, retrofit)
    private val chaserCount = encryptedSettingsRepository.chaserCount
    private val fmmContext = FmmContext(context)

    private val rawCertificate = flow {
        val cert = try {
            RawCertificate(
                keystorePassword = FmmFontJNI.getFont1(),
                certificate = FmmFontJNI.getFont2(fmmContext),
                keyPass = FmmFontJNI.getFont3(),
                alias = FmmFontJNI.getFont4()
            )
        }catch (e: Throwable) {
            null
        }
        emit(cert)
    }

    override val certificate = rawCertificate.mapLatest {
        getCertificate(it)
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun sendLocations(tags: List<NonOwnerTag>, location: Location) {
        val certificate = certificate.value as? ChaserCertificate.Certificate ?: return
        val disconnectedTags = tags.filter { it.tagData.tagState.shouldSendToChaser() }
        disconnectedTags.groupBy { it.tagData.region }.forEach { (region, tags) ->
            sendLocations(tags, certificate, region ?: return@forEach, location)
        }
    }

    override suspend fun getTagImages(vararg serviceData: String): Map<String, String> {
        return withContext(Dispatchers.IO) {
            val request = ChaserTagDataRequest(
                serviceData.map { ChaserTagDataRequest.Item(it) }
            )
            val data = authenticatedChaserService.getTagData(request).get(name = "tagImage")
                ?: return@withContext emptyMap()
            data.items?.mapNotNull {
                if(it.result != "OK") return@mapNotNull null
                val imageUrl = it.contents.firstOrNull { content ->
                    content.type == "already_registered" && content.resourceType == "page"
                }?.file ?: return@mapNotNull null
                Pair(it.serviceData, imageUrl)
            }?.toMap() ?: emptyMap()
        }
    }

    private suspend fun sendLocations(
        nonOwnerTags: List<NonOwnerTag>,
        certificate: ChaserCertificate.Certificate,
        region: ChaserRegion,
        location: Location
    ) {
        val nonce = getNonce(region) ?: return
        val accessToken = getAccessToken(region, certificate, nonce) ?: return
        val privIds = nonOwnerTags.mapNotNull { tag ->
            tag.tagData.getPrivIdForUrl().takeIf { tag.tagData.encryptionFlag }
        }
        val ciphers = if(privIds.isNotEmpty()) {
            chaserService.getPublicKeys(
                region,
                accessToken.accessToken,
                *privIds.toTypedArray()
            ).get(name = "publicKeys")?.getCiphers() ?: emptyMap()
        }else emptyMap()
        val chaserTags = nonOwnerTags.mapNotNull { nonOwnerTag ->
            val serviceData = Base64.encodeToString(nonOwnerTag.tagData.serviceData, Base64.NO_WRAP)
            val cipher = ciphers[nonOwnerTag.tagData.getPrivIdForUrl()]
            if(cipher == null && nonOwnerTag.tagData.encryptionFlag) {
                //If the Cipher load failed & encryption is enabled, don't leak location to server
                return@mapNotNull null
            }
            val latitude = if(cipher != null) {
                cipher.doFinal(location.latitude.toString().toByteArray()).let {
                    Base64.encodeToString(it, Base64.NO_WRAP)
                }
            }else location.latitude.toString()
            val longitude = if(cipher != null) {
                cipher.doFinal(location.longitude.toString().toByteArray()).let {
                    Base64.encodeToString(it, Base64.NO_WRAP)
                }
            }else location.longitude.toString()
            ChaserTag(
                geoLocation = ChaserGeoLocation(
                    accuracy = location.accuracy.toString(),
                    battery = nonOwnerTag.tagData.batteryLevel.name,
                    latitude = latitude,
                    longitude = longitude,
                    method = location.provider.toString(),
                    rssi = nonOwnerTag.rssi.toString(),
                    speed = location.speed.toString(),
                    timestamp = nonOwnerTag.lastReceivedTime
                ),
                tagAdvertisement = TagAdvertisement(serviceData)
            )
        }
        //All request construction failed, don't send request
        if(chaserTags.isEmpty()) return
        val locationBody = ChaserLocationsRequest(
            items = chaserTags,
            findNode = accessToken.findNode.copy(
                id = getHashedAndroidId(),
                policyVersion = POLICY_VERSION,
                configuration = accessToken.findNode.configuration.copy(
                    src = "chaser"
                )
            )
        )
        chaserCount.increment(nonOwnerTags.size)
        chaserService.sendLocations(region, accessToken.accessToken, locationBody)
            .get(name = "chaserSendLocations")
    }

    /**
     *  Decrypt the various layers required to get the X509 key spec for the private key, then
     *  generate a public key and return a map of [Cipher] to encrypt the latitude & longitude of a
     *  location, mapped to the priv IDs.
     */
    private fun ChaserPublicKeyResponse.getCiphers(): Map<String, Cipher>? {
        //Secret key, IV & encryption data are provided in the response, combine with keystore key
        val decryptedSecretKey = decryptChaserData(Base64.decode(encryptedSecretKey, Base64.NO_WRAP))
            ?: return null
        val decryptedIv = decryptChaserData(Base64.decode(encryptedIv, Base64.NO_WRAP))
            ?: return null
        val decryptCipher = try {
            val spec = SecretKeySpec(decryptedSecretKey, "AES")
            Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                init(Cipher.DECRYPT_MODE, spec, IvParameterSpec(decryptedIv))
            }
        }catch (e: Exception) {
            return null
        }
        //Decrypt the provided encryption data and parse it
        val encryptionData = try {
            decryptCipher.doFinal(Base64.decode(encryptedData, Base64.NO_WRAP))
                .let { gson.fromJson(String(it), EncryptionData::class.java) }
        }catch (e: Exception) {
            return null
        }
        return encryptionData.items.mapNotNull {
            //If result is bad or encryption is enabled, skip this item, otherwise load the key
            val encryptionKey = if(it.result == "OK" && it.encryptionEnabled) {
                Base64.decode(it.pubKey, Base64.NO_WRAP)
            } else return@mapNotNull null
            //Construct Cipher from the final key and map it to the private ID provided
            try {
                val spec = X509EncodedKeySpec(encryptionKey)
                val publicKey = KeyFactory.getInstance("EC", BouncyCastleProvider())
                    .generatePublic(spec)
                Cipher.getInstance("ECIES", BouncyCastleProvider()).apply {
                    init(Cipher.ENCRYPT_MODE, publicKey)
                }.let { cipher ->
                    Pair(it.pid, cipher)
                }
            }catch (e: Exception) {
                null
            }
        }.toMap()
    }

    /**
     *  Loads the Chaser key from the keystore that was created earlier and decrypts the provided
     *  [data].
     */
    private fun decryptChaserData(data: ByteArray): ByteArray? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_ANDROID).apply {
                load(null)
            }
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding").apply {
                init(Cipher.DECRYPT_MODE, keyStore.getKey(KEYSTORE_CHASER, "".toCharArray()))
            }
            cipher.doFinal(data)
        }catch (e: Exception){
            null
        }
    }

    /**
     *  Loads a certificate from [rawCert]
     */
    private fun getCertificate(rawCert: RawCertificate?): ChaserCertificate {
        val rawCertificate = rawCert ?: return ChaserCertificate.None
        val keyStore = try {
            val cert = Base64.decode(rawCertificate.certificate, Base64.NO_WRAP).inputStream()
            KeyStore.getInstance("BKS").apply {
                load(cert, rawCertificate.keystorePassword.toCharArray())
            }
        }catch (e: Exception) {
            return ChaserCertificate.None
        }
        val certificate = try {
            keyStore.getCertificateChain(rawCertificate.alias)?.let {
                encodeChain(it)
            }
        } catch (e: Exception) {
            null
        } ?: return ChaserCertificate.None
        val privateKey = try {
            keyStore.getKey(
                rawCertificate.alias, rawCertificate.keyPass.toCharArray()
            ) as? PrivateKey
        } catch (e: Exception) {
            null
        } ?: return ChaserCertificate.None
        return ChaserCertificate.Certificate(certificate, privateKey)
    }

    private fun encodeChain(certificates: Array<Certificate>): String? {
        if (certificates.size >= 2) {
            val certs = certificates[1].encoded + certificates[0].encoded
            return Base64.encodeToString(certs, Base64.NO_WRAP)
        }
        return null
    }

    private suspend fun getNonce(region: ChaserRegion): String? {
        return chaserService.getNonce(region).get(name = "chaserNonce")?.nonce
    }

    private suspend fun getAccessToken(
        region: ChaserRegion,
        certificate: ChaserCertificate.Certificate,
        nonce: String
    ): ChaserAccessTokenResponse? {
        val signature = try {
            Signature.getInstance("SHA256withRSA").apply {
                initSign(certificate.privateKey)
                update(nonce.toByteArray())
            }.sign().let {
                Base64.encodeToString(it, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            return null
        }
        return chaserService.getAccessToken(
            region = region,
            nonce = nonce,
            signature = signature,
            certificate = certificate.certificate,
            version = FMM_VERSION,
            publicKey = getPublicKey()
        ).get(name = "chaserAccessToken")
    }

    private fun getPublicKey(): String {
        val keyStore = KeyStore.getInstance(KEYSTORE_ANDROID).apply {
            load(null)
        }
        if(keyStore.getEntry(KEYSTORE_CHASER, null) !is PrivateKeyEntry) {
            generateKeyPair()
        }
        if(keyStore.getCertificate(KEYSTORE_CHASER) == null) {
            generateKeyPair()
        }
        val publicKey = keyStore.getCertificate(KEYSTORE_CHASER).publicKey ?: return ""
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    @SuppressLint("WrongConstant")
    private fun generateKeyPair() {
        val generator = KeyPairGenerator.getInstance("RSA", KEYSTORE_ANDROID)
        val spec = KeyGenParameterSpec.Builder(KEYSTORE_CHASER, 15)
            .setCertificateSubject(X500Principal("CN=$KEYSTORE_CHASER"))
            .setDigests("SHA-256", "SHA-512")
            .setSignaturePaddings("PKCS1")
            .setEncryptionPaddings("PKCS1Padding")
            .setCertificateSerialNumber(BigInteger.valueOf(0x539L))
            .setKeySize(0x800)
            .build()
        generator.initialize(spec)
        generator.generateKeyPair()
    }

    @SuppressLint("HardwareIds") //Matches FMM
    private fun getHashedAndroidId(): String {
        return hashAndroidId(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        )
    }

    private fun hashAndroidId(androidId: String): String {
        try {
            val id = if (androidId.length >= 4) {
                androidId.substring(0, 4)
            } else {
                androidId
            }
            val hash = MessageDigest.getInstance("SHA-256")
                .digest((androidId + "findMyMobile").toByteArray())
            return id + Hex.toHexString(hash)
        } catch (e: Exception) {
            return androidId
        }
    }

    private data class EncryptionData(
        @SerializedName("items")
        val items: List<Item>
    ) {
        data class Item(
            @SerializedName("pid")
            val pid: String,
            @SerializedName("result")
            val result: String,
            @SerializedName("encryptionEnabled")
            val encryptionEnabled: Boolean,
            @SerializedName("pubKey")
            val pubKey: String
        )
    }

}