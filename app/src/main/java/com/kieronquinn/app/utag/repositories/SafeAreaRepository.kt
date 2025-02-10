package com.kieronquinn.app.utag.repositories

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import androidx.annotation.StringRes
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.ExitBuffer
import com.kieronquinn.app.utag.model.database.LocationSafeArea
import com.kieronquinn.app.utag.model.database.NotifyDisconnectConfig
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.model.database.WiFiSafeArea
import com.kieronquinn.app.utag.receivers.GeofenceReceiver
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.NotificationRepository.PendingIntentId
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea.Location
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeArea.WiFi
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeAreaResult
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.WarningState
import com.kieronquinn.app.utag.utils.extensions.await
import com.kieronquinn.app.utag.utils.extensions.currentWiFiInfo
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.getMacAddressOrNull
import com.kieronquinn.app.utag.utils.extensions.getSSIDOrNull
import com.kieronquinn.app.utag.utils.extensions.hasBackgroundLocationPermission
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import com.kieronquinn.app.utag.utils.extensions.hasNotificationPermission
import com.kieronquinn.app.utag.utils.extensions.toBoolean
import com.kieronquinn.app.utag.utils.extensions.toDouble
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import com.kieronquinn.app.utag.utils.extensions.toEnum
import com.kieronquinn.app.utag.utils.extensions.toFloat
import com.kieronquinn.app.utag.utils.extensions.toLocation
import com.kieronquinn.app.utag.utils.extensions.toLong
import com.kieronquinn.app.utag.utils.extensions.toStringSet
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper.RoomEncryptionFailedCallback
import com.kieronquinn.app.utag.xposed.extensions.getLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.location.Location as AndroidLocation

interface SafeAreaRepository: RoomEncryptionFailedCallback {

    /**
     *  Get database-backed list of decrypted safe areas. This is not process safe.
     */
    fun getSafeAreas(): Flow<List<SafeArea>>

    /**
     *  Get database-backed state of notify disconnect for a given [deviceId]
     */
    fun isNotifyDisconnectEnabled(deviceId: String): Flow<Boolean>

    /**
     *  Sets whether notify disconnect is enabled for a given [deviceId]
     */
    suspend fun setNotifyDisconnectEnabled(deviceId: String, enabled: Boolean)

    /**
     *  Get database-backed state of whether to show a map image for a given [deviceId]
     */
    fun isShowImageEnabled(deviceId: String): Flow<Boolean>

    /**
     *  Sets whether to show a map image in the notification for a given [deviceId]
     */
    suspend fun setShowImageEnabled(deviceId: String, enabled: Boolean)

    /**
     *  Add or update a safe area in the database
     */
    suspend fun updateSafeArea(safeArea: SafeArea)

    /**
     *  Deletes a given WiFi Safe Area by ID
     */
    suspend fun deleteWiFiSafeArea(id: String)

    /**
     *  Deletes a given Location Safe Area by ID
     */
    suspend fun deleteLocationSafeArea(id: String)

    /**
     *  Checks if a given device is in a safe area currently, or that the exit time is within the
     *  buffer
     */
    suspend fun isDeviceInSafeArea(deviceId: String): Boolean

    /**
     *  Sets up listeners for the WiFi & Location Safe Areas and updates the database accordingly.
     *
     *  When a Tag disconnects, the database can be queried to see if it's in a Safe Area.
     */
    suspend fun setupSafeAreaListeners(): SafeAreaResult

    /**
     *  Manually update the safe areas for whether they're active - this is required on start to
     *  make sure everything is in sync
     */
    suspend fun manuallyUpdateSafeAreas(): SafeAreaResult

    /**
     *  Called when the Geofencing Client broadcasts an event
     */
    suspend fun onGeofenceEvent(event: Int, ids: List<String>)

    /**
     *  Gets the warning state to show the user in the settings. This is also used to show/hide the
     *  toggle in the main settings, `!= null` means the toggle will be hidden and the user can only
     *  toggle it in the notify disconnect settings.
     */
    suspend fun getWarningState(): WarningState?

    /**
     *  Gets all Location Safe Area backups from the database
     */
    suspend fun getLocationBackups(): List<LocationSafeArea.Backup>

    /**
     *  Restores Location Safe Area backups to the database
     */
    suspend fun restoreLocationBackup(backup: List<LocationSafeArea.Backup>)

    /**
     *  Gets all WiFi Safe Area backups from the database
     */
    suspend fun getWiFiBackups(): List<WiFiSafeArea.Backup>

    /**
     *  Restores WiFi Safe Area backups to the database
     */
    suspend fun restoreWiFiBackup(backup: List<WiFiSafeArea.Backup>)

    /**
     *  Gets all Notify Disconnect backups from the database
     */
    suspend fun getNotifyDisconnectConfigBackups(): List<NotifyDisconnectConfig.Backup>

    /**
     *  Restores Notify Disconnect backups to the database
     */
    suspend fun restoreNotifyDisconnectConfigBackup(backup: List<NotifyDisconnectConfig.Backup>)

    sealed class SafeArea(
        open val id: String,
        open val name: String,
        open val isActive: Boolean,
        open val lastExitTimestamp: Long,
        open val exitBuffer: ExitBuffer,
        open val activeDeviceIds: Set<String>
    ) {
        data class Location(
            override val id: String,
            override val name: String,
            override val isActive: Boolean,
            override val lastExitTimestamp: Long,
            override val exitBuffer: ExitBuffer,
            override val activeDeviceIds: Set<String>,
            val latLng: LatLng,
            val radius: Float
        ): SafeArea(
            id,
            name,
            isActive,
            lastExitTimestamp,
            exitBuffer,
            activeDeviceIds
        ) {
            fun matches(other: AndroidLocation): Boolean {
                val distance = latLng.toLocation().distanceTo(other)
                return distance <= radius
            }

            override fun copyWithDeviceIds(activeDeviceIds: Set<String>): Location {
                return copy(activeDeviceIds = activeDeviceIds)
            }
        }

        data class WiFi(
            override val id: String,
            override val name: String,
            override val isActive: Boolean,
            override val lastExitTimestamp: Long,
            override val exitBuffer: ExitBuffer,
            override val activeDeviceIds: Set<String>,
            val ssid: String,
            val mac: String?
        ): SafeArea(
            id,
            name,
            isActive,
            lastExitTimestamp,
            exitBuffer,
            activeDeviceIds
        ) {
            fun matches(wifiInfo: WifiInfo?): Boolean {
                return wifiInfo != null && wifiInfo.getSSIDOrNull() == ssid &&
                        (mac == null || mac == wifiInfo.getMacAddressOrNull())
            }

            override fun copyWithDeviceIds(activeDeviceIds: Set<String>): WiFi {
                return copy(activeDeviceIds = activeDeviceIds)
            }
        }

        abstract fun copyWithDeviceIds(activeDeviceIds: Set<String>): SafeArea
    }

    enum class SafeAreaResult {
        SUCCESS,
        FAILED_NO_PERMISSIONS,
        FAILED_TO_GET_LOCATION
    }

    enum class WarningState(
        @StringRes val title: Int,
        @StringRes val content: Int,
        val action: WarningAction,
        val isCritical: Boolean = true
    ) {
        NOTIFICATIONS_DISABLED(
            R.string.tag_more_notify_when_disconnected_notifications_disabled_title,
            R.string.tag_more_notify_when_disconnected_notifications_disabled_content,
            WarningAction.NOTIFICATION_SETTINGS
        ),
        NOTIFICATION_CHANNEL_DISABLED(
            R.string.tag_more_find_device_notifications_disabled_title,
            R.string.tag_more_notify_when_disconnected_notifications_disabled_content_channel,
            WarningAction.NOTIFICATION_CHANNEL_SETTINGS
        ),
        NOTIFICATION_CHANNEL_SILENCED(
            R.string.tag_more_notify_when_disconnected_notifications_silenced_title,
            R.string.tag_more_notify_when_disconnected_notifications_silenced_content,
            WarningAction.NOTIFICATION_CHANNEL_SETTINGS,
            isCritical = false
        ),
        DO_NOT_DISTURB_ENABLED(
            R.string.tag_more_notify_when_disconnected_notifications_dnd_title,
            R.string.tag_more_notify_when_disconnected_notifications_dnd_content,
            WarningAction.NOTIFICATION_CHANNEL_SETTINGS,
            isCritical = false
        );
    }

    enum class WarningAction(@StringRes val label: Int) {
        NOTIFICATION_SETTINGS(R.string.tag_more_notify_when_disconnected_notifications_disabled_action),
        NOTIFICATION_CHANNEL_SETTINGS(R.string.tag_more_notify_when_disconnected_notifications_disabled_action)
    }

}

class SafeAreaRepositoryImpl(
    private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    database: UTagDatabase
): SafeAreaRepository {

    private val scope = MainScope()
    private val locationTable = database.locationSafeAreaTable()
    private val wifiTable = database.wifiSafeAreaTable()
    private val notifyDisconnectTable = database.notifyDisconnectTable()
    private val geofenceEvents = MutableSharedFlow<GeofenceEvent>()
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var geofenceChangedJob: Job? = null
    private var wifiChangedJob: Job? = null

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val geofencePendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            PendingIntentId.GEOFENCE.ordinal,
            Intent(context, GeofenceReceiver::class.java),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val location = locationTable.getSafeAreas().mapLatest { list ->
        list.map {
            Location(
                id = it.id,
                name = String(it.name.bytes),
                isActive = it.isActive.toBoolean(),
                exitBuffer = it.exitBuffer.toEnum(),
                lastExitTimestamp = it.lastExitTimestamp.toLong(),
                activeDeviceIds = it.activeDeviceIds.toStringSet(),
                latLng = LatLng(it.latitude.toDouble(), it.longitude.toDouble()),
                radius = it.radius.toFloat()
            )
        }
    }.flowOn(Dispatchers.IO)

    private val wifi = wifiTable.getSafeAreas().mapLatest { list ->
        list.map {
            WiFi(
                id = it.id,
                name = String(it.name.bytes),
                isActive = it.isActive.toBoolean(),
                exitBuffer = it.exitBuffer.toEnum(),
                lastExitTimestamp = it.lastExitTimestamp.toLong(),
                activeDeviceIds = it.activeDeviceIds.toStringSet(),
                ssid = String(it.ssid.bytes),
                mac = it.mac?.let { mac -> String(mac.bytes) }
            )
        }
    }.flowOn(Dispatchers.IO)

    private val safeAreas = combine(location, wifi) { location, wifi ->
        location + wifi
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)

    private val notifyDisconnectHashes = notifyDisconnectTable.getConfigs().map { configs ->
        configs.filter { it.notifyDisconnect.toBoolean() }.map { it.deviceIdHash }
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)

    override fun getSafeAreas(): Flow<List<SafeArea>> {
        return safeAreas.filterNotNull()
    }

    override fun isNotifyDisconnectEnabled(deviceId: String): Flow<Boolean> {
        return notifyDisconnectHashes.filterNotNull().map {
            it.contains(deviceId.hashCode())
        }
    }

    override suspend fun setNotifyDisconnectEnabled(deviceId: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            val hash = deviceId.hashCode()
            val current = notifyDisconnectTable.getConfig(hash)
                ?: NotifyDisconnectConfig(
                    deviceId.hashCode(),
                    true.toEncryptedValue(),
                    true.toEncryptedValue()
                )
            notifyDisconnectTable.insert(current.copy(notifyDisconnect = enabled.toEncryptedValue()))
        }
    }

    override fun isShowImageEnabled(deviceId: String): Flow<Boolean> {
        val hash = deviceId.hashCode()
        return notifyDisconnectTable.getConfigs().mapLatest {
            it.firstOrNull { config -> config.deviceIdHash == hash }?.showImage?.toBoolean() ?: true
        }
    }

    override suspend fun setShowImageEnabled(deviceId: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            val hash = deviceId.hashCode()
            val current = notifyDisconnectTable.getConfig(hash)
                ?: NotifyDisconnectConfig(
                    deviceId.hashCode(),
                    true.toEncryptedValue(),
                    true.toEncryptedValue()
                )
            notifyDisconnectTable.insert(current.copy(showImage = enabled.toEncryptedValue()))
        }
    }

    override suspend fun updateSafeArea(safeArea: SafeArea) {
        with(safeArea) {
            when(this) {
                is Location -> {
                    val area = LocationSafeArea(
                        id = id,
                        name = name.toEncryptedValue(),
                        latitude = latLng.latitude.toEncryptedValue(),
                        longitude = latLng.longitude.toEncryptedValue(),
                        radius = radius.toEncryptedValue(),
                        isActive = isActive.toEncryptedValue(),
                        lastExitTimestamp = lastExitTimestamp.toEncryptedValue(),
                        exitBuffer = exitBuffer.toEncryptedValue(),
                        activeDeviceIds = activeDeviceIds.toEncryptedValue()
                    )
                    withContext(Dispatchers.IO) {
                        locationTable.insert(area)
                    }
                }
                is WiFi -> {
                    val area = WiFiSafeArea(
                        id = id,
                        name = name.toEncryptedValue(),
                        ssid = ssid.toEncryptedValue(),
                        mac = mac?.toEncryptedValue(),
                        isActive = isActive.toEncryptedValue(),
                        lastExitTimestamp = lastExitTimestamp.toEncryptedValue(),
                        exitBuffer = exitBuffer.toEncryptedValue(),
                        activeDeviceIds = activeDeviceIds.toEncryptedValue()
                    )
                    withContext(Dispatchers.IO) {
                        wifiTable.insert(area)
                    }
                }
            }
        }
    }

    override suspend fun deleteWiFiSafeArea(id: String) {
        withContext(Dispatchers.IO) {
            wifiTable.delete(id)
        }
    }

    override suspend fun deleteLocationSafeArea(id: String) {
        withContext(Dispatchers.IO) {
            locationTable.delete(id)
        }
    }

    override suspend fun isDeviceInSafeArea(deviceId: String): Boolean {
        return safeAreas.firstNotNull().any {
            when {
                !it.activeDeviceIds.contains(deviceId) -> false
                it.isActive -> true
                else -> it.isInBuffer()
            }
        }
    }

    private fun SafeArea.isInBuffer(): Boolean {
        //Check if we're within the buffer. Default exit timestamp is 0.
        val timestamp = System.currentTimeMillis()
        val buffer = exitBuffer.minutes * 60_000L
        return (timestamp - lastExitTimestamp) <= buffer
    }

    override suspend fun setupSafeAreaListeners(): SafeAreaResult {
        geofenceChangedJob?.cancel()
        wifiChangedJob?.cancel()
        geofencingClient.removeGeofences(geofencePendingIntent).await()
        if(safeAreas.firstNotNull().isEmpty()) return SafeAreaResult.SUCCESS //Nothing to set up
        //Location is required for both location & WiFi
        if(!context.hasLocationPermissions()) return SafeAreaResult.FAILED_NO_PERMISSIONS
        if(!context.hasBackgroundLocationPermission()) return SafeAreaResult.FAILED_NO_PERMISSIONS
        geofenceChangedJob = scope.launch {
            geofenceEvents.collect { event ->
                val safeAreas = safeAreas.firstNotNull()
                safeAreas.filterIsInstance<Location>().filter { event.matches(it) }.forEach {
                    val isActive = when(event.event) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> true
                        Geofence.GEOFENCE_TRANSITION_EXIT -> false
                        else -> return@forEach
                    }
                    val isChange = isActive != it.isActive
                    val lastExitTimestamp = if(isChange && !isActive) {
                        System.currentTimeMillis()
                    }else it.lastExitTimestamp
                    updateSafeArea(
                        it.copy(isActive = isActive, lastExitTimestamp = lastExitTimestamp)
                    )
                }
            }
        }
        wifiChangedJob = scope.launch {
            connectivityManager.currentWiFiInfo().collect { wifiInfo ->
                val safeAreas = safeAreas.firstNotNull()
                safeAreas.filterIsInstance<WiFi>().forEach {
                    val isActive = it.matches(wifiInfo)
                    val isChange = isActive != it.isActive
                    val lastExitTimestamp = if(isChange && !isActive) {
                        System.currentTimeMillis()
                    }else it.lastExitTimestamp
                    updateSafeArea(
                        it.copy(isActive = isActive, lastExitTimestamp = lastExitTimestamp)
                    )
                }
            }
        }
        val locations = safeAreas.firstNotNull().filterIsInstance<Location>()
        if(locations.isNotEmpty()) {
            locations.addGeofences()
        }
        return SafeAreaResult.SUCCESS
    }

    @SuppressLint("MissingPermission")
    private suspend fun List<Location>.addGeofences() = forEach {
        val geofence = Geofence.Builder().apply {
            setRequestId(it.id)
            setCircularRegion(it.latLng.latitude, it.latLng.longitude, it.radius)
            setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
        }.build()
        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT
            )
            addGeofence(geofence)
        }.build()
        geofencingClient.addGeofences(request, geofencePendingIntent).await()
    }

    override suspend fun manuallyUpdateSafeAreas(): SafeAreaResult {
        val safeAreas = safeAreas.firstNotNull().ifEmpty { null }
            ?: return SafeAreaResult.SUCCESS //Nothing to update
        //Location is required for both location & WiFi
        if(!context.hasLocationPermissions()) return SafeAreaResult.FAILED_NO_PERMISSIONS
        if(!context.hasBackgroundLocationPermission()) return SafeAreaResult.FAILED_NO_PERMISSIONS
        var result = SafeAreaResult.SUCCESS
        val locationSafeAreas = safeAreas.filterIsInstance<Location>()
        val wifiSafeAreas = safeAreas.filterIsInstance<WiFi>()
        if(locationSafeAreas.isNotEmpty()) {
            val staleness = encryptedSettingsRepository.locationStaleness.get()
            val location = context.getLocation(staleness = staleness).first()
            if(location != null) {
                locationSafeAreas.forEach { locationSafeArea ->
                    val isActive = locationSafeArea.matches(location)
                    updateSafeArea(locationSafeArea.copy(isActive = isActive))
                }
            }else{
                result = SafeAreaResult.FAILED_TO_GET_LOCATION
            }
        }
        if(wifiSafeAreas.isNotEmpty()) {
            val wifiInfo = connectivityManager.currentWiFiInfo().first()
            if(wifiInfo != null) {
                wifiSafeAreas.forEach { wifiSafeArea ->
                    val isActive = wifiSafeArea.matches(wifiInfo)
                    updateSafeArea(wifiSafeArea.copy(isActive = isActive))
                }
            }
        }
        return result
    }

    override suspend fun onGeofenceEvent(event: Int, ids: List<String>) {
        geofenceEvents.emit(GeofenceEvent(event, ids))
    }

    override suspend fun getWarningState(): WarningState? {
        if(!context.hasNotificationPermission()) return WarningState.NOTIFICATIONS_DISABLED
        if(!notificationManager.areNotificationsEnabled()) return WarningState.NOTIFICATIONS_DISABLED
        val channel = notificationRepository
            .createNotificationChannel(NotificationChannel.LEFT_BEHIND)
        if(channel.importance == NotificationManager.IMPORTANCE_NONE) {
            return WarningState.NOTIFICATION_CHANNEL_DISABLED
        }
        //Whether the notification pops up is up to the user, so we only check for default here
        if(channel.importance < NotificationManager.IMPORTANCE_DEFAULT) {
            return WarningState.NOTIFICATION_CHANNEL_SILENCED
        }
        if(notificationManager.currentInterruptionFilter > INTERRUPTION_FILTER_ALL &&
            !channel.canBypassDnd()) {
            return WarningState.DO_NOT_DISTURB_ENABLED
        }
        return null
    }

    override suspend fun getLocationBackups(): List<LocationSafeArea.Backup> {
        return withContext(Dispatchers.IO) {
            locationTable.getSafeAreas().first().map {
                it.toBackup()
            }
        }
    }

    override suspend fun restoreLocationBackup(backup: List<LocationSafeArea.Backup>) {
        withContext(Dispatchers.IO) {
            backup.forEach {
                locationTable.insert(it.toLocationSafeArea())
            }
        }
    }

    override suspend fun getWiFiBackups(): List<WiFiSafeArea.Backup> {
        return withContext(Dispatchers.IO) {
            wifiTable.getSafeAreas().first().map {
                it.toBackup()
            }
        }
    }

    override suspend fun restoreWiFiBackup(backup: List<WiFiSafeArea.Backup>) {
        withContext(Dispatchers.IO) {
            backup.forEach {
                wifiTable.insert(it.toWiFiSafeArea())
            }
        }
    }

    override suspend fun getNotifyDisconnectConfigBackups(): List<NotifyDisconnectConfig.Backup> {
        return withContext(Dispatchers.IO) {
            notifyDisconnectTable.getConfigs().first().map {
                it.toBackup()
            }
        }
    }

    override suspend fun restoreNotifyDisconnectConfigBackup(backup: List<NotifyDisconnectConfig.Backup>) {
        withContext(Dispatchers.IO) {
            backup.forEach {
                notifyDisconnectTable.insert(it.toConfig())
            }
        }
    }

    override fun onEncryptionFailed() {
        //Clear encrypted tables
        scope.launch {
            locationTable.clear()
            wifiTable.clear()
            notifyDisconnectTable.clear()
        }
    }

    private data class GeofenceEvent(val event: Int, val ids: List<String>) {
        fun matches(location: Location): Boolean {
            return ids.contains(location.id)
        }
    }

}