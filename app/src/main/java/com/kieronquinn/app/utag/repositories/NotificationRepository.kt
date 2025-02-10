package com.kieronquinn.app.utag.repositories

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationId
import com.kieronquinn.app.utag.ui.activities.MainActivity
import com.kieronquinn.app.utag.utils.extensions.getContentText
import com.kieronquinn.app.utag.utils.extensions.getStyle
import com.kieronquinn.app.utag.utils.extensions.hasNotificationPermission
import com.kieronquinn.app.utag.xposed.extensions.isInForeground
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.NotificationChannel as AndroidNotificationChannel

interface NotificationRepository {

    /**
     *  Create a notification built with [builder]
     */
    fun createNotification(
        channel: NotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): Notification

    /**
     *  As [createNotification], but also shows it immediately
     */
    fun showNotification(
        id: NotificationId,
        channel: NotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): Notification

    /**
     *  As [showNotification], but uses a raw ID
     */
    fun showNotification(
        id: Int,
        channel: NotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): Notification

    /**
     *  Creates and returns a created [AndroidNotificationChannel] from [channel]
     */
    fun createNotificationChannel(channel: NotificationChannel): AndroidNotificationChannel

    /**
     *  Gets an already created channel, if it exists
     */
    fun getNotificationChannel(channel: NotificationChannel): AndroidNotificationChannel?

    /**
     *  Cancels a previously shown notification with a given [id]
     */
    fun cancelNotification(id: NotificationId, delay: Long = 0L)

    /**
     *  As [cancelNotification], but uses a raw ID
     */
    fun cancelNotification(id: Int, delay: Long = 0L)

    /**
     *  If the app is not visible, shows a notification telling the user they've been logged out
     *  and to log in again.
     */
    fun showLoggedOutNotificationIfNeeded()

    enum class NotificationChannel(
        val id: String,
        val importance: Int,
        val titleRes: Int,
        val descRes: Int,
        val options: AndroidNotificationChannel.() -> Unit = {}
    ) {
        FOREGROUND_SERVICE(
            "foreground_service",
            NotificationManager.IMPORTANCE_LOW,
            R.string.notification_channel_foreground_service_title,
            R.string.notification_channel_foreground_service_content,
            options = { setShowBadge(false) }
        ),
        UPDATES (
            "updates",
            NotificationManager.IMPORTANCE_HIGH,
            R.string.notification_channel_updates_title,
            R.string.notification_channel_updates_content
        ),
        ERROR(
            "error",
            NotificationManager.IMPORTANCE_HIGH,
            R.string.notification_channel_error_title,
            R.string.notification_channel_error_content
        ),
        FIND_DEVICE(
            "find_device",
            NotificationManager.IMPORTANCE_HIGH,
            R.string.notification_channel_find_title,
            R.string.notification_channel_find_content,
            options = {
                enableVibration(false)
                setSound(null, null)
            }
        ),
        LEFT_BEHIND(
            "left_behind",
            NotificationManager.IMPORTANCE_HIGH,
            R.string.notification_channel_left_behind_title,
            R.string.notification_channel_left_behind_content,
            options = {
                enableVibration(true)
            }
        ),
        UNKNOWN_TAG(
            "unknown_tag",
            NotificationManager.IMPORTANCE_HIGH,
            R.string.notification_channel_unknown_tag_title,
            R.string.notification_channel_unknown_tag_content,
            options = {
                enableVibration(true)
            }
        );

        fun isEnabled(context: Context): Boolean {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if(!context.hasNotificationPermission()) return false
            if(!notificationManager.areNotificationsEnabled()) return false
            //If the channel hasn't been created yet, default to enabled
            val channel = notificationManager.getNotificationChannel(id) ?: return true
            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }
    }

    enum class NotificationId {
        BUFFER, //Foreground ID cannot be 0
        FOREGROUND_SERVICE,
        UPDATES,
        ERROR,
        FOREGROUND_SERVICE_ERROR,
        FIND_MY_DEVICE,
        LEFT_BEHIND_PREFIX,
        UNKNOWN_TAG,
        LOGGED_OUT
    }

    enum class PendingIntentId {
        FOREGROUND_SERVICE,
        FOREGROUND_SERVICE_ACTION,
        FIND_MY_DEVICE,
        FIND_MY_DEVICE_STOP,
        AUTOMATION,
        BLUETOOTH,
        ALARM_LOCATION,
        ALARM_WIDGET,
        SAFE_AREA_PERMISSIONS,
        SAFE_AREA_RETRY,
        GEOFENCE,
        WIDGET_REFRESH_PREFIX,
        WIDGET_CLICK_PREFIX,
        WIDGET_ERROR_CLICK_PREFIX,
        UNKNOWN_TAG
    }

}

class NotificationRepositoryImpl(
    private val context: Context
): NotificationRepository {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val scope = MainScope()

    override fun createNotification(
        channel: NotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): Notification {
        return context.createNotification(channel, builder)
    }

    override fun createNotificationChannel(channel: NotificationChannel): AndroidNotificationChannel {
        return getNotificationChannel(channel)
            ?: context.createNotificationChannel(channel, channel.options)
    }

    override fun getNotificationChannel(channel: NotificationChannel): AndroidNotificationChannel? {
        return notificationManager.getNotificationChannel(channel.id)
    }

    override fun showNotification(
        id: NotificationId,
        channel: NotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): Notification {
        return createNotification(channel, builder).also {
            notificationManager.notify(id.ordinal, it)
        }
    }

    override fun showNotification(
        id: Int,
        channel: NotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): Notification {
        return createNotification(channel, builder).also {
            notificationManager.notify(id, it)
        }
    }

    override fun cancelNotification(id: NotificationId, delay: Long) {
        cancelNotification(id.ordinal, delay)
    }

    override fun cancelNotification(id: Int, delay: Long) {
        if(delay <= 0) {
            notificationManager.cancel(id)
        }else{
            scope.launch {
                delay(delay)
                notificationManager.cancel(id)
            }
        }
    }

    override fun showLoggedOutNotificationIfNeeded() {
        //Don't show notification if app is already visible
        if(context.isInForeground() == true) {
            cancelNotification(NotificationId.LOGGED_OUT)
            return
        }
        showNotification(
            NotificationId.LOGGED_OUT,
            NotificationChannel.ERROR
        ) {
            it.setSmallIcon(R.drawable.ic_notification_error)
            it.setContentTitle(context.getString(R.string.notification_title_logged_out))
            it.setContentText(context.getString(R.string.notification_content_logged_out))
            it.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    NotificationId.LOGGED_OUT.ordinal,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                ))
            it.setAutoCancel(true)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    private fun Context.createNotificationChannel(
        channel: NotificationChannel,
        builder: (AndroidNotificationChannel) -> Unit
    ): AndroidNotificationChannel {
        val notificationChannel =
            AndroidNotificationChannel(
                channel.id,
                getString(channel.titleRes),
                channel.importance
            ).apply {
                description = getString(channel.descRes)
                builder(this)
            }
        notificationManager.createNotificationChannel(notificationChannel)
        return notificationChannel
    }

    private fun Context.createNotification(
        channel: NotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): Notification {
        createNotificationChannel(channel)
        return NotificationCompat.Builder(this, channel.id).apply(builder).apply {
            if(getStyle() != null) return@apply //Already applied
            val text = getContentText() ?: return@apply
            setStyle(NotificationCompat.BigTextStyle(this).bigText(text))
        }.build()
    }

}