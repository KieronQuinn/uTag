package com.kieronquinn.app.utag.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.Application.Companion.CLIENT_ID_LOGIN
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.utils.extensions.Locale_getDefaultWithCountry
import org.apache.commons.codec.binary.Hex
import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import org.json.JSONException
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 *  Decompilation of sign in related functions from SmartThings. This contains code to generate
 *  sign in URLs.
 */
object SignInUtils: KoinComponent {

    private val authRepository by inject<AuthRepository>()
    private val context by inject<Context>()

    private const val CHAR_SET_NAME = "UTF-8"
    private const val KEY_SIZE = 16
    private const val SHA_ALGORITHM = "SHA-256"

    fun generateAuthUrl(
        deviceId: String,
        baseUrl: String,
        publicKey: String,
        chkDoNum: String,
        codeChallenge: String
    ): String {
        val svcParam = createSvcParam(codeChallenge, deviceId)
        val encrypted = encrypt(svcParam, publicKey, chkDoNum)
        return "$baseUrl?locale=${Locale_getDefaultWithCountry().toLanguageTag()}&svcParam=$encrypted&mode=C"
    }

    private fun createSvcParam(codeChallenge: String, deviceId: String): SvcParam {
        val redirectUri = Uri.Builder()
            .scheme(context.getString(R.string.auth_redirect_uri_scheme))
            .authority(context.getString(R.string.auth_redirect_uri_host))
            .build()
        return SvcParam(
            clientId = CLIENT_ID_LOGIN,
            codeChallenge = generateCodeChallenge(codeChallenge),
            codeChallengeMethod = "S256",
            competitorDeviceYNFlag = if(Build.MANUFACTURER == "samsung") "N" else "Y",
            countryCode = Locale_getDefaultWithCountry().country.lowercase(),
            //Samsung's login requires a supported browser, we spoof Chrome
            deviceInfo = "${Build.MANUFACTURER}|com.android.chrome",
            deviceModelID = Build.MODEL,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            deviceOSVersion = Build.VERSION.SDK_INT.toString(),
            devicePhysicalAddressText = "ANID:$deviceId",
            deviceType = "APP",
            deviceUniqueID = deviceId,
            redirectUri = redirectUri.toString(),
            replaceableClientConnectYN = "N",
            replaceableClientId = "",
            replaceableDevicePhysicalAddressText = "",
            responseEncryptionType = "1",
            responseEncryptionYNFlag = "Y",
            scope = "",
            state = authRepository.getState(),
            svcIptLgnID = "",
            iosYNFlag = "Y",
        )
    }

    private fun base64Encode(input: ByteArray): String {
        return String(Base64.encode(input, Base64.NO_WRAP))
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        IOException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        InvalidAlgorithmParameterException::class,
        JSONException::class
    )
    private fun encrypt(
        svcParam: SvcParam,
        b64PublicKey: String,
        chkDoNumString: String
    ): String {
        val chkDoNum = chkDoNumString.toInt()
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(Base64.decode(b64PublicKey, Base64.NO_WRAP))
        )
        val json = GsonBuilder().create().toJson(svcParam)
        val generatedKey = getKey(chkDoNumString, chkDoNum)
        val svcEncKY = getEncryptedKey(generatedKey, publicKey)
        val iv = ByteArray(KEY_SIZE)
        val svcEncParam = getEncryptedValue(generatedKey, iv, json)
        val svcEncIV = String(Hex.encodeHex(iv))
        val jSONObject = JSONObject()
        jSONObject.put("chkDoNum", chkDoNumString)
        jSONObject.put("svcEncParam", svcEncParam)
        jSONObject.put("svcEncKY", svcEncKY)
        jSONObject.put("svcEncIV", svcEncIV)
        return URLEncoder.encode(
            String(
                Base64.encode(jSONObject.toString().toByteArray(), Base64.NO_WRAP),
                charset(CHAR_SET_NAME)
            ),
            CHAR_SET_NAME
        )
    }

    fun generateCodeChallenge(codeVerifier: String): String? {
        try {
            return base64Encode(
                MessageDigest.getInstance(SHA_ALGORITHM).digest(
                    codeVerifier.toByteArray(
                        charset(CHAR_SET_NAME)
                    )
                )
            ).replace("=".toRegex(), "")
                .replace("\\+".toRegex(), "-")
                .replace("/".toRegex(), "_")
        } catch (exception: Exception) {
            return null
        }
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        UnsupportedEncodingException::class
    )
    private fun getEncryptedKey(key: ByteArray, publicKey: PublicKey): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return String(
            Base64.encode(cipher.doFinal(Base64.encode(key, Base64.NO_WRAP)), Base64.NO_WRAP),
            charset(CHAR_SET_NAME)
        )
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        UnsupportedEncodingException::class
    )
    private fun getEncryptedValue(key: ByteArray, iv: ByteArray, input: String): String {
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, 0, key.size, "AES"),
            IvParameterSpec(iv)
        )
        return String(
            Base64.encode(cipher.doFinal(input.toByteArray()), Base64.NO_WRAP),
            charset(CHAR_SET_NAME)
        )
    }

    private fun getKey(input: String, v: Int): ByteArray {
        val hashedData = hashData(input)
        val secureRandom = SecureRandom()
        val key = ByteArray(KEY_SIZE)
        secureRandom.nextBytes(key)
        return pbkdf2(hashedData!!.toCharArray(), key, v)
    }

    private fun hashData(input: String): String? {
        try {
            return base64Encode(
                MessageDigest.getInstance(SHA_ALGORITHM).digest(
                    input.toByteArray(
                        charset(CHAR_SET_NAME)
                    )
                )
            )
        } catch (exception: Exception) {
            return null
        }
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterationCount: Int): ByteArray {
        val generator = PKCS5S2ParametersGenerator(SHA256Digest())
        generator.init(
            PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password),
            salt,
            iterationCount
        )
        return (generator.generateDerivedMacParameters(0x80) as KeyParameter).key
    }

    fun decrypt(data: String, key: String): String? {
        try {
            val cipher = Cipher.getInstance("AES")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(
                    SecretKeySpec(key.toByteArray(), 0, KEY_SIZE, "AES").encoded,
                    "AES"
                )
            )
            return String(cipher.doFinal(Hex.decodeHex(data.toCharArray())))
        } catch (exception: Exception) {
            return null
        }
    }

    private class SvcParam(
        @SerializedName("birthDate")
        val birthDate: String? = null,
        @SerializedName("clientId")
        val clientId: String? = null,
        @SerializedName("code_challenge")
        val codeChallenge: String? = null,
        @SerializedName("code_challenge_method")
        val codeChallengeMethod: String? = null,
        @SerializedName("competitorDeviceYNFlag")
        val competitorDeviceYNFlag: String? = null,
        @SerializedName("countryCode")
        val countryCode: String? = null,
        @SerializedName("deviceInfo")
        val deviceInfo: String? = null,
        @SerializedName("deviceModelID")
        val deviceModelID: String? = null,
        @SerializedName("deviceName")
        val deviceName: String? = null,
        @SerializedName("deviceOSVersion")
        val deviceOSVersion: String? = null,
        @SerializedName("devicePhysicalAddressText")
        val devicePhysicalAddressText: String? = null,
        @SerializedName("deviceType")
        val deviceType: String? = null,
        @SerializedName("deviceUniqueID")
        val deviceUniqueID: String? = null,
        @SerializedName("firstName")
        val firstName: String? = null,
        @SerializedName("lastName")
        val lastName: String? = null,
        @SerializedName("redirect_uri")
        val redirectUri: String? = null,
        @SerializedName("replaceableClientConnectYN")
        val replaceableClientConnectYN: String? = null,
        @SerializedName("replaceableClientId")
        val replaceableClientId: String? = null,
        @SerializedName("replaceableDevicePhysicalAddressText")
        val replaceableDevicePhysicalAddressText: String? = null,
        @SerializedName("responseEncryptionType")
        val responseEncryptionType: String? = null,
        @SerializedName("responseEncryptionYNFlag")
        val responseEncryptionYNFlag: String? = null,
        @SerializedName("scope")
        val scope: String? = null,
        @SerializedName("state")
        val state: String? = null,
        @SerializedName("svcIptLgnID")
        val svcIptLgnID: String? = null,
        @SerializedName("iosYNFlag")
        val iosYNFlag: String? = null
    )

}

