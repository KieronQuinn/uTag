package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.kieronquinn.app.utag.Application.Companion.PP_VERSION
import com.kieronquinn.app.utag.model.EncryptionKey
import com.kieronquinn.app.utag.model.GeoLocation
import com.kieronquinn.app.utag.model.database.cache.CacheItem.CacheType
import com.kieronquinn.app.utag.networking.model.find.EncryptionKeyRequest
import com.kieronquinn.app.utag.networking.model.find.FindItemRequest
import com.kieronquinn.app.utag.networking.model.smartthings.Agreement
import com.kieronquinn.app.utag.networking.model.smartthings.DevicesResponse
import com.kieronquinn.app.utag.networking.model.smartthings.DisableSmartThingsActionsRequest
import com.kieronquinn.app.utag.networking.model.smartthings.E2eStateRequest
import com.kieronquinn.app.utag.networking.model.smartthings.GeoLocationResponse
import com.kieronquinn.app.utag.networking.model.smartthings.GetLocationResponse
import com.kieronquinn.app.utag.networking.model.smartthings.InstalledAppsRequest.Companion.Method
import com.kieronquinn.app.utag.networking.model.smartthings.LostModeRequestResponse
import com.kieronquinn.app.utag.networking.model.smartthings.RingRequest
import com.kieronquinn.app.utag.networking.model.smartthings.RingRequest.RingCommand
import com.kieronquinn.app.utag.networking.model.smartthings.SendLocationRequest
import com.kieronquinn.app.utag.networking.model.smartthings.SetFavouritesItem
import com.kieronquinn.app.utag.networking.model.smartthings.SetSearchingStatusRequest
import com.kieronquinn.app.utag.networking.model.smartthings.SetSearchingStatusRequest.SearchingStatus
import com.kieronquinn.app.utag.networking.model.smartthings.SetShareableRequest
import com.kieronquinn.app.utag.networking.model.smartthings.ShareableMember
import com.kieronquinn.app.utag.networking.model.smartthings.UserOptionsResponse
import com.kieronquinn.app.utag.networking.services.FindService
import com.kieronquinn.app.utag.networking.services.InstalledAppsService
import com.kieronquinn.app.utag.networking.services.InstalledAppsService.Companion.get
import com.kieronquinn.app.utag.networking.services.InstalledAppsService.Companion.getInstalledAppId
import com.kieronquinn.app.utag.networking.services.InstalledAppsService.Companion.request
import com.kieronquinn.app.utag.networking.services.InstalledAppsService.Companion.send
import com.kieronquinn.app.utag.repositories.ApiRepository.GetLocationHistoryResult
import com.kieronquinn.app.utag.repositories.ApiRepository.GetLocationResult
import com.kieronquinn.app.utag.repositories.EncryptionRepository.DecryptionResult
import com.kieronquinn.app.utag.utils.extensions.Locale_getDefaultWithCountry
import com.kieronquinn.app.utag.utils.extensions.get
import com.kieronquinn.app.utag.utils.extensions.iso3Toiso2Country
import retrofit2.Retrofit
import java.io.IOException

interface ApiRepository {

    companion object {
        const val FIND_PRIVACY_URL =
            "https://smartthingsfind.samsung.com/contents/legal/%s/policy.html"
    }

    /**
     *  Returns the installed app ID for FMM on this SmartThings install
     */
    suspend fun getInstalledAppId(): String?

    /**
     *  This request actually agrees to the location terms and is required to enable location
     *  tracking on Tags that have not previously been tracked by this user using the official app
     */
    suspend fun setShareableMembers(deviceId: String, userId: String): Boolean

    /**
     *  Gets user options, including policy states
     */
    suspend fun getUserOptions(): UserOptionsResponse?

    /**
     *  Allows a new account to work by agreeing to the terms
     */
    suspend fun setTermsAgreed(): Boolean

    /**
     *  Stops sharing location, which removes practically all functionality of the app, so the user
     *  should be warned that uTag will close and that if they open it again they will be opted
     *  back in.
     */
    suspend fun stopSharing(deviceId: String): Boolean

    /**
     *  Sets whether the Tag is shared with other home members
     */
    suspend fun setShareable(deviceId: String, enabled: Boolean): Boolean

    /**
     *  Gets the last known location of this Tag from the server. This can be updated with
     *  [sendLocation]
     */
    suspend fun getLocation(deviceId: String): GetLocationResult

    /**
     *  Updates the location of this Tag to the server
     */
    suspend fun sendLocation(deviceId: String, request: SendLocationRequest): Boolean

    /**
     *  Retrieves location history for this Tag from [startTime] to [endTime], with a [limit] of
     *  results. Max limit is 500.
     */
    suspend fun getLocationHistory(
        deviceId: String,
        startTime: Long,
        endTime: Long,
        limit: Int = 500,
        onProgressChanged: suspend (Float) -> Unit = {}
    ): GetLocationHistoryResult

    /**
     *  Clears **all** location history for this Tag
     */
    suspend fun deleteLocationHistory(deviceId: String): Boolean

    /**
     *  Sets whether the Tag is ringing on the network. This will only take effect when the Tag is
     *  next "seen" by the network, and BLE should be preferred if the Tag is nearby.
     */
    suspend fun setRinging(deviceId: String, ringing: Boolean): Boolean

    /**
     *  Sets whether the Tag is in "searching" mode. Enabling this will send a push notification
     *  to the user when the Tag is next seen by the network.
     */
    suspend fun setSearching(deviceId: String, searching: Boolean): Boolean

    /**
     *  Gets whether lost mode is enabled for this Tag and its associated data such as message,
     *  email and phone number
     */
    suspend fun getLostMode(deviceId: String): LostModeRequestResponse?

    /**
     *  Updates lost mode data for this Tag, including its state
     */
    suspend fun setLostMode(deviceId: String, lostMode: LostModeRequestResponse): Boolean

    /**
     *  Sets whether E2E is enabled for a given device, this requires a PIN to be set first
     */
    suspend fun setE2eEnabled(deviceId: String, enabled: Boolean): Boolean

    /**
     *  Gets encryption key for account, if one is set. This includes the timestamp when the key
     *  was set.
     */
    suspend fun getEncryptionKey(): EncryptionKey?

    /**
     *  Gets a list of FMM devices, whose IDs are used in the favourites system.
     */
    suspend fun getDevices(): DevicesResponse?

    /**
     *  Sets or updates the FMM pin for an account. If no FMM device is added, this will not work.
     */
    suspend fun setPin(
        privateKey: String,
        publicKey: String,
        iv: String
    ): Boolean

    /**
     *  Sets or updates favourites for an account
     */
    suspend fun setFavourites(request: List<SetFavouritesItem>): Boolean

    /**
     *  Disables SmartThings button actions (pet walking & remote ring) for a given [deviceId]
     */
    suspend fun disableSmartThingsButtonActions(deviceId: String): Boolean

    sealed class GetLocationResult(open val cached: Boolean) {
        data class PINRequired(
            val time: Long,
            override val cached: Boolean): GetLocationResult(cached)
        data class Location(
            val location: GeoLocation,
            val isEncrypted: Boolean,
            override val cached: Boolean
        ): GetLocationResult(cached)
        data class NoKeys(
            val time: Long,
            override val cached: Boolean
        ): GetLocationResult(cached)
        data class NotAllowed(override val cached: Boolean): GetLocationResult(cached)
        data class Error(
            val code: Int,
            override val cached: Boolean
        ): GetLocationResult(cached)
        data class NoLocation(
            override val cached: Boolean
        ): GetLocationResult(cached)
    }

    sealed class GetLocationHistoryResult {
        data class Locations(
            val locations: List<GeoLocation>,
            val pinRequired: Boolean
        ): GetLocationHistoryResult()
        data object Error: GetLocationHistoryResult()
    }

}

class ApiRepositoryImpl(
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    private val encryptionRepository: EncryptionRepository,
    private val userRepository: UserRepository,
    private val gson: Gson,
    context: Context,
    retrofit: Retrofit
): ApiRepository {

    companion object {
        private const val URI_TRACKER_API = "/trackerapi"
    }

    private val installedAppsService = InstalledAppsService.createService(context, retrofit)
    private val findService = FindService.createService(context, retrofit)

    private suspend fun getSTCountryCode(): String? {
        return userRepository.getUserInfo()?.countryCode?.iso3Toiso2Country()
            ?: Locale_getDefaultWithCountry().country
    }

    override suspend fun getInstalledAppId(): String? {
        return installedAppsService.getInstalledAppId()
    }

    override suspend fun setShareableMembers(deviceId: String, userId: String): Boolean {
        return installedAppsService.send(
            Method.POST,
            body = ShareableMember(saGuid = userId),
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/shareable/members"
        ) //We don't care about the body, just that it was called
    }

    override suspend fun getUserOptions(): UserOptionsResponse? {
        val countryCode = getSTCountryCode() ?: return null
        return installedAppsService.get<UserOptionsResponse>(
            Method.GET,
            uri = "/user/options",
            extraParameters = mapOf(
                "saCountryCode" to countryCode,
                "fmmAgreement" to "true"
            ),
            type = CacheType.USER_OPTIONS
        )
    }

    override suspend fun setTermsAgreed(): Boolean {
        val countryCode = getSTCountryCode() ?: return false
        return try {
            installedAppsService.request(
                Method.POST,
                uri = "/user/options",
                extraParameters = mapOf(
                    "ppVersion" to PP_VERSION,
                    "saCountryCode" to countryCode,
                    "fmmAgreement" to "true",
                    "encodedAgreement" to Agreement.getAgreement()
                )
            ) != null
        }catch (e: IOException) {
            //Network connection issue, but terms may already be agreed - proceed for now
            return true
        }
    }

    override suspend fun stopSharing(deviceId: String): Boolean {
        val userId = encryptedSettingsRepository.userId.getOrNull() ?: return false
        return installedAppsService.request(
            Method.DELETE,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/shareable/members/$userId"
        ) != null
    }

    override suspend fun setShareable(deviceId: String, enabled: Boolean): Boolean {
        return installedAppsService.request(
            Method.PUT,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/shareable",
            body = SetShareableRequest(enabled)
        ) != null
    }

    override suspend fun getLocation(deviceId: String): GetLocationResult {
        val locationResponse = installedAppsService.get<GeoLocationResponse>(
            Method.GET,
            //This does not use the regular URI, it has its own
            uri = "/trackers/geolocation",
            extraParameters = mapOf("stDids" to deviceId),
            type = CacheType.TAG_LOCATION,
            subType = deviceId,
            convert = { copy(cached = true) }
        ) ?: return GetLocationResult.Error(1401, false)
        val item = locationResponse.items.firstOrNull { it.deviceId == deviceId }
            ?: return GetLocationResult.Error(1404, false)
        if(item.resultCode == 403) return GetLocationResult.NotAllowed(locationResponse.wasFromCache())
        val location = item.geoLocations.firstOrNull()
            ?: return GetLocationResult.NoLocation(locationResponse.wasFromCache())
        val keyPair = locationResponse.keyPairs.firstOrNull()
        val hasKeyPair = keyPair != null
        val cipher = keyPair?.let {
            val pin = encryptionRepository.getPin()
            encryptionRepository.getDecryptionCipher(pin, it)
        }
        val decrypted = encryptionRepository.decryptLocationIfNeeded(location, cipher, hasKeyPair)
        return when(decrypted) {
            is DecryptionResult.Success -> {
                GetLocationResult.Location(
                    decrypted.location, keyPair != null, locationResponse.wasFromCache()
                )
            }
            is DecryptionResult.PINRequired -> {
                GetLocationResult.PINRequired(location.lastUpdateTime, locationResponse.wasFromCache())
            }
            is DecryptionResult.NoKeys -> {
                GetLocationResult.NoKeys(location.lastUpdateTime, locationResponse.wasFromCache())
            }
            is DecryptionResult.Error -> {
                GetLocationResult.Error(1403, locationResponse.wasFromCache())
            }
        }
    }

    override suspend fun sendLocation(deviceId: String, request: SendLocationRequest): Boolean {
        return installedAppsService.request(
            Method.POST,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/geolocations",
            body = request
        ) != null
    }

    override suspend fun getLocationHistory(
        deviceId: String,
        startTime: Long,
        endTime: Long,
        limit: Int,
        onProgressChanged: suspend (Float) -> Unit
    ): GetLocationHistoryResult {
        //Get the current location to retrieve the KeyPair if required
        val keyPair = installedAppsService.get<GeoLocationResponse>(
            Method.GET,
            uri = "/trackers/geolocation",
            extraParameters = mapOf("stDids" to deviceId)
        )?.keyPairs?.firstOrNull()
        val locations = installedAppsService.get<GetLocationResponse>(
            Method.GET,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/geolocations?order=asc&isSummary=false&startTime=$startTime&endTime=$endTime&limit=$limit"
        ) ?: return GetLocationHistoryResult.Error
        var requiresPin = false
        val hasKeyPair = keyPair != null
        val cipher = keyPair?.let {
            val pin = encryptionRepository.getPin()
            encryptionRepository.getDecryptionCipher(pin, it)
        }
        val size = locations.geoLocations.size
        val decryptedLocations = locations.geoLocations.mapIndexedNotNull { index, it ->
            val progress = index / size.toFloat()
            onProgressChanged(progress)
            when(val decrypted = encryptionRepository.decryptLocationIfNeeded(it, cipher, hasKeyPair)) {
                is DecryptionResult.Success -> decrypted.location
                is DecryptionResult.PINRequired, is DecryptionResult.NoKeys -> {
                    requiresPin = true
                    null
                }
                is DecryptionResult.Error -> return GetLocationHistoryResult.Error
            }
        }
        return GetLocationHistoryResult.Locations(decryptedLocations, requiresPin)
    }

    override suspend fun deleteLocationHistory(deviceId: String): Boolean {
        return installedAppsService.request(
            Method.DELETE,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/geolocations"
        ) != null
    }

    override suspend fun setRinging(deviceId: String, ringing: Boolean): Boolean {
        val request = if(ringing) {
            RingRequest(RingCommand.START)
        }else{
            RingRequest(RingCommand.STOP)
        }
        return installedAppsService.request(
            Method.PUT,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/ring",
            body = request
        ) != null
    }

    override suspend fun setSearching(deviceId: String, searching: Boolean): Boolean {
        val request = if (searching) {
            SetSearchingStatusRequest(SearchingStatus.SEARCHING)
        } else {
            SetSearchingStatusRequest(SearchingStatus.STOP)
        }
        return installedAppsService.request(
            Method.PUT,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/searchingstatus",
            body = request
        ) != null
    }

    override suspend fun getLostMode(deviceId: String): LostModeRequestResponse? {
        return installedAppsService.get(
            Method.GET,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/lostmode",
            type = CacheType.LOST_MODE,
            subType = deviceId
        )
    }

    override suspend fun setLostMode(deviceId: String, lostMode: LostModeRequestResponse): Boolean {
        return installedAppsService.request(
            Method.PUT,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/lostmode",
            body = lostMode
        ) != null
    }

    override suspend fun setE2eEnabled(deviceId: String, enabled: Boolean): Boolean {
        return installedAppsService.request(
            Method.PUT,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/e2eencryption",
            body = E2eStateRequest(enabled)
        ) != null
    }

    override suspend fun getEncryptionKey(): EncryptionKey? {
        val userId = encryptedSettingsRepository.userId.getOrNull() ?: return null
        val response = findService.getEncryptionKey(userId)
            .get(CacheType.ENCRYPTION_KEY, name = "getEncryptionKey")
        return when {
            response == null -> return null
            response.item == null -> EncryptionKey.Unset
            else -> EncryptionKey.Set(
                response.item.privateKey,
                response.item.publicKey,
                response.item.iv,
                response.item.timeUpdated
            )
        }
    }

    override suspend fun setPin(
        privateKey: String,
        publicKey: String,
        iv: String
    ): Boolean {
        val userId = encryptedSettingsRepository.userId.getOrNull() ?: return false
        return findService.setEncryptionKey(
            userId,
            FindItemRequest(EncryptionKeyRequest(privateKey, publicKey, iv))
        ).get(name = "setEncryptionKey") != null
    }

    override suspend fun setFavourites(request: List<SetFavouritesItem>): Boolean {
        val body = Base64.encodeToString(gson.toJson(request).toByteArray(), Base64.NO_WRAP)
        return installedAppsService.send(
            Method.POST,
            uri = "/devices/favorites",
            extraParameters = mapOf("encodedFavorites" to body)
        )
    }

    override suspend fun disableSmartThingsButtonActions(deviceId: String): Boolean {
        return installedAppsService.request(
            Method.PUT,
            uri = URI_TRACKER_API,
            extraUri = "/trackers/$deviceId/button/options",
            body = DisableSmartThingsActionsRequest()
        ) != null
    }

    override suspend fun getDevices(): DevicesResponse? {
        return installedAppsService.get(
            Method.GET,
            uri = "/devices",
            type = CacheType.FMM_DEVICES
        )
    }

}