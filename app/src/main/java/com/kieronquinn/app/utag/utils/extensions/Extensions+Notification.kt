package com.kieronquinn.app.utag.utils.extensions

import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Style
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationId

fun Service.startForeground(notificationId: NotificationId, notification: Notification): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                notificationId.ordinal,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        }else{
            startForeground(notificationId.ordinal, notification)
        }
        true
    }catch (e: Exception) {
        //Caches ForegroundServiceStartNotAllowedException on S+ when unable to startForeground
        false
    }
}

fun NotificationCompat.Builder.getContentText(): CharSequence? {
    return this::class.java.getDeclaredField("mContentText").apply {
        isAccessible = true
    }.get(this) as CharSequence?
}

fun NotificationCompat.Builder.getStyle(): Style? {
    return this::class.java.getDeclaredField("mStyle").apply {
        isAccessible = true
    }.get(this) as Style?
}