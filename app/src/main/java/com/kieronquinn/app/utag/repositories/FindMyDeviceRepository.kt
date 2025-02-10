package com.kieronquinn.app.utag.repositories

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.database.FindMyDeviceConfig
import com.kieronquinn.app.utag.model.database.FindMyDeviceConfig.Backup
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository.Companion.STOP_INTENT
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository.ErrorState
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.utils.extensions.hasNotificationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface FindMyDeviceRepository {

    companion object {
        const val ACTION_STOP = "${BuildConfig.APPLICATION_ID}.action.STOP_FIND_MY_DEVICE"

        val STOP_INTENT = Intent(ACTION_STOP).apply {
            `package` = BuildConfig.APPLICATION_ID
        }
    }

    /**
     *  Get the current config for a given Tag [deviceId]. If a config doesn't exist, one with the
     *  defaults will be created instead.
     */
    fun getConfig(deviceId: String): Flow<FindMyDeviceConfig>

    /**
     *  One off check for whether Find my Device is enabled for a specific tag, used in the service
     *  before launching the activity, where [getConfig] is used to get the rest of the config.
     */
    suspend fun isEnabled(deviceId: String): Boolean

    /**
     *  Update config for a given Tag
     */
    suspend fun updateConfig(deviceId: String, block: FindMyDeviceConfig.() -> FindMyDeviceConfig)

    /**
     *  Gets the error state to show the user in the settings. This is also used to show/hide the
     *  toggle in the main settings, `!= null` means the toggle will be hidden and the user can only
     *  toggle it in the Find Device settings.
     */
    suspend fun getErrorState(deviceId: String): ErrorState?

    /**
     *  Get all backups from the database
     */
    suspend fun getBackups(): List<Backup>

    /**
     *  Restore backups to the database
     */
    suspend fun restoreBackup(backups: List<Backup>)

    /**
     *  Sends the intent to end any ongoing Find my Device ringing
     */
    fun sendStopIntent()

    enum class ErrorState(
        @StringRes val title: Int,
        @StringRes val content: Int,
        val action: WarningAction,
        val isCritical: Boolean = true
    ) {
        NOTIFICATIONS_DISABLED(
            R.string.tag_more_find_device_notifications_disabled_title,
            R.string.tag_more_find_device_notifications_disabled_content,
            WarningAction.NOTIFICATION_SETTINGS
        ),
        NOTIFICATION_CHANNEL_DISABLED(
            R.string.tag_more_find_device_notifications_disabled_title,
            R.string.tag_more_find_device_notifications_disabled_content_channel,
            WarningAction.NOTIFICATION_CHANNEL_SETTINGS
        ),
        NOTIFICATION_CHANNEL_SILENCED(
            R.string.tag_more_find_device_notifications_silenced_title,
            R.string.tag_more_find_device_notifications_silenced_content,
            WarningAction.NOTIFICATION_CHANNEL_SETTINGS
        ),
        FULL_SCREEN_PERMISSION_REQUIRED(
            R.string.tag_more_find_device_full_screen_permission_required_title,
            R.string.tag_more_find_device_full_screen_permission_required_content,
            WarningAction.FULL_SCREEN_INTENT_SETTINGS
        ),
        PET_WALKING_ENABLED(
            R.string.tag_more_find_device_pet_walking_enabled_title,
            R.string.tag_more_find_device_pet_walking_enabled_content,
            WarningAction.DISABLE_PET_WALKING,
            isCritical = false
        ),
        ENABLED_ON_OTHER_DEVICE(
            R.string.tag_more_find_device_remote_ring_enabled_title,
            R.string.tag_more_find_device_remote_ring_enabled_content,
            WarningAction.DISABLE_REMOTE_RING,
            isCritical = false
        );
    }

    enum class WarningAction(@StringRes val label: Int) {
        NOTIFICATION_SETTINGS(R.string.tag_more_find_device_notifications_disabled_action),
        NOTIFICATION_CHANNEL_SETTINGS(R.string.tag_more_find_device_notifications_disabled_action),
        FULL_SCREEN_INTENT_SETTINGS(R.string.tag_more_find_device_full_screen_permission_required_action),
        DISABLE_PET_WALKING(R.string.tag_more_find_device_pet_walking_disable_action),
        DISABLE_REMOTE_RING(R.string.tag_more_find_device_remote_ring_disable_action),
    }

}

class FindMyDeviceRepositoryImpl(
    private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val deviceRepository: DeviceRepository,
    database: UTagDatabase
): FindMyDeviceRepository {

    private val table = database.findMyDeviceConfigTable()

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun getConfig(deviceId: String): Flow<FindMyDeviceConfig> {
        val hash = deviceId.hashCode()
        return table.getConfigAsFlow(hash).map {
            it ?: FindMyDeviceConfig(hash)
        }
    }

    override suspend fun isEnabled(deviceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            table.getConfig(deviceId.hashCode())?.enabled ?: false
        }
    }

    override suspend fun updateConfig(
        deviceId: String,
        block: FindMyDeviceConfig.() -> FindMyDeviceConfig
    ) {
        val hash = deviceId.hashCode()
        withContext(Dispatchers.IO) {
            val current = table.getConfig(hash) ?: FindMyDeviceConfig(hash)
            table.insert(block(current))
        }
    }

    override suspend fun getErrorState(deviceId: String): ErrorState? {
        val deviceInfo = deviceRepository.getDeviceInfo(deviceId)
        if(!context.hasNotificationPermission()) return ErrorState.NOTIFICATIONS_DISABLED
        if(!notificationManager.areNotificationsEnabled()) return ErrorState.NOTIFICATIONS_DISABLED
        val channel = notificationRepository
            .createNotificationChannel(NotificationChannel.FIND_DEVICE)
        if(channel.importance == NotificationManager.IMPORTANCE_NONE) {
            return ErrorState.NOTIFICATION_CHANNEL_DISABLED
        }
        if(channel.importance < NotificationManager.IMPORTANCE_HIGH) {
            return ErrorState.NOTIFICATION_CHANNEL_SILENCED
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!notificationManager.canUseFullScreenIntent()) {
                return ErrorState.FULL_SCREEN_PERMISSION_REQUIRED
            }
        }
        if(deviceInfo?.petWalkingEnabled == true) return ErrorState.PET_WALKING_ENABLED
        if(deviceInfo?.remoteRingEnabled == true) return ErrorState.ENABLED_ON_OTHER_DEVICE
        return null
    }

    override suspend fun getBackups(): List<Backup> {
        return withContext(Dispatchers.IO) {
            table.getConfigs().map { it.toBackup() }
        }
    }

    override suspend fun restoreBackup(backups: List<Backup>) {
        withContext(Dispatchers.IO) {
            backups.forEach {
                table.insert(it.toConfig())
            }
        }
    }

    override fun sendStopIntent() {
        context.sendBroadcast(STOP_INTENT)
    }

}