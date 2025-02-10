package com.kieronquinn.app.utag.repositories

import com.kieronquinn.app.utag.Application.Companion.CLIENT_ID_FIND
import com.kieronquinn.app.utag.Application.Companion.CLIENT_ID_ONECONNECT
import com.kieronquinn.app.utag.model.database.cache.CacheItem
import com.kieronquinn.app.utag.networking.model.smartthings.ConsentDetails
import com.kieronquinn.app.utag.networking.services.AuthService
import com.kieronquinn.app.utag.networking.services.AuthService.Companion.authenticate
import com.kieronquinn.app.utag.networking.services.AuthService.Companion.authorise
import com.kieronquinn.app.utag.networking.services.AuthService.Companion.token
import com.kieronquinn.app.utag.networking.services.EntryPointService
import com.kieronquinn.app.utag.networking.services.SamsungConsentService
import com.kieronquinn.app.utag.utils.SignInUtils
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.get
import com.kieronquinn.app.utag.utils.extensions.secureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Retrofit

interface AuthRepository {

    val biometricPassed: StateFlow<Boolean>

    fun getState(): String
    fun isLoggedIn(): StateFlow<Boolean?>
    suspend fun handleAuthResponse(authServerUrl: String, authCode: String, username: String): Boolean
    suspend fun generateLoginUrl(): String?
    suspend fun generateLogoutUrl(): String?
    suspend fun getConsentDetails(): ConsentDetails?

    /**
     *  Clears stored tokens, resetting the app state to being logged out. This should be called
     *  either after the user has logged out (via [SignInUtils.generateAuthUrl]) or if the tokens
     *  no longer work, in which case the log out page is skipped because we want the user to
     *  log back in.
     */
    fun clearCredentials()

    /**
     *  BLOCKING CALL, only to be used in interceptor
     */
    fun refreshSmartThingsToken(): Boolean

    /**
     *  BLOCKING CALL, only to be used in interceptor
     */
    fun refreshFindToken(): Boolean

    /**
     *  BLOCKING CALL, only to be used in interceptor
     */
    fun getUserId(): String?

    /**
     *  BLOCKING CALL, only to be used in interceptor
     */
    fun getSmartThingsAuthToken(): String?

    /**
     *  BLOCKING CALL, only to be used in interceptor
     */
    fun getFindAuthToken(): String?

    /**
     *  BLOCKING CALL, only to be used in interceptor
     */
    fun getAuthServerUrl(): String?

    /**
     *  Returns the Android ID - this is randomised on newer Android versions, but remains the same
     *  between launches, and this is what the API expects
     */
    fun getDeviceId(): String

    /**
     *  Called when the Biometric Prompt is passed
     */
    suspend fun onBiometricSuccess()

    /**
     *  Called when the app goes into the background, resetting the biometric state
     */
    fun onAppBackgrounded()

}

class AuthRepositoryImpl(
    private val cacheRepository: CacheRepository,
    private val smartTagRepository: SmartTagRepository,
    private val encryptedStorage: EncryptedSettingsRepository,
    private val storage: SettingsRepository,
    private val widgetRepository: WidgetRepository,
    private val historyWidgetRepository: HistoryWidgetRepository,
    retrofit: Retrofit
): AuthRepository {

    private val scope = MainScope()
    private val state = secureRandom(20)
    private val codeVerifier = secureRandom(43)
    private val entryPointService = EntryPointService.createService(retrofit)
    private val authService = AuthService.createService(retrofit)
    private val consentService = SamsungConsentService.createService(retrofit)
    private val refreshLock = Mutex()

    private val smartThingsToken = encryptedStorage.iotToken.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val findToken = encryptedStorage.offlineAccessToken.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val userId = encryptedStorage.userId.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val authServerUrl = encryptedStorage.authServerUrl.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val biometricPassed = MutableStateFlow(false)

    override fun getState(): String = state

    override fun isLoggedIn() = combine(
        encryptedStorage.userAuthToken.asFlow(),
        encryptedStorage.loginId.asFlow(),
        encryptedStorage.authServerUrl.asFlow(),
        encryptedStorage.offlineAccessToken.asFlow(),
        encryptedStorage.offlineAccessRefreshToken.asFlow(),
        encryptedStorage.iotToken.asFlow(),
        encryptedStorage.iotRefreshToken.asFlow(),
        encryptedStorage.userId.asFlow()
    ) { settings ->
        settings.all { it.isNotBlank() }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun handleAuthResponse(
        authServerUrl: String,
        authCode: String,
        username: String
    ): Boolean {
        return refreshLock.withLock {
            val response = authService.authenticate(
                baseUrl = authServerUrl,
                code = authCode,
                codeVerifier = codeVerifier,
                username = username,
                id = getDeviceId()
            ).get(name = "auth") ?: return@withLock false
            encryptedStorage.userAuthToken.set(response.userAuthToken)
            encryptedStorage.userId.set(response.userId)
            encryptedStorage.loginId.set(username)
            encryptedStorage.authServerUrl.set(authServerUrl)
            if(!getSmartThingsToken()) return@withLock false
            if(!getFindToken()) return@withLock false
            true
        }
    }

    override suspend fun generateLoginUrl() = withContext(Dispatchers.IO) {
        val entryPoint = entryPointService.getEntryPoint().get(name = "entrypoint")
            ?: return@withContext null
        val deviceId = getDeviceId()
        SignInUtils.generateAuthUrl(
            deviceId,
            entryPoint.signInUri,
            entryPoint.publicKey,
            entryPoint.chkDoNum,
            codeVerifier
        )
    }

    override suspend fun generateLogoutUrl() = withContext(Dispatchers.IO) {
        val entryPoint = entryPointService.getEntryPoint().get(name = "entrypoint")
            ?: return@withContext null
        val deviceId = getDeviceId()
        SignInUtils.generateAuthUrl(
            deviceId,
            entryPoint.signOutUri,
            entryPoint.publicKey,
            entryPoint.chkDoNum,
            codeVerifier
        )
    }

    override fun getSmartThingsAuthToken(): String? {
        return runBlocking {
            smartThingsToken.firstNotNull().takeIf { it.isNotBlank() }
        }
    }

    override fun getFindAuthToken(): String? {
        return runBlocking {
            findToken.firstNotNull().takeIf { it.isNotBlank() }
        }
    }

    override fun getAuthServerUrl(): String? {
        return runBlocking {
            authServerUrl.firstNotNull().takeIf { it.isNotBlank() }
        }
    }

    override fun getUserId(): String? {
        return runBlocking {
            userId.firstNotNull().takeIf { it.isNotBlank() }
        }
    }

    override fun clearCredentials() {
        scope.launch {
            encryptedStorage.userAuthToken.clear()
            encryptedStorage.iotToken.clear()
            encryptedStorage.iotRefreshToken.clear()
            encryptedStorage.offlineAccessToken.clear()
            encryptedStorage.offlineAccessRefreshToken.clear()
            encryptedStorage.authServerUrl.clear()
            encryptedStorage.userId.clear()
            //PIN is probably not the same between accounts, so clear it
            encryptedStorage.savedPin.clear()
            //Clear known devices
            smartTagRepository.clearKnownTags()
            //Require policy agreement on sign in again
            storage.hasAgreedToPolicies.clear()
            //Clear widget states
            widgetRepository.updateWidgets()
            historyWidgetRepository.updateWidgets()
            //Clear cache
            cacheRepository.clearCache()
        }
    }

    override suspend fun getConsentDetails(): ConsentDetails? {
        val deviceId = getDeviceId()
        return consentService.getConsentDetails(deviceId = deviceId)
            .get(CacheItem.CacheType.CONSENT_DETAILS, deviceId, name = "consent")?.firstOrNull()
    }

    override fun refreshSmartThingsToken(): Boolean {
        val beforeRefreshToken = encryptedStorage.iotRefreshToken.getSync()
            .takeIf { it.isNotBlank() } ?: return false
        val beforeAuthServerUrl = encryptedStorage.authServerUrl.getSync()
            .takeIf { it.isNotBlank() } ?: return false
        return runBlocking {
            refreshLock.withLock {
                val refreshToken = encryptedStorage.iotRefreshToken.get().takeIf { it.isNotBlank() }
                    ?: return@runBlocking false
                val authServerUrl = encryptedStorage.authServerUrl.get().takeIf { it.isNotBlank() }
                    ?: return@runBlocking false
                if (refreshToken != beforeRefreshToken || authServerUrl != beforeAuthServerUrl) {
                    //This call was beaten by an earlier refresh, which has worked. Just return success.
                    return@runBlocking true
                }
                val response = authService.token(authServerUrl, CLIENT_ID_ONECONNECT, refreshToken)
                    .get(name = "iotToken") ?: return@runBlocking getSmartThingsToken()
                //If refreshing fails, try using the user auth token instead
                encryptedStorage.iotRefreshToken.set(response.refreshToken)
                encryptedStorage.iotToken.set(response.accessToken)
                true
            }
        }
    }

    override fun refreshFindToken(): Boolean {
        val beforeRefreshToken = encryptedStorage.offlineAccessRefreshToken.getSync()
            .takeIf { it.isNotBlank() } ?: return false
        val beforeAuthServerUrl = encryptedStorage.authServerUrl.getSync()
            .takeIf { it.isNotBlank() } ?: return false
        return runBlocking {
            refreshLock.withLock {
                val refreshToken = encryptedStorage.offlineAccessRefreshToken.get()
                    .takeIf { it.isNotBlank() } ?: return@runBlocking false
                val authServerUrl = encryptedStorage.authServerUrl.get().takeIf { it.isNotBlank() }
                    ?: return@runBlocking false
                if (refreshToken != beforeRefreshToken || authServerUrl != beforeAuthServerUrl) {
                    //This call was beaten by an earlier refresh, which has worked. Just return success.
                    return@runBlocking true
                }
                val response = authService.token(authServerUrl, CLIENT_ID_FIND, refreshToken)
                    .get(name = "findToken") ?: return@runBlocking getFindToken()
                //If refreshing fails, try using the user auth token instead
                encryptedStorage.offlineAccessRefreshToken.set(response.refreshToken)
                encryptedStorage.offlineAccessToken.set(response.accessToken)
                true
            }
        }
    }

    /**
     *  Uses user auth token to get a new SmartThings (`iot.client`) token
     */
    private suspend fun getSmartThingsToken(): Boolean {
        val userAuthToken = encryptedStorage.userAuthToken.get().takeIf { it.isNotEmpty() }
            ?: return false
        val authServerUrl = encryptedStorage.authServerUrl.get().takeIf { it.isNotBlank() }
            ?: return false
        val loginId = encryptedStorage.loginId.get().takeIf { it.isNotBlank() }
            ?: return false
        val physicalAddressText = getDeviceId()
        val authoriseResponse = authService.authorise(
            baseUrl = authServerUrl,
            clientId = CLIENT_ID_ONECONNECT,
            userauthToken = userAuthToken,
            codeChallenge = SignInUtils.generateCodeChallenge(codeVerifier) ?: return false,
            physicalAddressText = physicalAddressText,
            scope = "iot.client",
            loginId = loginId
        ).get(name = "iotAuthorise") ?: return false
        val authResponse = authService.token(
            baseUrl = authServerUrl,
            clientId = CLIENT_ID_ONECONNECT,
            code = authoriseResponse.code,
            codeVerifier = codeVerifier,
            physicalAddressText = physicalAddressText
        ).get(name = "iotAuth") ?: return false
        encryptedStorage.iotRefreshToken.set(authResponse.refreshToken)
        encryptedStorage.iotToken.set(authResponse.accessToken)
        return true
    }

    /**
     *  Uses user auth token to get a new Find (`offline.access`) token
     */
    private suspend fun getFindToken(): Boolean {
        val userAuthToken = encryptedStorage.userAuthToken.get().takeIf { it.isNotEmpty() }
            ?: return false
        val authServerUrl = encryptedStorage.authServerUrl.get().takeIf { it.isNotBlank() }
            ?: return false
        val loginId = encryptedStorage.loginId.get().takeIf { it.isNotBlank() }
            ?: return false
        val physicalAddressText = getDeviceId()
        val authoriseResponse = authService.authorise(
            baseUrl = authServerUrl,
            clientId = CLIENT_ID_FIND,
            userauthToken = userAuthToken,
            codeChallenge = SignInUtils.generateCodeChallenge(codeVerifier) ?: return false,
            physicalAddressText = physicalAddressText,
            scope = "offline.access",
            loginId = loginId
        ).get(name = "findAuthorise") ?: return false
        val authResponse = authService.token(
            baseUrl = authServerUrl,
            clientId = CLIENT_ID_FIND,
            code = authoriseResponse.code,
            codeVerifier = codeVerifier,
            physicalAddressText = physicalAddressText
        ).get(name = "findAuth") ?: return false
        encryptedStorage.offlineAccessRefreshToken.set(authResponse.refreshToken)
        encryptedStorage.offlineAccessToken.set(authResponse.accessToken)
        return true
    }

    /**
     *  SmartThings uses the Android ID, which since Android 8.0 has been a random hex string based
     *  on the app's signing key, device and user info. This is fine for it, and would be fine for
     *  uTag too, except that the modded SmartThings APK ends up with the same Android ID as uTag
     *  since it uses the same signature and therefore causes conflicts with the tokens. Instead,
     *  we use a randomly generated hex string of our own, which is persisted to storage.
     */
    override fun getDeviceId(): String {
        return storage.getAndroidId()
    }

    override suspend fun onBiometricSuccess() {
        biometricPassed.emit(true)
    }

    override fun onAppBackgrounded() {
        scope.launch {
            biometricPassed.emit(false)
        }
    }

}