package com.kieronquinn.app.utag.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.database.FindMyDeviceConfig
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationId
import com.kieronquinn.app.utag.repositories.NotificationRepository.PendingIntentId
import com.kieronquinn.app.utag.ui.activities.FindMyDeviceActivity
import com.kieronquinn.app.utag.ui.activities.FindMyDeviceActivity.FindMyDeviceActivityConfig
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.xposed.extensions.getParcelableExtraCompat
import com.kieronquinn.app.utag.utils.extensions.getVibrator
import com.kieronquinn.app.utag.utils.extensions.isServiceRunning
import com.kieronquinn.app.utag.utils.extensions.startForeground
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject

class UTagFindMyDeviceService: LifecycleService() {

    companion object {
        private const val EXTRA_DEVICE_LABEL = "device_label"
        private const val EXTRA_CONFIG = "config"
        private const val START_DELAY = 5000L
        private const val TIMEOUT = 60_000L

        private val URI_RING = Uri.parse(
            "android.resource://${BuildConfig.APPLICATION_ID}/${R.raw.find_my_device_ring}"
        )

        fun startIfNeeded(context: Context, config: FindMyDeviceConfig, label: String?) {
            if(context.isServiceRunning(UTagFindMyDeviceService::class.java)) return
            val intent = Intent(context, UTagFindMyDeviceService::class.java).apply {
                putExtra(EXTRA_CONFIG, config)
                putExtra(EXTRA_DEVICE_LABEL, label)
            }
            context.startService(intent)
        }
    }

    private val notificationRepository by inject<NotificationRepository>()
    private var mediaPlayer: MediaPlayer? = null
    private var startAlarmVolume: Int? = null

    private val vibrator by lazy {
        getVibrator()
    }

    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val speakerDevice by lazy {
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
    }

    private fun setupStopListener() = whenCreated {
        broadcastReceiverAsFlow(IntentFilter(FindMyDeviceRepository.ACTION_STOP)).collect {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            //Cancel any remaining race-condition created notifications with a delay
            notificationRepository.cancelNotification(NotificationId.FIND_MY_DEVICE, 2500L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        setupStopListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null || !handleIntent(intent)) {
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer = null
        vibrator.cancel()
        startAlarmVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent): Boolean {
        val config = intent.getParcelableExtraCompat(EXTRA_CONFIG, FindMyDeviceConfig::class.java)
            ?: return false
        val deviceLabel = intent.getStringExtra(EXTRA_DEVICE_LABEL)
        val now = System.currentTimeMillis()
        val activityConfig = FindMyDeviceActivityConfig(
            deviceLabel,
            if(config.delay) now + START_DELAY else now,
            now + TIMEOUT,
            START_DELAY,
            TIMEOUT
        )
        val notification = notificationRepository.createNotification(
            NotificationChannel.FIND_DEVICE
        ) {
            if(config.delay) {
                it.delay(activityConfig)
            }else{
                it.ringing(activityConfig, true)
            }
        }
        return startForeground(NotificationId.FIND_MY_DEVICE, notification).also { success ->
            if(!success) return@also
            startConfigBasedRinging(config, activityConfig)
        }
    }

    private fun startConfigBasedRinging(
        config: FindMyDeviceConfig,
        activityConfig: FindMyDeviceActivityConfig
    ) = whenCreated {
        if(config.delay) {
            startRingingAfterDelay(config, activityConfig)
        }else{
            startRinging(config)
        }
    }

    private suspend fun startRingingAfterDelay(
        config: FindMyDeviceConfig,
        activityConfig: FindMyDeviceActivityConfig
    ) {
        delay(START_DELAY)
        //Update the notification as we're about to start ringing, but without a full screen intent
        notificationRepository.showNotification(
            NotificationId.FIND_MY_DEVICE,
            NotificationChannel.FIND_DEVICE
        ) {
            it.ringing(activityConfig, false)
        }
        startRinging(config)
    }

    private suspend fun startRinging(config: FindMyDeviceConfig) {
        val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@UTagFindMyDeviceService, URI_RING)
            setVolume(config.volume, config.volume)
            isLooping = true
            //Output to the speaker even if BT is connected
            speakerDevice?.let {
                setPreferredDevice(it)
            }
            setAudioAttributes(AudioAttributes.Builder()
                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            prepare()
            audioManager.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build()
            )
            start()
        }
        if(config.vibrate) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500L, 500L), 1))
        }
        delay(TIMEOUT)
        sendBroadcast(FindMyDeviceRepository.STOP_INTENT)
    }

    private fun NotificationCompat.Builder.ringing(
        config: FindMyDeviceActivityConfig,
        fullScreen: Boolean
    ) = apply {
        val intent = FindMyDeviceActivity.createIntent(
            this@UTagFindMyDeviceService,
            config
        )
        val pendingIntent = PendingIntent.getActivity(
            this@UTagFindMyDeviceService,
            PendingIntentId.FIND_MY_DEVICE.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val actionPendingIntent = PendingIntent.getBroadcast(
            this@UTagFindMyDeviceService,
            PendingIntentId.FIND_MY_DEVICE_STOP.ordinal,
            FindMyDeviceRepository.STOP_INTENT,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val title = getString(R.string.notification_title_find_my_device)
        val content = if(config.deviceLabel != null) {
            getString(R.string.notification_content_find_my_device, config.deviceLabel)
        }else{
            getString(R.string.notification_content_find_my_device_generic)
        }
        setContentTitle(getString(R.string.notification_title_find_my_device))
        setContentText(content)
        setSmallIcon(R.drawable.ic_notification_ring)
        setOngoing(true)
        if(fullScreen) {
            setFullScreenIntent(pendingIntent, true)
        }
        setContentIntent(pendingIntent)
        setGroup("find_my_device")
        setTicker(title)
        addAction(
            0,
            getString(R.string.notification_action_find_my_device),
            actionPendingIntent
        )
    }

    private fun NotificationCompat.Builder.delay(config: FindMyDeviceActivityConfig) = apply {
        val intent = FindMyDeviceActivity.createIntent(
            this@UTagFindMyDeviceService,
            config
        )
        val pendingIntent = PendingIntent.getActivity(
            this@UTagFindMyDeviceService,
            PendingIntentId.FIND_MY_DEVICE.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val actionPendingIntent = PendingIntent.getBroadcast(
            this@UTagFindMyDeviceService,
            PendingIntentId.FIND_MY_DEVICE_STOP.ordinal,
            FindMyDeviceRepository.STOP_INTENT,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val title = getString(R.string.notification_title_find_my_device)
        val content = if(config.deviceLabel != null) {
            getString(R.string.notification_content_find_my_device_delay, config.deviceLabel)
        }else{
            getString(R.string.notification_content_find_my_device_delay_generic)
        }
        setContentTitle(getString(R.string.notification_title_find_my_device))
        setContentText(content)
        setSmallIcon(R.drawable.ic_notification_ring)
        setOngoing(true)
        setFullScreenIntent(pendingIntent, true)
        setContentIntent(pendingIntent)
        setGroup("find_my_device")
        setTicker(title)
        addAction(
            0,
            getString(R.string.notification_action_find_my_device),
            actionPendingIntent
        )
    }

}