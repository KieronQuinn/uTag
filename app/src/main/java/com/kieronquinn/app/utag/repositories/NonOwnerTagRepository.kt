package com.kieronquinn.app.utag.repositories

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.ParcelUuid
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.ktx.utils.sphericalDistance
import com.kieronquinn.app.utag.Application.Companion.isMainProcess
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.bluetooth.ConnectedTagConnection.Companion.SERVICE_ID
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.model.database.AcknowledgedUnknownTag
import com.kieronquinn.app.utag.model.database.AcknowledgedUnknownTagTable.Companion.getTag
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepository.NonOwnerTag
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepository.UnknownTag
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationId
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagData
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagData.TagStateValue
import com.kieronquinn.app.utag.ui.activities.UnknownTagActivity
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.utils.extensions.delayBy
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.toBoolean
import com.kieronquinn.app.utag.utils.extensions.toDouble
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import com.kieronquinn.app.utag.utils.extensions.toInt
import com.kieronquinn.app.utag.xposed.Xposed.Companion.ACTION_SCAN_RECEIVED
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_RESULT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.xposed.extensions.getParcelableExtraCompat
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.time.Duration
import kotlin.collections.filterValues
import kotlin.collections.groupBy
import kotlin.collections.map
import kotlin.collections.mapValues
import kotlin.collections.set
import com.kieronquinn.app.utag.model.database.UnknownTag as DatabaseUnknownTag

/**
 *  Handles both Unknown Tag scanning (UTS) for notifications if an unknown Tag appears to be
 *  following the user, and sending Tag locations to Chaser if the user has network contributions
 *  set up.
 */
interface NonOwnerTagRepository {

    /**
     *  Called when the service triggers a location update for Tags, at which time non-owner nearby
     *  Tags should be written to the database if they're in the [TagStateValue.OVERMATURE_OFFLINE]
     *  state, and sent to the network if the user has that enabled and set up.
     */
    fun onLocationUpdate()

    /**
     *  Get a list of any currently detected unknown tags. They must meet the minimum requirements
     *  to show a notification to be in this list.
     */
    fun getUnknownTags(): Flow<List<UnknownTag>>

    /**
     *  Acknowledges an Unknown Tag, preventing notifications from being shown again
     */
    fun acknowledgeUnknownTag(privacyId: String)

    /**
     *  Marks an Unknown Tag as safe, preventing it from appearing in the list anymore
     */
    fun markUnknownTagAsSafe(privacyId: String)

    /**
     *  Clears all acknowledgement and safe flags for Unknown Tags
     */
    fun resetUnknownSafeTags()

    /**
     *  Removes stale unknown tags
     */
    suspend fun trimUnknownTags()

    /**
     *  Get acknowledged Unknown Tags for backup
     */
    suspend fun getBackup(): List<AcknowledgedUnknownTag.Backup>

    /**
     *  Restore acknowledged Unknown Tags for backup
     */
    suspend fun restoreBackup(backups: List<AcknowledgedUnknownTag.Backup>)

    data class NonOwnerTag(
        val mac: String?,
        val rssi: Int,
        val tagData: TagData,
        val lastReceivedTime: Long
    )

    @Parcelize
    data class UnknownTag(
        val privacyId: String,
        val isSafe: Boolean,
        val hasAcknowledged: Boolean,
        val detections: List<Detection>
    ): Parcelable {
        @Parcelize
        data class Detection(
            val timestamp: Long,
            val mac: String?,
            val rssi: Int,
            val serviceData: TagData,
            val location: LatLng
        ): Parcelable
    }

}

class NonOwnerTagRepositoryImpl(
    private val chaserRepository: ChaserRepository,
    private val smartTagRepository: SmartTagRepository,
    private val smartThingsRepository: SmartThingsRepository,
    private val notificationRepository: NotificationRepository,
    private val context: Context,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    database: UTagDatabase
): NonOwnerTagRepository {

    companion object {
        private val SERVICE_UUID = ParcelUuid.fromString(SERVICE_ID)
        //How long to keep Tags in cache as "seen" to be used for location updates
        private const val TAG_CACHE_DURATION = 120_000L
        //How long to keep unknown Tags in the database
        private val UNKNOWN_TAG_CACHE_DURATION = Duration.ofHours(24)
        //Delay to allow tag scan to happen first for known Tags
        private const val SCAN_DELAY = 2500L
    }

    private val unknownTagTable = database.unknownTagTable()
    private val acknowledgedUnknownTagTable = database.acknowledgedUnknownTagTable()
    private val scope = MainScope()

    private val networkContributions = encryptedSettingsRepository.networkContributionsEnabled
        .asFlow().stateIn(scope, SharingStarted.Eagerly, null)

    private val utsEnabled = encryptedSettingsRepository.utsScanEnabled
        .asFlow().stateIn(scope, SharingStarted.Eagerly, null)

    private val utsSensitivity = encryptedSettingsRepository.utsSensitivity
        .asFlow().stateIn(scope, SharingStarted.Eagerly, null)

    private val scanResult = context.broadcastReceiverAsFlow(
        IntentFilter(ACTION_SCAN_RECEIVED)
    ).map {
        it.verifySecurity(PACKAGE_NAME_ONECONNECT)
        it.getParcelableExtraCompat(EXTRA_RESULT, ScanResult::class.java)
    }.delayBy(SCAN_DELAY)

    private val unknownTags = combine(
        unknownTagTable.getTags(),
        acknowledgedUnknownTagTable.getTags()
    ) { unknownTags, acknowledged ->
        getUnknownTags(unknownTags, acknowledged)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     *  Privacy ID -> [NonOwnerTag]. Tags are kept for as long as [TAG_CACHE_DURATION]. It's
     *  possible that a Tag may change its ID in this time, in which case a location may be
     *  reported twice. This only happens for Tags that are below the
     *  [TagStateValue.OVERMATURE_OFFLINE] state, so is only an issue for reporting to Chaser.
     */
    private val tagCache = HashMap<String, NonOwnerTag>()

    override fun onLocationUpdate() {
        scope.launch {
            val networkEnabled = networkContributions.firstNotNull()
            val utsEnabled = utsEnabled.firstNotNull()
            //Skip if neither are enabled
            if(!networkEnabled && !utsEnabled) return@launch
            val location = smartThingsRepository.getLocation() ?: return@launch
            val tagCacheEntries = synchronized(tagCache) {
                //Prune old cache before we use the values so we don't get old tags
                pruneTagCache()
                tagCache.values.toList()
            }
            handleTagLocationUpdate(networkEnabled, utsEnabled, tagCacheEntries, location)
        }
    }

    override fun getUnknownTags(): Flow<List<UnknownTag>> {
        return unknownTags.filterNotNull()
    }

    override fun acknowledgeUnknownTag(privacyId: String) {
        scope.launch(Dispatchers.IO) {
            val current = acknowledgedUnknownTagTable.getTag(privacyId)
            acknowledgedUnknownTagTable.insert(
                current.copy(hasAcknowledged = true.toEncryptedValue())
            )
        }
    }

    override fun markUnknownTagAsSafe(privacyId: String) {
        scope.launch(Dispatchers.IO) {
            val current = acknowledgedUnknownTagTable.getTag(privacyId)
            acknowledgedUnknownTagTable.insert(
                current.copy(isSafe = true.toEncryptedValue())
            )
        }
    }

    override fun resetUnknownSafeTags() {
        scope.launch(Dispatchers.IO) {
            acknowledgedUnknownTagTable.clear()
        }
    }

    override suspend fun trimUnknownTags() {
        val lastTimestamp = System.currentTimeMillis() - UNKNOWN_TAG_CACHE_DURATION.toMillis()
        withContext(Dispatchers.IO) {
            unknownTagTable.trim(lastTimestamp)
        }
    }

    override suspend fun getBackup(): List<AcknowledgedUnknownTag.Backup> {
        return withContext(Dispatchers.IO) {
            acknowledgedUnknownTagTable.getTags().first().map {
                it.toBackup()
            }
        }
    }

    override suspend fun restoreBackup(backups: List<AcknowledgedUnknownTag.Backup>) {
        withContext(Dispatchers.IO) {
            val current = acknowledgedUnknownTagTable.getTags().first()
            current.filter {
                val privacyId = String(it.privacyId.bytes)
                backups.any { backup -> backup.privacyId == privacyId }
            }.forEach {
                acknowledgedUnknownTagTable.delete(it.id)
            }
            backups.forEach {
                acknowledgedUnknownTagTable.insert(
                    AcknowledgedUnknownTag(
                        privacyId = it.privacyId.toEncryptedValue(),
                        hasAcknowledged = it.hasAcknowledged?.toEncryptedValue(),
                        isSafe = it.isSafe?.toEncryptedValue()
                    )
                )
            }
        }
    }

    private suspend fun handleTagLocationUpdate(
        networkEnabled: Boolean,
        utsEnabled: Boolean,
        tags: List<NonOwnerTag>,
        location: Location
    ) = withContext(Dispatchers.IO) {
        if(networkEnabled) {
            //Try to sync Tag to network if the user has it set up
            chaserRepository.sendLocations(tags, location)
        }
        val timestamp = System.currentTimeMillis()
        tags.forEach { tag ->
            //Write the Tag to the database if it's overmature
            if(utsEnabled && tag.tagData.tagState == TagStateValue.OVERMATURE_OFFLINE) {
                unknownTagTable.insert(
                    DatabaseUnknownTag(
                        timestamp = timestamp,
                        mac = tag.mac?.toEncryptedValue(),
                        rssi = tag.rssi.toEncryptedValue(),
                        latitude = location.latitude.toEncryptedValue(),
                        longitude = location.longitude.toEncryptedValue(),
                        serviceData = EncryptedValue(tag.tagData.serviceData)
                    )
                )
            }
        }
        //Remove stale Tag cache
        val lastTimestamp = timestamp - UNKNOWN_TAG_CACHE_DURATION.toMillis()
        unknownTagTable.trim(lastTimestamp)
    }

    private fun setupScan() = scope.launch {
        scanResult.filterNotNull().collect {
            onScanResult(it)
        }
    }

    private fun setupUnknownTagNotifications() = scope.launch {
        unknownTags.filterNotNull().collect {
            onUnknownTagsChanged(it)
        }
    }

    private suspend fun onScanResult(scanResult: ScanResult) {
        val tag = decodeScanResult(scanResult) ?: return
        synchronized(tagCache) {
            //Store the Tag in the cache, this will overwrite any with the same ID
            tagCache[tag.tagData.privacyId] = tag
            //Clear old Tags from cache that exceed the cache time
            pruneTagCache()
        }
    }

    /**
     *  Removes Tags from the cache if they were last seen more than 120 seconds ago
     */
    private fun pruneTagCache() {
        val now = System.currentTimeMillis()
        tagCache.entries.removeIf {
            now - it.value.lastReceivedTime > TAG_CACHE_DURATION
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun decodeScanResult(scanResult: ScanResult): NonOwnerTag? {
        val record = scanResult.scanRecord
        val device = scanResult.device
        val serviceData = record?.getServiceData(SERVICE_UUID) ?: return null
        val bleMac = device.address
        val rssi = scanResult.rssi
        val tagData = smartTagRepository.decodeServiceData(serviceData)
        val lastReceivedTime = System.currentTimeMillis()
        val isKnown = smartTagRepository.isKnownPrivacyId(tagData.privacyId)
        return NonOwnerTag(
            bleMac,
            rssi,
            tagData,
            lastReceivedTime
        ).takeUnless { isKnown }
    }

    private suspend fun getUnknownTags(
        tags: List<DatabaseUnknownTag>,
        acknowledged: List<AcknowledgedUnknownTag>
    ): List<UnknownTag> {
        val safeIds = acknowledged.filter {
            it.isSafe?.toBoolean() == true
        }.map {
            String(it.privacyId.bytes)
        }
        val acknowledgedIds = acknowledged.filter {
            it.hasAcknowledged?.toBoolean() == true
        }.map {
            String(it.privacyId.bytes)
        }
        return tags.map {
            UnknownTag.Detection(
                timestamp = it.timestamp,
                mac = it.mac?.let { mac -> String(mac.bytes) },
                rssi = it.rssi.toInt(),
                serviceData = smartTagRepository.decodeServiceData(it.serviceData.bytes),
                location = LatLng(it.latitude.toDouble(), it.longitude.toDouble())
            )
        }.groupBy {
            it.serviceData.privacyId
        }.mapValues {
            it.value.sortedBy { tag -> tag.timestamp }
        }.filterValues {
            it.matchesRequirements()
        }.map {
            UnknownTag(it.key, safeIds.contains(it.key), acknowledgedIds.contains(it.key), it.value)
        }
    }

    private suspend fun List<UnknownTag.Detection>.matchesRequirements(): Boolean {
        if(size < 2) return false //Checks require at least 2 detections
        val sensitivity = utsSensitivity.firstNotNull()
        val duration = maxOf { it.timestamp } - minOf { it.timestamp }
        if(duration < Duration.ofMinutes(sensitivity.duration).toMillis()) return false
        val bounds = LatLngBounds.Builder().apply {
            forEach { include(it.location) }
        }.build()
        val distance = bounds.northeast.sphericalDistance(bounds.southwest)
        return distance >= sensitivity.distance
    }

    private suspend fun onUnknownTagsChanged(unknownTags: List<UnknownTag>) {
        if(!utsEnabled.firstNotNull()) return //UTS is disabled
        val tagsToNotify = unknownTags.filter {
            !it.isSafe && !it.hasAcknowledged
        }
        if(tagsToNotify.isEmpty()) return //Already notified or all safe
        val title = context.resources.getQuantityString(
            R.plurals.notification_title_unknown_tag,
            tagsToNotify.size
        )
        val content = context.resources.getQuantityString(
            R.plurals.notification_content_unknown_tag,
            tagsToNotify.size
        )
        notificationRepository.showNotification(
            NotificationId.UNKNOWN_TAG,
            NotificationChannel.UNKNOWN_TAG
        ) {
            it.setSmallIcon(dev.oneuiproject.oneui.R.drawable.ic_oui_security_2)
            it.setContentTitle(title)
            it.setContentText(content)
            it.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    NotificationId.UNKNOWN_TAG.ordinal,
                    Intent(context, UnknownTagActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                ))
            it.setAutoCancel(true)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    init {
        //We only want to run these from the service process
        if(!isMainProcess()) {
            setupScan()
            setupUnknownTagNotifications()
        }
    }

}