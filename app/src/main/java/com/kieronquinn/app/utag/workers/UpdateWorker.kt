package com.kieronquinn.app.utag.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.NotificationRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationId
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.repositories.UpdateRepository
import com.kieronquinn.app.utag.ui.activities.MainActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val UPDATE_CHECK_WORK_TAG = "utag_update_check"
        private const val UPDATE_CHECK_HOUR = 12L
        private const val TAG_ALL = "all"

        private fun clearCheckWorker(context: Context){
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(UPDATE_CHECK_WORK_TAG)
        }

        fun queueCheckWorker(context: Context){
            clearCheckWorker(context)
            val checkWorker = Builder().build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                UPDATE_CHECK_WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, checkWorker
            )
        }
    }

    private val notificationRepository by inject<NotificationRepository>()
    private val settingsRepository by inject<SettingsRepository>()
    private val updateRepository by inject<UpdateRepository>()
    private val smartThingsRepository by inject<SmartThingsRepository>()

    override suspend fun doWork(): Result {
        if(isUpdateCheckEnabled()){
            checkUpdates()
        }
        return Result.success()
    }

    private fun isUpdateCheckEnabled(): Boolean {
        if(!NotificationChannel.UPDATES.isEnabled(applicationContext)) return false
        if(!settingsRepository.autoUpdatesEnabled.getSync()) return false
        return true
    }

    private suspend fun checkUpdates() {
        val update = updateRepository.getUpdate()
        val smartThingsVersion = smartThingsRepository.getModuleState()?.takeIf {
            it.isUTagBuild && !it.wasInstalledOnPlay
        }?.let {
            when(it) {
                is ModuleState.Installed -> Pair(it.versionCode, it.versionName)
                is ModuleState.Newer -> Pair(it.versionCode, it.versionName)
                is ModuleState.Outdated -> Pair(it.versionCode, it.versionName)
                else -> null
            }
        }
        val modUpdate = if(smartThingsVersion!= null) {
            updateRepository.getModRelease()?.takeIf {
                it.versionCode != smartThingsVersion.first
            }
        }else null
        if(update == null && modUpdate == null) return //No updates available
        val title = when {
            update != null && modUpdate != null -> R.string.update_notification_multiple_title
            update != null -> R.string.update_notification_title
            else -> R.string.update_notification_smartthings_title
        }
        val content = when {
            update != null && modUpdate != null -> {
                applicationContext.getString(R.string.update_notification_multiple_content)
            }
            update != null -> applicationContext.getString(
                R.string.update_notification_content, BuildConfig.TAG_NAME, update.versionName
            )
            smartThingsVersion != null -> applicationContext.getString(
                R.string.update_notification_smartthings_content,
                smartThingsVersion.second,
                modUpdate?.version
            )
            else -> return
        }
        notificationRepository.showNotification(
            NotificationId.UPDATES,
            NotificationChannel.UPDATES
        ) {
            it.setSmallIcon(R.drawable.ic_notification)
            it.setContentTitle(applicationContext.getString(title))
            it.setContentText(content)
            it.setContentIntent(
                PendingIntent.getActivity(
                applicationContext,
                NotificationId.UPDATES.ordinal,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ))
            it.setAutoCancel(true)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    class Builder {
        fun build(): PeriodicWorkRequest {
            val delay = if (LocalDateTime.now().hour < UPDATE_CHECK_HOUR) {
                Duration.between(ZonedDateTime.now(), ZonedDateTime.now().toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plusHours(UPDATE_CHECK_HOUR)).toMinutes()
            } else {
                Duration.between(ZonedDateTime.now(), ZonedDateTime.now().toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plusDays(1)
                    .plusHours(UPDATE_CHECK_HOUR)).toMinutes()
            }
            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            return PeriodicWorkRequest.Builder(
                UpdateWorker::class.java, 24, TimeUnit.HOURS
            ).addTag(UPDATE_CHECK_WORK_TAG)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(TAG_ALL)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                ).build()
        }
    }


}