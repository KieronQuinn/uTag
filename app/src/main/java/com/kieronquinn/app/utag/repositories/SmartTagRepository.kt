package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import android.util.Base64
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection
import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.model.ChaserRegion
import com.kieronquinn.app.utag.model.DeviceInfo
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.model.GeoLocation
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.providers.ConnectedSmartspacerComplication
import com.kieronquinn.app.utag.repositories.ApiRepository.GetLocationResult
import com.kieronquinn.app.utag.repositories.SmartTagRepository.Companion.ACTION_REFRESH_TAG_STATES
import com.kieronquinn.app.utag.repositories.SmartTagRepository.Companion.refreshTagStates
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagData
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState.Loaded.LocationState
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState.Loaded.LocationState.Location
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper.RoomEncryptionFailedCallback
import com.kieronquinn.app.utag.xposed.extensions.applySecurity
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.kieronquinn.app.utag.model.database.TagData as CachedTagData

interface SmartTagRepository: RoomEncryptionFailedCallback {

    companion object {
        internal const val ACTION_REFRESH_TAG_STATES =
            "${BuildConfig.APPLICATION_ID}.action.REFRESH_TAG_STATES"

        fun refreshTagStates(context: Context) {
            context.sendBroadcast(Intent(ACTION_REFRESH_TAG_STATES).apply {
                `package` = context.packageName
                applySecurity(context)
            })
        }
    }

    /**
     *  Get the number of Tags that are currently connected
     */
    fun getConnectedTagCount(): Int

    /**
     *  Set the number of connected Tags and update the Smartspacer complication if required
     */
    fun setConnectedTagCount(count: Int)

    /**
     *  Gets the current, server-backed state of a Tag. This will be automatically updated when
     *  [refreshTagStates] is called, when a location update succeeds in [RemoteTagConnection].
     */
    fun getTagState(deviceId: String): Flow<TagState>

    /**
     *  Gets current server state of a Tag, for one-off use (notification images)
     */
    suspend fun getCurrentTagState(deviceId: String): TagState

    /**
     *  If set, [getTagState] will never return PIN Required for its [LocationState], and will
     *  always return [LocationState.NoKeys] instead. Set when the user cancels the PIN entry
     *  dialog, and reset when they enter their PIN (either from a second prompt or resetting it)
     */
    suspend fun setPINSuppressed(suppressed: Boolean)

    suspend fun createTagConnections(): Map<String, RemoteTagConnection>?
    fun getTagConnection(deviceId: String): RemoteTagConnection?
    fun destroyTagConnections()

    /**
     *  Triggers a server-backed refresh of tag states. This does not run a Bluetooth connect, to
     *  save battery on the tag. Call [ConnectedTagConnection.syncLocation] to do that.
     */
    fun refreshTagStates()

    /**
     *  Save a map of device id -> names in the Room database, and notify the foreground service
     *  that the cache has updated. These are used in the foreground notification.
     */
    suspend fun cacheKnownTags(tags: Map<String, String>)

    /**
     *  Save the current service data against the device ID in the database
     */
    suspend fun cacheServiceData(deviceId: String, serviceData: ByteArray, bleMac: String)

    /**
     *  Gets a map of device id hash -> names from the Room database
     */
    fun getKnownTagNames(): Flow<Map<Int, String>>

    /**
     *  Gets the current cached [TagData] for a device with ID [deviceId], or null if none has
     *  been received.
     */
    suspend fun getCachedTagData(deviceId: String): TagData?

    /**
     *  Gets the current cached BLE mac address for a device with ID hash [deviceIdHash], or null
     *  if none has been received.
     */
    suspend fun getCachedBleMac(deviceIdHash: Int): String?

    /**
     *  Decodes raw service data into [TagData]
     */
    fun decodeServiceData(serviceData: ByteArray): TagData

    /**
     *  Returns if a [privacyId] is stored in the database, which is used to know if a Tag is our
     *  own.
     */
    suspend fun isKnownPrivacyId(privacyId: String): Boolean

    /**
     *  Clear known tags in the Room database, for example if the user has logged out
     */
    fun clearKnownTags()

    sealed class TagState(
        open val deviceId: String,
        open val timestamp: Long,
        open val isInPassiveMode: Boolean
    ) {
        data class Loaded(
            override val deviceId: String,
            override val timestamp: Long = System.currentTimeMillis(),
            override val isInPassiveMode: Boolean,
            val device: DeviceInfo,
            val locationState: LocationState?,
        ): TagState(deviceId, timestamp, isInPassiveMode) {
            sealed class LocationState(open val cached: Boolean) {
                data class Location(
                    val latLng: LatLng,
                    val address: String?,
                    val time: Long,
                    val isEncrypted: Boolean,
                    val geoLocation: GeoLocation,
                    override val cached: Boolean
                ): LocationState(cached)
                data class PINRequired(
                    val time: Long,
                    override val cached: Boolean
                ): LocationState(cached)
                data class NoKeys(
                    val time: Long,
                    override val cached: Boolean
                ): LocationState(cached)
                data class NoLocation(
                    override val cached: Boolean
                ): LocationState(cached)
                data class NotAllowed(
                    override val cached: Boolean
                ): LocationState(cached)
            }

            fun getLocation() = locationState as? Location
            fun isPinRequired() = locationState is LocationState.PINRequired
            fun requiresAgreement() = locationState is LocationState.NotAllowed && !device.isOwner
        }
        data class Error(
            override val deviceId: String,
            override val timestamp: Long = System.currentTimeMillis(),
            override val isInPassiveMode: Boolean,
            val code: Int
        ): TagState(deviceId, timestamp, isInPassiveMode)
    }

    @Parcelize
    data class TagData(
        val serviceData: ByteArray,
        val encodedServiceData: String,
        val version: Int,
        val advertisingType: Int,
        val tagState: TagStateValue,
        val privacyId: String,
        val privId: String,
        val region: ChaserRegion?,
        val batteryLevel: BatteryLevel,
        val uwbFlag: Boolean,
        val encryptionFlag: Boolean,
        val motionDetection: Boolean,
        val activityTrackingMode: Boolean,
        val signature: ByteArray,
        val reserved: ByteArray,
        val agingCounter: Int
    ): Parcelable {

        fun getPrivIdForUrl(): String {
            return Base64.encodeToString(
                Base64.decode(privId, Base64.NO_WRAP), Base64.URL_SAFE
            ).trim()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TagData

            if (!serviceData.contentEquals(other.serviceData)) return false
            if (encodedServiceData != other.encodedServiceData) return false
            if (version != other.version) return false
            if (advertisingType != other.advertisingType) return false
            if (tagState != other.tagState) return false
            if (privacyId != other.privacyId) return false
            if (privId != other.privId) return false
            if (region != other.region) return false
            if (batteryLevel != other.batteryLevel) return false
            if (uwbFlag != other.uwbFlag) return false
            if (encryptionFlag != other.encryptionFlag) return false
            if (motionDetection != other.motionDetection) return false
            if (activityTrackingMode != other.activityTrackingMode) return false
            if (!signature.contentEquals(other.signature)) return false
            if (!reserved.contentEquals(other.reserved)) return false
            if (agingCounter != other.agingCounter) return false

            return true
        }

        override fun hashCode(): Int {
            var result = serviceData.contentHashCode()
            result = 31 * result + encodedServiceData.hashCode()
            result = 31 * result + version
            result = 31 * result + advertisingType
            result = 31 * result + tagState.ordinal
            result = 31 * result + privacyId.hashCode()
            result = 31 * result + privId.hashCode()
            result = 31 * result + (region?.ordinal ?: 0)
            result = 31 * result + batteryLevel.ordinal
            result = 31 * result + uwbFlag.hashCode()
            result = 31 * result + encryptionFlag.hashCode()
            result = 31 * result + motionDetection.hashCode()
            result = 31 * result + activityTrackingMode.hashCode()
            result = 31 * result + signature.contentHashCode()
            result = 31 * result + reserved.contentHashCode()
            result = 31 * result + agingCounter
            return result
        }

        enum class TagStateValue(private val value: Int?) {
            /**
             *  Tag has been disconnected from owner device for < 15 minutes
             */
            PREMATURE_OFFLINE(1),
            /**
             *  Tag has been disconnected from owner device for between 15 minutes and 24 hours
             */
            OFFLINE(2),
            /**
             *  Tag has been disconnected from owner device for more than 24 hours
             */
            OVERMATURE_OFFLINE(3),
            /**
             *  Tag is connected to one device, the owner
             */
            ONE_WITH_PAIRED(4),
            /**
             *  Tag is connected to one device, non-owner
             */
            ONE(5),
            /**
             *  Tag is connected to two devices (no distinction for owner)
             */
            TWO(6),
            /**
             *  Tag state is not one of the known values
             */
            UNKNOWN(null);

            /**
             *  Prevention should happen if the Tag is already overmature or may be soon.
             */
            fun shouldPreventOvermatureOffline(): Boolean {
                return this == OVERMATURE_OFFLINE || this == OFFLINE
            }

            /**
             *  Chaser server rejects calls with Tags that are connected or recently disconnected
             */
            fun shouldSendToChaser(): Boolean {
                return this > PREMATURE_OFFLINE && this < ONE_WITH_PAIRED
            }

            companion object {
                fun getTagState(value: Int): TagStateValue {
                    return entries.firstOrNull { it.value == value } ?: UNKNOWN
                }
            }
        }
    }

}

class SmartTagRepositoryImpl(
    private val geocoderRepository: GeocoderRepository,
    private val apiRepository: ApiRepository,
    private val context: Context,
    private val passiveModeRepository: PassiveModeRepository,
    private val contentCreatorRepository: ContentCreatorRepository,
    database: UTagDatabase
): SmartTagRepository, KoinComponent {

    private val scope = MainScope()

    private val refreshTagBus = context.broadcastReceiverAsFlow(
        IntentFilter(ACTION_REFRESH_TAG_STATES)
    ).map {
        it.verifySecurity(BuildConfig.APPLICATION_ID)
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val tagData = database.tagDataTable()
    private val pinSuppressed = MutableStateFlow(false)
    private val tagConnectionLock = Mutex()
    private val tagConnections = HashMap<String, RemoteTagConnection>()
    private val deviceRepository by inject<DeviceRepository>()
    private val tagStates = HashMap<String, StateFlow<TagState?>>()
    private var connectedTagCount = 0

    private val knownPrivacyIds = tagData.getTags().mapLatest { tags ->
        tags.mapNotNull { it.lastServiceData }.map {
            decodeServiceData(it.bytes).privacyId
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun getConnectedTagCount(): Int {
        return connectedTagCount
    }

    override fun setConnectedTagCount(count: Int) {
        connectedTagCount = count
        SmartspacerComplicationProvider.notifyChange(
            context,
            ConnectedSmartspacerComplication::class.java
        )
    }

    @Synchronized
    override fun getTagState(deviceId: String): Flow<TagState> {
        tagStates[deviceId]?.let {
            return it.filterNotNull()
        }
        val tagState = refreshTagBus.map {
            getCurrentTagState(deviceId)
        }
        return combine(
            pinSuppressed,
            tagState.filterNotNull()
        ) { pinSuppressed, state ->
            when {
                !pinSuppressed -> state
                state !is TagState.Loaded -> state
                state.locationState !is LocationState.PINRequired -> state
                else -> {
                    state.copy(locationState = LocationState.NoKeys(
                        state.locationState.time, state.locationState.cached
                    ))
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, null).also {
            tagStates[deviceId] = it
        }.filterNotNull()
    }

    override suspend fun getCurrentTagState(deviceId: String): TagState {
        val passiveModeEnabled = passiveModeRepository.isInPassiveMode(deviceId, true)
        val deviceInfo = deviceRepository.getDeviceInfo(deviceId)
            ?: return TagState.Error(deviceId, isInPassiveMode = passiveModeEnabled, code = 404)
        val apiLocation = contentCreatorRepository.getLocation(deviceId)
            ?: apiRepository.getLocation(deviceId)
        val location = apiLocation.let {
            when(it) {
                is GetLocationResult.Location -> {
                    val latLng = LatLng(it.location.latitude, it.location.longitude)
                    val address = geocoderRepository.geocode(latLng)
                    Location(
                        latLng,
                        address,
                        it.location.time,
                        it.isEncrypted,
                        it.location,
                        it.cached
                    )
                }
                is GetLocationResult.PINRequired -> {
                    LocationState.PINRequired(it.time, it.cached)
                }
                is GetLocationResult.NoKeys -> LocationState.NoKeys(it.time, it.cached)
                is GetLocationResult.NoLocation -> LocationState.NoLocation(it.cached)
                is GetLocationResult.NotAllowed -> LocationState.NotAllowed(it.cached)
                is GetLocationResult.Error -> {
                    return TagState.Error(
                        deviceId, isInPassiveMode = passiveModeEnabled, code = it.code
                    )
                }
            }
        }
        return TagState.Loaded(
            deviceId,
            device = deviceInfo,
            locationState = location,
            isInPassiveMode = passiveModeEnabled
        )
    }

    override suspend fun setPINSuppressed(suppressed: Boolean) {
        pinSuppressed.emit(suppressed)
    }

    override fun refreshTagStates() {
        SmartTagRepository.refreshTagStates(context)
    }

    override suspend fun cacheKnownTags(tags: Map<String, String>) {
        withContext(Dispatchers.IO) {
            val currentTags = tagData.getTags().first()
            tags.forEach {
                val hash = it.key.hashCode()
                val tagData = currentTags.firstOrNull { current -> current.deviceIdHash == hash }
                    ?.copy(name = EncryptedValue(it.value.toByteArray()))
                    ?: CachedTagData(hash, name = EncryptedValue(it.value.toByteArray()))
                this@SmartTagRepositoryImpl.tagData.insert(tagData)
            }
        }
    }

    override suspend fun cacheServiceData(deviceId: String, serviceData: ByteArray, bleMac: String) {
        withContext(Dispatchers.IO) {
            val hash = deviceId.hashCode()
            val tagData = tagData.getTags().first().firstOrNull { current ->
                current.deviceIdHash == hash
            }?.copy(
                lastServiceData = EncryptedValue(serviceData),
                bleMac = bleMac.toEncryptedValue(),
                serviceDataTimestamp = System.currentTimeMillis().toEncryptedValue()
            ) ?: CachedTagData(
                hash,
                lastServiceData = EncryptedValue(serviceData),
                bleMac = bleMac.toEncryptedValue(),
                serviceDataTimestamp = System.currentTimeMillis().toEncryptedValue()
            )
            this@SmartTagRepositoryImpl.tagData.insert(tagData)
        }
    }

    override fun getKnownTagNames(): Flow<Map<Int, String>> {
        return tagData.getTags().map { tags ->
            tags.mapNotNull {
                Pair(
                    it.deviceIdHash,
                    it.name?.bytes?.let { name -> String(name) } ?: return@mapNotNull null
                )
            }.toMap()
        }
    }

    override suspend fun getCachedTagData(deviceId: String): TagData? {
        val current = withContext(Dispatchers.IO) {
            tagData.getTag(deviceId.hashCode())
        } ?: return null
        return decodeServiceData(current.lastServiceData?.bytes ?: return null)
    }

    override suspend fun getCachedBleMac(deviceIdHash: Int): String? {
        val current = withContext(Dispatchers.IO) {
            tagData.getTag(deviceIdHash)
        } ?: return null
        return current.bleMac?.let { String(it.bytes) }
    }

    override suspend fun isKnownPrivacyId(privacyId: String): Boolean {
        return knownPrivacyIds.firstNotNull().contains(privacyId)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun decodeServiceData(serviceData: ByteArray): TagData {
        val version = (serviceData[0].toInt() and 0xF0) shr 4
        val tagStateAndAdvertisementType = serviceData[0].toInt() and 15
        val tagState = tagStateAndAdvertisementType and 7
        val advertisementType = tagStateAndAdvertisementType shr 3 and 1
        val agingCounter = serviceData[1].toInt() and 0xFF or
                ((serviceData[2].toInt() and 0xFF) shl 8) or
                ((serviceData[3].toInt() and 0xFF) shl 16)
        val id = ByteArray(8).apply {
            serviceData.copyInto(this, startIndex = 4, endIndex = 12)
        }
        val privacyId = id.toHexString()
        val privId = Base64.encodeToString(id, Base64.NO_WRAP)
        val regionId = (serviceData[12].toInt() and 0xF0) shr 4
        val flags = serviceData[12].toInt() and 15
        val uwbFlag = flags shr 2 and 1
        val encryptionFlag = flags shr 3 and 1
        val batteryLevel = flags and 3
        val motionDetection = 1 and ((serviceData[13].toInt() and 0xFF) shr 7)
        val reserved = ByteArray(2).apply {
            serviceData.copyInto(this, startIndex = 14, endIndex = 16)
        }
        val activityTrackingMode = (serviceData[15].toInt() and 1) != 0
        val signature = ByteArray(4).apply {
            serviceData.copyInto(this, startIndex = 16, endIndex = 20)
        }
        return TagData(
            serviceData = serviceData,
            encodedServiceData = Base64.encodeToString(serviceData, Base64.NO_WRAP),
            version = version,
            advertisingType = advertisementType,
            tagState = TagData.TagStateValue.getTagState(tagState),
            privacyId = privacyId,
            privId = privId,
            region = ChaserRegion.getRegion(regionId),
            batteryLevel = BatteryLevel.fromIntLevel(batteryLevel),
            uwbFlag = uwbFlag == 1,
            encryptionFlag = encryptionFlag == 1,
            motionDetection = motionDetection == 1,
            activityTrackingMode = activityTrackingMode,
            signature = signature,
            reserved = reserved,
            agingCounter = agingCounter
        )
    }

    override fun clearKnownTags() {
        scope.launch(Dispatchers.IO) {
            tagData.clear()
        }
    }

    override suspend fun createTagConnections(): Map<String, RemoteTagConnection>? {
        if(tagConnections.isNotEmpty()) return tagConnections
        val deviceIds = deviceRepository.getDeviceIds() ?: return null
        synchronized(tagConnectionLock) {
            deviceIds.forEach {
                tagConnections[it] = RemoteTagConnection(it)
            }
        }
        return tagConnections
    }

    override fun getTagConnection(deviceId: String): RemoteTagConnection? {
        return synchronized(tagConnectionLock) {
            tagConnections[deviceId]
        }
    }

    override fun destroyTagConnections() {
        synchronized(tagConnectionLock) {
            tagConnections.forEach { it.value.close() }
            tagConnections.clear()
        }
    }

    override fun onEncryptionFailed() {
        clearKnownTags()
    }

}