package com.kieronquinn.app.utag.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.DeadObjectException
import android.os.IBinder
import android.os.PowerManager
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.bundle.bundleOf
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.GeofencingEvent
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.components.bluetooth.BaseTagConnection
import com.kieronquinn.app.utag.components.bluetooth.BaseTagConnection.SyncResult
import com.kieronquinn.app.utag.components.bluetooth.ConnectedTagConnection
import com.kieronquinn.app.utag.components.bluetooth.ConnectedTagConnection.TagConnectionState
import com.kieronquinn.app.utag.components.bluetooth.ScannedTagConnection
import com.kieronquinn.app.utag.model.ButtonVolumeLevel
import com.kieronquinn.app.utag.model.TagStateChangeEvent
import com.kieronquinn.app.utag.model.TagStatusChangeEvent
import com.kieronquinn.app.utag.model.VolumeLevel
import com.kieronquinn.app.utag.providers.PassiveModeProvider
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.AutomationRepository
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.RefreshPeriod
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.WidgetRefreshPeriod
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository
import com.kieronquinn.app.utag.repositories.LeftBehindRepository
import com.kieronquinn.app.utag.repositories.LeftBehindRepository.LeftBehindTag
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationChannel
import com.kieronquinn.app.utag.repositories.NotificationRepository.NotificationId
import com.kieronquinn.app.utag.repositories.NotificationRepository.PendingIntentId
import com.kieronquinn.app.utag.repositories.PassiveModeRepository
import com.kieronquinn.app.utag.repositories.QcServiceRepository
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import com.kieronquinn.app.utag.repositories.SafeAreaRepository
import com.kieronquinn.app.utag.repositories.SafeAreaRepository.SafeAreaResult
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagData
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.WidgetRepository
import com.kieronquinn.app.utag.service.callback.IBooleanCallback
import com.kieronquinn.app.utag.service.callback.IStringCallback
import com.kieronquinn.app.utag.service.callback.ITagAutoSyncLocationCallback
import com.kieronquinn.app.utag.service.callback.ITagConnectResultCallback
import com.kieronquinn.app.utag.service.callback.ITagStateCallback
import com.kieronquinn.app.utag.service.callback.ITagStatusCallback
import com.kieronquinn.app.utag.ui.activities.BatteryOptimisationTrampolineActivity
import com.kieronquinn.app.utag.ui.activities.MainActivity
import com.kieronquinn.app.utag.utils.extensions.bluetoothEnabledAsFlow
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.utils.extensions.canScheduleExactAlarmsCompat
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.getDarkMode
import com.kieronquinn.app.utag.utils.extensions.isServiceRunning
import com.kieronquinn.app.utag.utils.extensions.startForeground
import com.kieronquinn.app.utag.utils.extensions.suspendCancellableCoroutineWithTimeout
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import com.kieronquinn.app.utag.xposed.Xposed.Companion.ACTION_SMARTTHINGS_PAUSED
import com.kieronquinn.app.utag.xposed.Xposed.Companion.ACTION_SMARTTHINGS_RESUMED
import com.kieronquinn.app.utag.xposed.Xposed.Companion.ACTION_TAG_DEVICE_STATUS_CHANGED
import com.kieronquinn.app.utag.xposed.Xposed.Companion.ACTION_TAG_SCAN_RECEIVED
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_BINDER
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_BLE_MAC
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_CHARACTERISTICS
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_DEVICE_ID
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_NOTIFICATION_CONTENT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_RSSI
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_SERVICE_DATA
import com.kieronquinn.app.utag.xposed.Xposed.Companion.EXTRA_VALUE
import com.kieronquinn.app.utag.xposed.Xposed.Companion.SCAN_ID_UTAG
import com.kieronquinn.app.utag.xposed.Xposed.Companion.SCAN_ID_UTAG_ALT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.SCAN_TYPE_UTAG
import com.kieronquinn.app.utag.xposed.Xposed.Companion.SCAN_TYPE_UTAG_ALT
import com.kieronquinn.app.utag.xposed.extensions.TagActivity_createIntent
import com.kieronquinn.app.utag.xposed.extensions.applySecurity
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity
import com.samsung.android.oneconnect.smarttag.service.IGattRssiCallback
import com.samsung.android.oneconnect.smarttag.service.ISmartTagStateCallback
import com.samsung.android.oneconnect.smarttag.service.ISmartTagSupportService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.android.ext.android.inject
import java.io.FileDescriptor
import java.io.PrintWriter
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.system.exitProcess

class UTagForegroundService: LifecycleService() {

    companion object {
        private const val CONNECT_TIMEOUT = 10_000L
        private const val DISCONNECT_NOTIFICATION_DELAY = 180_000L // 3 minutes
        private const val MIN_RSSI_FOR_OVERMATURE_UPDATE = -75

        private const val ACTION_START_SMARTTAG_SERVICE =
            "com.samsung.android.oneconnect.smarttag.START_SMARTTAG_SUPPORT_SERVICE"

        private const val ACTION_START_TAG_SERVICE =
            "com.samsung.android.oneconnect.action.TAG_START_SERVICE"

        private const val ACTION_RETRY = "${BuildConfig.APPLICATION_ID}.action.RETRY"
        private const val ACTION_LOCATION = "${BuildConfig.APPLICATION_ID}.action.LOCATION"
        private const val ACTION_WIDGET = "${BuildConfig.APPLICATION_ID}.action.WIDGET"
        private const val ACTION_RETRY_SAFE_AREAS =
            "${BuildConfig.APPLICATION_ID}.action.RETRY_SAFE_AREAS"
        private const val EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME"

        const val ACTION_REFRESH_WIDGET = "${BuildConfig.APPLICATION_ID}.action.REFRESH_WIDGET"
        const val EXTRA_APP_WIDGET_ID = "EXTRA_APP_WIDGET_ID"

        private val SMART_TAG_SERVICE_INTENT = Intent(ACTION_START_SMARTTAG_SERVICE).apply {
            `package` = PACKAGE_NAME_ONECONNECT
        }

        private val TAG_SERVICE_INTENT = Intent(ACTION_START_TAG_SERVICE).apply {
            `package` = PACKAGE_NAME_ONECONNECT
        }

        fun startIfNeeded(context: Context): Boolean {
            if(context.isServiceRunning(UTagForegroundService::class.java)) return false
            try {
                context.startService(Intent(context, UTagForegroundService::class.java))
            }catch (e: SecurityException) {
                //Process is bad
                return false
            }
            return true
        }

        fun getRetryIntent(): Intent {
            return Intent(ACTION_RETRY).apply {
                `package` = BuildConfig.APPLICATION_ID
            }
        }
    }

    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val alarmManager by lazy {
        getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val locationServicePendingIntent by lazy {
        val intent = Intent(ACTION_LOCATION).apply { `package` = packageName }
        PendingIntent.getService(
            this,
            PendingIntentId.ALARM_LOCATION.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private val widgetServicePendingIntent by lazy {
        val intent = Intent(ACTION_WIDGET).apply { `package` = packageName }
        PendingIntent.getService(
            this,
            PendingIntentId.ALARM_WIDGET.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private val safeAreaRetryPendingIntent by lazy {
        val intent = Intent(ACTION_RETRY_SAFE_AREAS).apply { `package` = packageName }
        PendingIntent.getService(
            this,
            PendingIntentId.SAFE_AREA_RETRY.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private val connectLock = Mutex()
    private val scanLock = Mutex()
    private val safeAreasLock = Mutex()
    private val notifications by inject<NotificationRepository>()
    private val smartThingsRepository by inject<SmartThingsRepository>()
    private val smartTagRepository by inject<SmartTagRepository>()
    private val qcServiceRepository by inject<QcServiceRepository>()
    private val findMyDeviceRepository by inject<FindMyDeviceRepository>()
    private val automationRepository by inject<AutomationRepository>()
    private val encryptedSettings by inject<EncryptedSettingsRepository>()
    private val settings by inject<SettingsRepository>()
    private val authRepository by inject<AuthRepository>()
    private val safeAreaRepository by inject<SafeAreaRepository>()
    private val deviceRepository by inject<DeviceRepository>()
    private val widgetRepository by inject<WidgetRepository>()
    private val historyWidgetRepository by inject<HistoryWidgetRepository>()
    private val leftBehindRepository by inject<LeftBehindRepository>()
    private val passiveModeRepository by inject<PassiveModeRepository>()
    private val nonOwnerRepository by inject<NonOwnerTagRepository>()

    private var serviceConnection: IServiceConnection? = null
    private var foregroundServiceConnection: IServiceConnection? = null
    private var remoteService: ISmartTagSupportService? = null
    private var remoteForegroundService: IUTagSmartThingsForegroundService? = null
    private var isDisconnecting = false
    private var isSettingUpSafeAreas = false
    private var notificationHash: Int? = null

    private val tagStateChangeBus = MutableStateFlow(System.currentTimeMillis())
    private val safeAreaSetupBus = MutableStateFlow(System.currentTimeMillis())
    private val tagStates = HashMap<String, TagConnectionState>()
    private val tagConnections = HashMap<String, BaseTagConnection>()
    private val tagStateCallbacks = HashMap<String, ITagStateCallback>()
    private val tagStatusCallbacks = HashMap<String, ITagStatusCallback>()
    private val tagConnectCallbacks = HashMap<String, ITagConnectResultCallback>()
    private val tagAutoSyncLocationCallbacks = HashMap<String, ITagAutoSyncLocationCallback>()
    private val tagDisconnectNotificationJobs = HashMap<String, Job>()

    private val locationRefreshPeriodSetting = encryptedSettings.locationRefreshPeriod
    private val locationOnBatterySaver = encryptedSettings.locationOnBatterySaver
    private val widgetRefreshPeriodSetting = encryptedSettings.widgetRefreshPeriod
    private val widgetOnBatterySaver = encryptedSettings.widgetRefreshOnBatterySaver

    private val overmatureOfflinePreventionEnabled =
        encryptedSettings.overmatureOfflinePreventionEnabled.asFlow()
            .stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val knownTagNames = smartTagRepository.getKnownTagNames()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val autoDismissNotifications = settings.autoDismissNotifications.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    
    private val debugEnabled = encryptedSettings.isDebugModeEnabled(lifecycleScope)

    private val retryRequestReceiver = broadcastReceiverAsFlow(IntentFilter(ACTION_RETRY)).map {
        it.verifySecurity(BuildConfig.APPLICATION_ID)
    }

    private val tagScanReceiver = broadcastReceiverAsFlow(
        IntentFilter(ACTION_TAG_SCAN_RECEIVED)
    ).map {
        it.also { it.verifySecurity(PACKAGE_NAME_ONECONNECT) }
    }

    private val smartThingsPausedReceiver = broadcastReceiverAsFlow(
        IntentFilter(ACTION_SMARTTHINGS_PAUSED)
    ).map {
        it.also { it.verifySecurity(PACKAGE_NAME_ONECONNECT) }
    }

    private val smartThingsResumedReceiver = broadcastReceiverAsFlow(
        IntentFilter(ACTION_SMARTTHINGS_RESUMED)
    ).map {
        it.also { it.verifySecurity(PACKAGE_NAME_ONECONNECT) }
    }

    private val retrySafeZoneRequestReceiver by lazy {
        broadcastReceiverAsFlow(
            IntentFilter(ACTION_RETRY_SAFE_AREAS)
        ).map {
            it.verifySecurity(BuildConfig.APPLICATION_ID)
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    private val batterySaverReceiver by lazy {
        broadcastReceiverAsFlow(
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        ).stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    //Has to be lazy as it relies on a system service
    private val bluetoothEnabled by lazy {
        bluetoothEnabledAsFlow(lifecycleScope)
    }

    private val darkMode by lazy {
        getDarkMode(lifecycleScope)
    }

    private val locationRefreshPeriod by lazy {
        combine(
            locationRefreshPeriodSetting.asFlow(),
            locationOnBatterySaver.asFlow(),
            tagStateChangeBus,
            batterySaverReceiver
        ) { locationSetting, locationOnBatterySaver, _, _ ->
            when {
                //Disable if battery saver is enabled and the user has not overridden
                !locationOnBatterySaver && powerManager.isPowerSaveMode -> RefreshPeriod.NEVER
                tagStates.isEmpty() || tagStates.none { it.value != TagConnectionState.DEFAULT } -> {
                    //No tags connected or scanned, no need to location refresh
                    RefreshPeriod.NEVER
                }
                else -> locationSetting
            }
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    private val widgetRefreshPeriod by lazy {
        combine(
            widgetRefreshPeriodSetting.asFlow(),
            widgetOnBatterySaver.asFlow(),
            widgetRepository.hasWidgets(),
            historyWidgetRepository.hasWidgets(),
            batterySaverReceiver
        ) { widgetSetting, widgetOnBatterySaver, hasWidgets, hasHistoryWidgets, _ ->
            when {
                //If there's no widgets added, disable
                !hasWidgets && !hasHistoryWidgets -> WidgetRefreshPeriod.NEVER
                //Disable if battery saver is enabled and the user has not overridden
                !widgetOnBatterySaver && powerManager.isPowerSaveMode -> WidgetRefreshPeriod.NEVER
                else -> widgetSetting
            }
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    private val tagStatusChangedReceiver =
        broadcastReceiverAsFlow(IntentFilter(ACTION_TAG_DEVICE_STATUS_CHANGED)).map {
            it.verifySecurity(PACKAGE_NAME_ONECONNECT)
            val deviceId = it.getStringExtra(EXTRA_DEVICE_ID) ?: return@map null
            val characteristics = it.getStringExtra(EXTRA_CHARACTERISTICS) ?: return@map null
            val value = it.getByteArrayExtra(EXTRA_VALUE) ?: return@map null
            TagStateChangeEvent(deviceId, characteristics, value)
        }.filterNotNull()

    private val retryPendingIntent by lazy {
        PendingIntent.getBroadcast(
            this,
            PendingIntentId.FOREGROUND_SERVICE_ACTION.ordinal,
            Intent(ACTION_RETRY).apply {
                `package` = packageName
                applySecurity(this@UTagForegroundService)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private val tagStateCallback = object: ISmartTagStateCallback.Stub() {
        override fun onSmartTagStateChanged(deviceId: String, state: Int) {
            val tagState = TagConnectionState.fromValue(state)
            //We cache the tag states, but a SCANNED update is always treated as new
            if(tagStates[deviceId] == tagState) return
            log("$deviceId -> $tagState")
            var shouldShowDisconnectNotification = false
            val isPassive = passiveModeRepository.isInPassiveMode(deviceId, true)
            when(tagState) {
                TagConnectionState.D2D_CONNECTED -> {
                    synchronized(tagConnections) {
                        if (tagConnections[deviceId] !is ConnectedTagConnection) {
                            tagConnections[deviceId]?.close()
                            tagConnections[deviceId] = ConnectedTagConnection(
                                deviceId,
                                !tagConnections.containsKey(deviceId),
                                ::onTagAutoSyncStarted,
                                ::onTagAutoSyncFinished,
                                ::onTagStatusChanged,
                                remoteService ?: return
                            )
                        }
                    }
                    //Regardless of connect method, we're now connected so disable temp override
                    passiveModeRepository.setTemporaryOverride(deviceId, false)
                    //Check if this was an overmature update connection, and disconnect if so
                    if(passiveModeRepository.isInPassiveMode(deviceId)) {
                        disconnect(deviceId)
                    }
                    synchronized(tagDisconnectNotificationJobs) {
                        //Cancel any existing notification delay job
                        tagDisconnectNotificationJobs[deviceId]?.cancel()
                        tagDisconnectNotificationJobs.remove(deviceId)
                    }
                    dismissDisconnectNotificationIfNeeded(deviceId)
                }
                TagConnectionState.D2D_SCANNED -> {
                    runWithRemoteService {
                        connectAndNotifyResult(deviceId)
                    }
                    synchronized(tagConnections) {
                        if(tagConnections[deviceId] !is ScannedTagConnection) {
                            tagConnections[deviceId]?.close()
                            if(tagConnections[deviceId] is ConnectedTagConnection && !isPassive) {
                                //Non-passive Tag disconnect, show notification
                                shouldShowDisconnectNotification = true
                            }
                            tagConnections[deviceId] = ScannedTagConnection(
                                deviceId,
                                !tagConnections.containsKey(deviceId),
                                ::onTagAutoSyncStarted,
                                ::onTagAutoSyncFinished
                            )
                        }
                    }
                    //Only passive tags should be dismissed in this state
                    if(isPassive) {
                        synchronized(tagDisconnectNotificationJobs) {
                            //Cancel any existing notification delay job
                            tagDisconnectNotificationJobs[deviceId]?.cancel()
                            tagDisconnectNotificationJobs.remove(deviceId)
                        }
                        dismissDisconnectNotificationIfNeeded(deviceId)
                    }
                }
                TagConnectionState.DEFAULT -> {
                    synchronized(tagConnections) {
                        if(tagConnections[deviceId] is ConnectedTagConnection && !isPassive) {
                            //Non-passive Tag disconnect (straight to DEFAULT), show notification
                            shouldShowDisconnectNotification = true
                        }
                        if(tagConnections[deviceId] is ScannedTagConnection && isPassive) {
                            //Passive Tag disconnect, show notification
                            shouldShowDisconnectNotification = true
                        }
                        tagConnections.remove(deviceId).also {
                            it?.close()
                        }
                    }
                }
            }
            if(shouldShowDisconnectNotification) {
                //If Tag has disconnected, restart scan in case it's due to a crash
                startScan()
                synchronized(tagDisconnectNotificationJobs) {
                    //Cancel any existing notification delay job
                    tagDisconnectNotificationJobs[deviceId]?.cancel()
                    tagDisconnectNotificationJobs[deviceId] =
                        sendDisconnectNotificationIfNeeded(deviceId, isPassive)
                }
            }
            tagStates[deviceId] = tagState
            onTagStatesChanged()
            whenCreated {
                tagStateChangeBus.emit(System.currentTimeMillis())
            }
        }
    }

    private val deathRecipient = IBinder.DeathRecipient {
        log("Remote service died")
        onServiceDisconnected()
    }

    private val foregroundDeathRecipient = IBinder.DeathRecipient {
        log("Foreground service died")
        onForegroundServiceDisconnected()
    }

    private val smartThingsBinder = object: IUTagSmartThingsForegroundService.Stub() {
        override fun ping(): Boolean {
            return true
        }

        override fun stop() {
            //Not this way
        }

        override fun stopProcess() {
            //Not this way
        }
    }

    override fun onCreate() {
        super.onCreate()
        log("Service created")
        val notification = notifications.createNotification(NotificationChannel.FOREGROUND_SERVICE) {
            it.ongoing()
        }
        //Clear any existing error notifications
        notifications.cancelNotification(NotificationId.FOREGROUND_SERVICE_ERROR)
        if (powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) &&
            startForeground(NotificationId.FOREGROUND_SERVICE, notification)
        ) {
            tryConnectToService()
            tryConnectToForegroundService()
            setupBluetoothReceiver()
            setupRetryReceiver()
            setupPassiveModeChanged()
            setupRetrySafeZoneReceiver()
            setupTagScanReceiver()
            setupSmartThingsPausedReceiver()
            setupSmartThingsResumedReceiver()
            setupPassiveModeTemporaryDisable()
            setupTagStatusChangedListener()
            setupNotificationForceUpdates()
            setupLocationRefreshPeriod()
            setupWidgetRefreshPeriod()
        } else {
            //Can't start foreground, battery optimisation probably needs disabling
            notifications.showNotification(
                NotificationId.FOREGROUND_SERVICE_ERROR,
                NotificationChannel.ERROR
            ) {
                it.batteryOptimisation(BuildConfig.APPLICATION_ID)
            }
            stopSelf()
        }
        //Should be running regardless of service
        setupDarkModeChangeListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_LOCATION -> locateAndScheduleNext()
            ACTION_WIDGET -> refreshWidgetAndScheduleNext()
            ACTION_REFRESH_WIDGET -> {
                refreshWidgets(intent.getIntExtra(EXTRA_APP_WIDGET_ID, -1))
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<out String>) {
        if(debugEnabled.value != true) return
        writer.use {
            if(BuildConfig.DEBUG) {
                //Debug builds write user ID and current tokens to dump. Not included on release.
                it.write("User ID: ${authRepository.getUserId()}\n")
                it.write("SmartThings token: ${authRepository.getSmartThingsAuthToken()}\n")
                it.write("Find auth token: ${authRepository.getFindAuthToken()}\n")
                it.write("\n")
            }
            it.write("Tag States:\n")
            tagStates.forEach { (t, u) ->
                it.write("$t: $u\n")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
        isDisconnecting = true
        serviceConnection?.let {
            smartThingsRepository.unbindServiceAsync(it)
        }
        disconnectForegroundService()
        onAllTagsDisconnected()
        foregroundServiceConnection?.let {
            smartThingsRepository.unbindServiceAsync(it)
        }
        //Remove any ongoing notifications. The delay is a hack to get around binder race conditions
        notifications.cancelNotification(NotificationId.FOREGROUND_SERVICE, 2500L)
        log("Service destroyed")
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return IUTagServiceImpl()
    }

    private fun setupBluetoothReceiver() = whenCreated {
        bluetoothEnabled.collect {
            if(!it.change) return@collect
            if (it.enabled) {
                startScan()
            } else {
                try {
                    remoteForegroundService?.stopProcess()
                }catch (e: DeadObjectException) {
                    //This is normal and means it worked
                }
                onAllTagsDisconnected()
            }
            updateNotification()
        }
    }

    private fun setupPassiveModeChanged() = whenCreated {
        PassiveModeProvider.onChange(this@UTagForegroundService).collect { pair ->
            val deviceIdHash = pair.first
            val enabled = pair.second
            if (!enabled) {
                val deviceId = synchronized(tagConnections) {
                    val connection = tagConnections.entries.firstOrNull {
                        it.key.hashCode() == deviceIdHash
                    }?.value as? ScannedTagConnection
                    connection?.deviceId
                }
                if (deviceId != null) {
                    runWithRemoteService {
                        connectAndNotifyResult(deviceId)
                    }
                }
            } else {
                val deviceId = synchronized(tagConnections) {
                    val connection = tagConnections.entries.firstOrNull {
                        it.key.hashCode() == deviceIdHash
                    }?.value as? ConnectedTagConnection
                    connection?.deviceId
                }
                if (deviceId != null) {
                    runWithRemoteService {
                        disconnect(deviceId)
                    }
                }
            }
        }
    }

    private fun setupRetryReceiver() = whenCreated {
        retryRequestReceiver.collect {
            tryConnectToService()
            tryConnectToForegroundService()
        }
    }

    private fun setupRetrySafeZoneReceiver() = whenCreated {
        combine(
            safeAreaRepository.getSafeAreas()
                .map { it.map { item -> item.id }.sorted().hashCode() }
                .distinctUntilChanged(),
            retrySafeZoneRequestReceiver,
            safeAreaSetupBus
        ) { _, _, _ ->
            System.currentTimeMillis()
        }.collect {
            setupSafeAreas()
        }
    }

    private fun setupTagScanReceiver() = whenCreated {
        tagScanReceiver.collect {
            val deviceId = it.getStringExtra(EXTRA_DEVICE_ID) ?: return@collect
            val serviceData = it.getByteArrayExtra(EXTRA_SERVICE_DATA) ?: return@collect
            val bleMac = it.getStringExtra(EXTRA_BLE_MAC) ?: return@collect
            val rssi = it.getIntExtra(EXTRA_RSSI, 1).takeIf { rssi -> rssi <= 0 }
                ?: return@collect
            val tagData = smartTagRepository.decodeServiceData(serviceData)
            log("Received tag scan for $deviceId, mac is now $bleMac with RSSI $rssi, privacy ID ${tagData.privacyId}")
            smartTagRepository.cacheServiceData(deviceId, serviceData, bleMac)
            if(!overmatureOfflinePreventionEnabled.firstNotNull()) return@collect
            if(tagData.tagState.shouldPreventOvermatureOffline() && rssi >= MIN_RSSI_FOR_OVERMATURE_UPDATE) {
                log("Start preventing overmature offline for $deviceId")
                preventOvermatureOffline(deviceId)
            }else{
                if(passiveModeRepository.isInPassiveMode(deviceId) &&
                    tagStates[deviceId] == TagConnectionState.D2D_CONNECTED) {
                    log("Retrying disconnect for $deviceId")
                    disconnect(deviceId)
                }
            }
        }
    }

    private fun setupSmartThingsPausedReceiver() = whenCreated {
        smartThingsPausedReceiver.collect {
            passiveModeRepository.onSmartThingsPaused()
        }
    }

    private fun setupSmartThingsResumedReceiver() = whenCreated {
        smartThingsResumedReceiver.collect {
            passiveModeRepository.onSmartThingsResumed()
        }
    }

    private fun setupPassiveModeTemporaryDisable() = whenCreated {
        passiveModeRepository.checkSmartThingsForeground()
        passiveModeRepository.passiveModeTemporaryDisable.collect { enabled ->
            if(enabled == false) {
                log("Passive mode temporary bypass disabled, disconnecting tags")
                synchronized(tagConnections) {
                    val connections = tagConnections.filterKeys {
                        passiveModeRepository.isInPassiveMode(it)
                    }.filterValues {
                        it is ConnectedTagConnection
                    }
                    connections.iterator().forEach {
                        disconnect(it.key)
                    }
                }
            }else{
                log("Passive mode temporary bypass enabled")
            }
        }
    }

    private fun <T> runWithRemoteService(block: ISmartTagSupportService.() -> T): T? {
        return try {
            remoteService?.let(block)
        }catch (e: Exception) {
            onServiceDisconnected()
            null
        }
    }

    private fun disconnect(deviceId: String) = whenCreated {
        log("Disconnecting ($deviceId)")
        runWithRemoteService {
            disconnect(deviceId)
        }
    }

    private fun onGeofenceReceived(intent: Intent) = whenCreated {
        val geofenceIntent = GeofencingEvent.fromIntent(intent)
        val event = geofenceIntent?.geofenceTransition
            ?.takeIf { transition -> transition >= 0 } ?: return@whenCreated
        val ids = geofenceIntent.triggeringGeofences?.map { geofence ->
            geofence.requestId
        } ?: return@whenCreated
        safeAreaRepository.onGeofenceEvent(event, ids)
    }

    private fun setupTagStatusChangedListener() = whenCreated {
        tagStatusChangedReceiver.collect {
            (tagConnections[it.deviceId] as? ConnectedTagConnection)?.onTagStateChanged(it)
        }
    }

    private fun tryConnectToService() = whenCreated {
        connectLock.withLock {
            if(remoteService != null) return@whenCreated
            //Remove the error notification, it will be re-posted if necessary
            notifications.cancelNotification(NotificationId.FOREGROUND_SERVICE_ERROR)
            updateNotification()
            log("Connecting to service (attempt 1)")
            if(connectToService()) return@withLock
            log("Connection failed")
            delay(5_000L)
            log("Connecting to service (attempt 2)")
            if(!connectToService()) {
                //Clear the hash so retry immediately shows the regular notification
                notificationHash = null
                //Update the ongoing notification
                notifications.showNotification(
                    NotificationId.FOREGROUND_SERVICE,
                    NotificationChannel.FOREGROUND_SERVICE
                ) {
                    it.ongoing(isError = true)
                }
                //Also show a higher priority error notification
                notifications.showNotification(
                    NotificationId.FOREGROUND_SERVICE_ERROR,
                    NotificationChannel.ERROR
                ) {
                    it.error()
                }
            }
        }
    }

    private fun tryConnectToForegroundService() = whenCreated {
        connectLock.withLock {
            if(foregroundServiceConnection != null) return@whenCreated
            //Check if SmartThings will be able to startForeground, show a notification and stop if not
            if(!checkSmartThingsForeground()) return@whenCreated
            log("Connecting to foreground service (attempt 1)")
            if(connectToForegroundService()) return@withLock
            log("Connection failed")
            delay(5_000L)
            log("Connecting to foreground service (attempt 2)")
            if(!connectToForegroundService()) {
                Log.e("UTAG", "Couldn't connect to foreground service, BT may be killed")
            }
        }
    }

    private suspend fun connectToService(): Boolean = suspendCancellableCoroutineWithTimeout(
        CONNECT_TIMEOUT
    ) {
        var hasResumed = false
        val serviceConnection = object: IServiceConnection.Stub() {
            override fun onServiceConnected(
                binder: IBinder,
                componentName: ComponentName
            ) {
                if(hasResumed) return
                hasResumed = true
                onServiceConnected(this, binder)
                it.resume(true)
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                onServiceDisconnected()
            }
        }
        if(!smartThingsRepository.bindService(SMART_TAG_SERVICE_INTENT, serviceConnection)){
            //If main bind call fails, connection will never return a callback
            it.resume(false)
        }
    } ?: false

    private suspend fun connectToForegroundService(): Boolean = suspendCancellableCoroutineWithTimeout(
        CONNECT_TIMEOUT
    ) {
        var hasResumed = false
        val serviceConnection = object: IServiceConnection.Stub() {
            override fun onServiceConnected(
                binder: IBinder,
                componentName: ComponentName
            ) {
                if(hasResumed) return
                hasResumed = true
                val service = IUTagSmartThingsForegroundService.Stub.asInterface(binder)
                if(service.ping()) {
                    remoteForegroundService = service
                    foregroundServiceConnection = this
                    log("Foreground service connected")
                    binder.linkToDeath(foregroundDeathRecipient, 0)
                    it.resume(true)
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                onForegroundServiceDisconnected()
            }
        }
        val intent = TAG_SERVICE_INTENT.apply {
            putExtra(EXTRA_DEVICE_NAME, getString(R.string.notification_title_smartthings))
            putExtra(EXTRA_NOTIFICATION_CONTENT, getString(R.string.notification_content_smartthings))
            putExtras(bundleOf(EXTRA_BINDER to smartThingsBinder.asBinder()))
        }
        if(!smartThingsRepository.bindService(intent, serviceConnection, true)){
            //If main bind call fails, connection will never return a callback
            it.resume(false)
        }
    } ?: false

    private fun disconnectForegroundService() {
        try {
            remoteForegroundService?.stop()
        } catch (e: Exception) {
            //Nothing we can do
        }
    }

    private suspend fun checkSmartThingsForeground(): Boolean {
        val batteryResult = powerManager.isIgnoringBatteryOptimizations(PACKAGE_NAME_ONECONNECT)
        if(!batteryResult) {
            //SmartThings can't start foreground, battery optimisation probably needs disabling
            notifications.showNotification(
                NotificationId.FOREGROUND_SERVICE_ERROR,
                NotificationChannel.ERROR
            ) {
                it.batteryOptimisation(PACKAGE_NAME_ONECONNECT)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return false
        }
        val smartThingsHasPermissions = smartThingsRepository.hasRequiredPermissions()
        if(!smartThingsHasPermissions) {
            //SmartThings doesn't have the required permissions to run in the background
            notifications.showNotification(
                NotificationId.FOREGROUND_SERVICE_ERROR,
                NotificationChannel.ERROR
            ) {
                it.smartThingsPermissions()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return false
        }
        return true
    }

    private fun onServiceConnected(serviceConnection: IServiceConnection, binder: IBinder) {
        this@UTagForegroundService.serviceConnection = serviceConnection
        binder.linkToDeath(deathRecipient, 0)
        val service = ISmartTagSupportService.Stub.asInterface(binder)
        remoteService = service
        service.registerSmartTagStateCallback(tagStateCallback)
        lifecycleScope.launch {
            //Allow time for the service to start if it's new then start a scan
            delay(2500L)
            startScan()
        }
    }

    private fun onServiceDisconnected() {
        serviceConnection = null
        remoteService = null
        onAllTagsDisconnected()
        if(!isDisconnecting) {
            tryConnectToService()
        }
    }

    /**
     *  Handle all tags being disconnected, due to a service death or Bluetooth being disabled
     */
    private fun onAllTagsDisconnected() {
        synchronized(tagConnections) {
            tagConnections.forEach { it.value.close() }
            tagConnections.clear()
        }
        whenCreated {
            tagStateChangeBus.emit(System.currentTimeMillis())
        }
        //Force a refresh of the connection notifications
        onTagStatesChanged()
    }

    private fun onForegroundServiceDisconnected() {
        foregroundServiceConnection = null
        if(!isDisconnecting) {
            tryConnectToForegroundService()
        }
    }

    private fun onTagStatusChanged(deviceId: String, status: TagStatusChangeEvent) {
        when(status) {
            TagStatusChangeEvent.BUTTON_DOUBLE_CLICK -> {
                onFindMyDeviceTriggered(deviceId)
            }
            TagStatusChangeEvent.BUTTON_CLICK -> {
                handleButtonAction(deviceId, TagButtonAction.PRESS)
            }
            TagStatusChangeEvent.BUTTON_LONG_CLICK -> {
                handleButtonAction(deviceId, TagButtonAction.HOLD)
            }
            else -> {
                //No-op, handled in callbacks
            }
        }
        tagStatusCallbacks.entries.toSet().forEach {
            try {
                it.value.onTagStatusChanged(deviceId, status)
            }catch (e: RemoteException) {
                //Callback has died
                tagStatusCallbacks.remove(it.key)
            }
        }
    }

    private fun onTagAutoSyncStarted(deviceId: String) {
        tagAutoSyncLocationCallbacks.entries.toSet().forEach {
            try {
                it.value.onStartSync(deviceId)
            }catch (e: RemoteException) {
                //Callback has died
                tagAutoSyncLocationCallbacks.remove(it.key)
            }
        }
    }

    private fun onTagAutoSyncFinished(deviceId: String, result: SyncResult) {
        tagAutoSyncLocationCallbacks.entries.toSet().forEach {
            try {
                it.value.onSyncFinished(deviceId, result.name)
            }catch (e: RemoteException) {
                //Callback has died
                tagAutoSyncLocationCallbacks.remove(it.key)
            }
        }
    }

    private fun handleButtonAction(deviceId: String, action: TagButtonAction) = whenCreated {
        if(!Settings.canDrawOverlays(this@UTagForegroundService)) {
            //Can't start Automation, show a notification and abort
            notifications.showNotification(
                NotificationId.FOREGROUND_SERVICE_ERROR,
                NotificationChannel.ERROR
            ) {
                it.automationError()
            }
            return@whenCreated
        }
        //Try to sync the rules from the server, if this fails we'll use the local state
        automationRepository.syncRemoteRuleStates()
        //Get the config for this Tag
        val config = automationRepository.getAutomationConfig(deviceId) ?: return@whenCreated
        val enabled = when(action) {
            TagButtonAction.PRESS -> config.pressEnabled && !config.pressRemoteEnabled
            TagButtonAction.HOLD -> config.holdEnabled && !config.holdRemoteEnabled
        }
        if(!enabled) return@whenCreated
        val intent = when(action) {
            TagButtonAction.PRESS -> config.pressIntent
            TagButtonAction.HOLD -> config.holdIntent
        }?.let { Intent.parseUri(it, 0) } ?: return@whenCreated
        UTagLaunchIntentService.startService(this@UTagForegroundService, intent)
    }

    private fun onFindMyDeviceTriggered(deviceId: String) = whenCreated {
        if(!findMyDeviceRepository.isEnabled(deviceId)) return@whenCreated //Not enabled for this tag
        val config = findMyDeviceRepository.getConfig(deviceId).first()
        val deviceLabel = knownTagNames.value?.get(deviceId.hashCode())
        val errorState = findMyDeviceRepository.getErrorState(deviceId)
        if (errorState == null || !errorState.isCritical) {
            UTagFindMyDeviceService.startIfNeeded(
                this@UTagForegroundService,
                config,
                deviceLabel
            )
        } else {
            //Can't start Find my Device, show a notification and abort
            notifications.showNotification(
                NotificationId.FOREGROUND_SERVICE_ERROR,
                NotificationChannel.ERROR
            ) {
                it.findMyDeviceError()
            }
        }
    }

    private fun setupNotificationForceUpdates() {
        var hasTriedToLoadDeviceIds = false
        combine(
            knownTagNames.filterNotNull(),
            passiveModeRepository.passiveModeConfigs
        ) { knownTagNames, _ ->
            updateNotification(true)
            if(knownTagNames.isEmpty() && !hasTriedToLoadDeviceIds) {
                hasTriedToLoadDeviceIds = true
                deviceRepository.getDeviceIds()
            }
        }.launchIn(lifecycleScope)
    }

    private fun setupSafeAreas() = whenCreated {
        safeAreasLock.withLock {
            if(isSettingUpSafeAreas) return@whenCreated
            isSettingUpSafeAreas = true
            val setupResult = safeAreaRepository.setupSafeAreaListeners()
            val result = if(setupResult == SafeAreaResult.SUCCESS) {
                safeAreaRepository.manuallyUpdateSafeAreas()
            }else setupResult
            when(result) {
                SafeAreaResult.SUCCESS -> {
                    notifications.cancelNotification(NotificationId.FOREGROUND_SERVICE_ERROR)
                }
                SafeAreaResult.FAILED_TO_GET_LOCATION -> {
                    notifications.showNotification(
                        NotificationId.FOREGROUND_SERVICE_ERROR,
                        NotificationChannel.ERROR
                    ) {
                        it.safeAreaError(
                            R.string.notification_content_safe_area_error_location
                        )
                    }
                }
                SafeAreaResult.FAILED_NO_PERMISSIONS -> {
                    notifications.showNotification(
                        NotificationId.FOREGROUND_SERVICE_ERROR,
                        NotificationChannel.ERROR
                    ) {
                        it.safeAreaError(
                            R.string.notification_content_safe_area_error_permissions
                        )
                    }
                }
            }
            isSettingUpSafeAreas = false
        }
    }

    private fun setupLocationRefreshPeriod() = whenCreated {
        locationRefreshPeriod.collect {
            scheduleLocationAlarm()
        }
    }

    private fun setupWidgetRefreshPeriod() = whenCreated {
        widgetRefreshPeriod.collect {
            scheduleWidgetAlarm()
        }
    }

    private fun setupDarkModeChangeListener() = whenCreated {
        darkMode.drop(1).collect {
            //Theme has changed, update widgets
            widgetRepository.onWidgetThemeChanged()
            historyWidgetRepository.onWidgetThemeChanged()
        }
    }

    private fun startScan() = whenCreated {
        qcServiceRepository.runWithService {
            it.startDiscovery(SCAN_TYPE_UTAG, SCAN_ID_UTAG, true, false)
        }
    }

    private fun stopScan() {
        qcServiceRepository.runWithServiceIfAvailable {
            it.stopDiscovery(SCAN_ID_UTAG, false)
        }
    }

    /**
     *  Runs a low-latency (higher power) scan when the app demands it. Only used when the UI is
     *  visible and with short timeouts.
     */
    private suspend fun startManualScan(scanTimeout: Long) = scanLock.withLock {
        //If Bluetooth is disabled, we cannot scan
        if(!bluetoothEnabled.value.enabled) return@withLock
        qcServiceRepository.runWithService {
            log("Start discovery for ${scanTimeout / 1000L} seconds")
            it.startDiscovery(
                SCAN_TYPE_UTAG_ALT,
                SCAN_ID_UTAG_ALT,
                false,
                false
            )
        }
        delay(scanTimeout)
        qcServiceRepository.runWithService {
            it.stopDiscovery(SCAN_ID_UTAG_ALT, false)
        }
    }

    private fun getTagLeftBehindNotificationId(id: String): Int? {
        //Notify disconnect is local, so these IDs would need to be filled during setup
        val index = knownTagNames.value?.keys?.indexOfFirst { key -> key == id.hashCode() }
            ?: return null
        return "${NotificationId.LEFT_BEHIND_PREFIX.ordinal}00$index".toInt()
    }

    private fun dismissDisconnectNotificationIfNeeded(deviceId: String) {
        val autoDismissNotifications = runBlocking {
            autoDismissNotifications.firstNotNull()
        }
        if(!autoDismissNotifications) return
        val notificationId = getTagLeftBehindNotificationId(deviceId) ?: return
        notifications.cancelNotification(notificationId)
        leftBehindRepository.dismissLeftBehindTag(deviceId)
    }

    private fun sendDisconnectNotificationIfNeeded(id: String, isPassive: Boolean) = whenCreated {
        if(!safeAreaRepository.isNotifyDisconnectEnabled(id).first()) return@whenCreated
        if(safeAreaRepository.isDeviceInSafeArea(id)) return@whenCreated
        val requiredStates = if(isPassive) {
            //Allow connected state for passive mode in case the user opens the app
            setOf(TagConnectionState.D2D_SCANNED, TagConnectionState.D2D_CONNECTED)
        }else{
            setOf(TagConnectionState.D2D_CONNECTED)
        }
        log("Delaying disconnect notification for $id")
        //Tags have a delay due to false positives
        delay(DISCONNECT_NOTIFICATION_DELAY)
        //Check the tag has not since reconnected, and reject if so
        val state = tagStates[id]
        if(state != null && requiredStates.contains(state)) {
            log("Rejecting disconnect notification for $id as it's reconnected ($state)")
            return@whenCreated
        }
        val tagState = smartTagRepository.getCurrentTagState(id)
        val name = knownTagNames.value?.get(id.hashCode()) ?: return@whenCreated
        val notificationId = getTagLeftBehindNotificationId(id) ?: return@whenCreated
        val image = if(tagState is SmartTagRepository.TagState.Loaded) {
            //If the location comes back as not allowed, the Tag has been revoked, don't show notif
            if(tagState.requiresAgreement()) {
                log("Rejecting disconnect notification for $name ($id) as the tag is no longer allowed")
                return@whenCreated
            }
            leftBehindRepository.getLeftBehindMap(tagState)
        }else null
        val showImage = safeAreaRepository.isShowImageEnabled(id).first()
        log("Sending disconnect notification for $name ($id). Image: $image ($showImage)")
        notifications.showNotification(notificationId, NotificationChannel.LEFT_BEHIND) {
            it.leftBehind(id, name, notificationId, image.takeIf { showImage })
        }
        leftBehindRepository.addLeftBehindTag(
            LeftBehindTag(
                id,
                getString(R.string.notification_title_disconnect),
                name,
                image
            )
        )
        //Remove this Job from the job map
        synchronized(tagDisconnectNotificationJobs) {
            tagDisconnectNotificationJobs.remove(id)
        }
    }

    private fun NotificationCompat.Builder.ongoing(
        isError: Boolean = false
    ) = apply {
        val connectedTags = synchronized(tagConnections) {
            tagConnections.filterValues { it is ConnectedTagConnection }
        }
        val passiveTags = synchronized(tagConnections) {
            tagConnections.filterValues { it is ScannedTagConnection }.filterKeys {
                passiveModeRepository.isInPassiveMode(it, ignoreBypass = true)
            }
        }
        val allTags = connectedTags + passiveTags
        val tagCount = allTags.size
        val tagNames = allTags.entries.mapNotNull {
            //Non-passive scanned states are filtered out above
            val isPassive = it.value is ScannedTagConnection
            knownTagNames.value?.get(it.key.hashCode())?.let { name ->
                if(isPassive) {
                    getString(R.string.notification_title_background_service_passive, name)
                }else name
            }
        }
        val notificationIntent =
            Intent(this@UTagForegroundService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        val title = when {
            isError -> {
                getString(R.string.notification_title_background_service_error_short)
            }
            tagCount == 0 -> {
                //Special treatment for zero as Android does not consider it in plurals in English
                getString(R.string.notification_title_background_service_zero)
            }
            else -> {
                resources.getQuantityString(
                    R.plurals.notification_title_background_service,
                    tagCount,
                    tagCount
                )
            }
        }
        val content = when {
            isError -> {
                getString(R.string.notification_content_background_service_error)
            }
            tagNames.isNotEmpty() -> {
                tagNames.joinToString(", ")
            }
            else -> null
        }
        setContentTitle(title)
        setContentText(content)
        setSmallIcon(R.drawable.ic_notification)
        setOngoing(true)
        setShowWhen(false)
        setContentIntent(
            PendingIntent.getActivity(
                this@UTagForegroundService,
                PendingIntentId.FOREGROUND_SERVICE.ordinal,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        //Disable grouping for ongoing notification so it doesn't rise in the event of an error
        setGroup("ongoing")
        setTicker(title)
        if(isError) {
            addAction(
                0,
                getString(R.string.notification_action_background_service_error_action),
                retryPendingIntent
            )
        }
    }

    private fun NotificationCompat.Builder.error() = apply {
        val notificationIntent =
            Intent(this@UTagForegroundService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        setContentTitle(getString(R.string.notification_title_background_service_error))
        setContentText(getString(R.string.notification_content_background_service_error))
        setSmallIcon(R.drawable.ic_notification_error)
        setOngoing(false)
        setContentIntent(
            PendingIntent.getActivity(
                this@UTagForegroundService,
                PendingIntentId.FOREGROUND_SERVICE.ordinal,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        setTicker(getString(R.string.notification_title_background_service_error))
        addAction(
            0,
            getString(R.string.notification_action_background_service_error_action),
            retryPendingIntent
        )
    }

    private fun NotificationCompat.Builder.findMyDeviceError() = apply {
        val notificationIntent =
            Intent(this@UTagForegroundService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        setContentTitle(getString(R.string.notification_title_find_my_device))
        setContentText(getString(R.string.notification_content_find_my_device_error))
        setSmallIcon(R.drawable.ic_notification_error)
        setOngoing(false)
        setAutoCancel(true)
        setContentIntent(
            PendingIntent.getActivity(
                this@UTagForegroundService,
                PendingIntentId.FIND_MY_DEVICE.ordinal,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        setTicker(getString(R.string.notification_title_find_my_device))
    }

    private fun NotificationCompat.Builder.leftBehind(
        deviceId: String,
        label: String,
        id: Int,
        image: Bitmap?
    ) = apply {
        val notificationIntent = TagActivity_createIntent(deviceId).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        setContentTitle(getString(R.string.notification_title_disconnect))
        setContentText(getString(R.string.notification_content_disconnect, label))
        setSmallIcon(R.drawable.ic_notification)
        setOngoing(false)
        setAutoCancel(true)
        setGroup("left_behind")
        setContentIntent(
            PendingIntent.getActivity(
                this@UTagForegroundService,
                id,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        setTicker(getString(R.string.notification_title_disconnect))
        if(image != null) {
            setStyle(NotificationCompat.BigPictureStyle().bigPicture(image))
        }
    }

    private fun NotificationCompat.Builder.automationError() = apply {
        val notificationIntent =
            Intent(this@UTagForegroundService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        setContentTitle(getString(R.string.notification_title_automation))
        setContentText(getString(R.string.notification_content_automation_error))
        setSmallIcon(R.drawable.ic_notification_error)
        setOngoing(false)
        setAutoCancel(true)
        setContentIntent(
            PendingIntent.getActivity(
                this@UTagForegroundService,
                PendingIntentId.AUTOMATION.ordinal,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        setTicker(getString(R.string.notification_title_automation))
    }

    private fun NotificationCompat.Builder.bluetoothError(pendingIntent: PendingIntent?) = apply {
        val notificationIntent =
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        val bluetoothIntent = pendingIntent ?: PendingIntent.getActivity(
            this@UTagForegroundService,
            PendingIntentId.BLUETOOTH.ordinal,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        setContentTitle(getString(R.string.notification_title_bluetooth_error))
        setContentText(getString(R.string.notification_content_bluetooth_error))
        setSmallIcon(R.drawable.ic_notification_error)
        setOngoing(true)
        setAutoCancel(false)
        setContentIntent(bluetoothIntent)
        setTicker(getString(R.string.notification_title_bluetooth_error))
    }

    private fun NotificationCompat.Builder.safeAreaError(@StringRes content: Int) = apply {
        val notificationIntent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            }
        setContentTitle(getString(R.string.notification_title_safe_area))
        setContentText(getString(content))
        setSmallIcon(R.drawable.ic_notification_error)
        setOngoing(true)
        setAutoCancel(false)
        setContentIntent(
            PendingIntent.getActivity(
                this@UTagForegroundService,
                PendingIntentId.SAFE_AREA_PERMISSIONS.ordinal,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        setTicker(getString(R.string.notification_title_safe_area))
        addAction(
            0,
            getString(R.string.notification_action_safe_area_retry),
            safeAreaRetryPendingIntent
        )
    }

    private fun NotificationCompat.Builder.batteryOptimisation(`package`: String) = apply {
        val notificationIntent = BatteryOptimisationTrampolineActivity.createIntent(
            this@UTagForegroundService, `package`
        )
        val appName = when(`package`) {
            BuildConfig.APPLICATION_ID -> getString(R.string.app_name)
            PACKAGE_NAME_ONECONNECT -> getString(R.string.app_name_st)
            else -> null
        }
        setContentTitle(getString(R.string.notification_title_background_service_battery))
        setContentText(
            getString(R.string.notification_content_background_service_battery, appName)
        )
        setSmallIcon(R.drawable.ic_notification_error)
        setOngoing(false)
        setContentIntent(
            PendingIntent.getActivity(
                this@UTagForegroundService,
                PendingIntentId.FOREGROUND_SERVICE.ordinal,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        setTicker(getString(R.string.notification_title_background_service_battery))
    }

    private fun NotificationCompat.Builder.smartThingsPermissions() = apply {
        val notificationIntent = Intent(this@UTagForegroundService, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        setContentTitle(getString(R.string.notification_title_background_service_permissions))
        setContentText(getString(R.string.notification_content_background_service_permissions))
        setSmallIcon(R.drawable.ic_notification_error)
        setOngoing(false)
        setContentIntent(
            PendingIntent.getActivity(
                this@UTagForegroundService,
                PendingIntentId.FOREGROUND_SERVICE.ordinal,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        setTicker(getString(R.string.notification_title_background_service_permissions))
        addAction(
            0,
            getString(R.string.notification_action_background_service_error_action),
            retryPendingIntent
        )
    }

    private fun onTagStatesChanged() = whenCreated {
        val connectedDeviceIds = synchronized(tagConnections) {
            tagConnections.filterValues {
                it is ConnectedTagConnection
            }.keys.toTypedArray()
        }
        val scannedDeviceIds = synchronized(tagConnections) {
            tagConnections.filterValues {
                it is ScannedTagConnection
            }.keys.toTypedArray()
        }
        val passiveTags = synchronized(tagConnections) {
            tagConnections.filterValues { it is ScannedTagConnection }.filterKeys {
                passiveModeRepository.isInPassiveMode(it)
            }
        }
        updateNotification()
        smartTagRepository.setConnectedTagCount(connectedDeviceIds.size + passiveTags.size)
        tagStateCallbacks.entries.toSet().forEach {
            try {
                it.value.onConnectedTagsChanged(connectedDeviceIds, scannedDeviceIds)
            }catch (e: RemoteException) {
                //Callback has died
                tagStateCallbacks.remove(it.key)
            }
        }
    }

    /**
     *  The way that SmartTags work for Unknown Tag scanning (UTS) requires them to be disconnected
     *  from the owner for at least 24h before showing up. This works by the Tag writing a state
     *  which is then provided in the service data ([TagData.tagState]). To prevent this when a Tag
     *  is in passive mode (which normally means we don't connect unless the user opens uTag or
     *  SmartThings), we briefly override passive mode, connect to the Tag and then disconnect again
     *  to reset the flag.
     */
    private fun preventOvermatureOffline(deviceId: String) {
        //Don't run if this device is already connecting or passive mode is temp disabled
        if(passiveModeRepository.isInPassiveMode(deviceId, bypassOnly = true)) return
        log("Preventing overmature offline for $deviceId")
        passiveModeRepository.setTemporaryOverride(deviceId, true)
        runWithRemoteService {
            connect(deviceId)
        }
        //Don't wait for the connect, this will be called again if the Tag pings again
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleLocationAlarm() = whenCreated {
        log("Cancelling location alarm")
        alarmManager.cancel(locationServicePendingIntent)
        //Don't prompt user if the refresh period is disabled
        val next = locationRefreshPeriod.firstNotNull().getNext() ?: return@whenCreated
        //Disabled if not overridden, will be re-scheduled when saver is disabled
        if(powerManager.isPowerSaveMode && !locationOnBatterySaver.get()) return@whenCreated
        //Should never happen unless the user manages to revoke while running
        if(!alarmManager.canScheduleExactAlarmsCompat()) return@whenCreated
        log("Next location refresh: ${locationRefreshPeriod.value?.getNext()}")
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC,
            next.toInstant().toEpochMilli(),
            locationServicePendingIntent
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleWidgetAlarm() = whenCreated {
        log("Cancelling widget alarm")
        alarmManager.cancel(widgetServicePendingIntent)
        //Don't prompt user if the refresh period is disabled
        val next = widgetRefreshPeriod.firstNotNull().getNext() ?: return@whenCreated
        //Disabled if not overridden, will be re-scheduled when saver is disabled
        if(powerManager.isPowerSaveMode && !widgetOnBatterySaver.get()) return@whenCreated
        //Should never happen unless the user manages to revoke while running
        if(!alarmManager.canScheduleExactAlarmsCompat()) return@whenCreated
        log("Next widget refresh: ${widgetRefreshPeriod.value?.getNext()}")
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC,
            next.toInstant().toEpochMilli(),
            widgetServicePendingIntent
        )
    }

    private fun locateAndScheduleNext() = whenCreated {
        scheduleLocationAlarm()
        val results = ArrayList<SyncResult>()
        val tagConnectionIterator = synchronized(tagConnections) {
            tagConnections.values.iterator()
        }
        tagConnectionIterator.forEach {
            it.syncLocationAndWait(false).also { result ->
                results.add(result)
                log("Sync result for ${it.deviceId}: $result")
            }
        }
        nonOwnerRepository.onLocationUpdate()
        if(results.any { it == SyncResult.SUCCESS }) {
            //Update widgets & main UI
            SmartTagRepository.refreshTagStates(this@UTagForegroundService)
            historyWidgetRepository.updateWidgets()
        }
        //(Re)start scan if needed
        runWithRemoteService {
            startScan()
        }
    }

    private fun refreshWidgetAndScheduleNext() = whenCreated {
        scheduleWidgetAlarm()
        if(widgetRepository.hasWidgets().first()) {
            widgetRepository.updateWidgets()
        }
        if(historyWidgetRepository.hasWidgets().first()) {
            historyWidgetRepository.updateWidgets()
        }
    }

    private fun refreshWidgets(widgetId: Int) = whenCreated {
        when {
            widgetRepository.getWidget(widgetId) != null -> {
                widgetRepository.updateWidget(widgetId)
            }
            historyWidgetRepository.getWidget(widgetId) != null -> {
                historyWidgetRepository.updateWidget(widgetId)
            }
        }
    }

    private suspend fun updateNotification(force: Boolean = false) {
        //Prevent updating the notification if the state hasn't actually changed
        val bluetoothEnabled = bluetoothEnabled.value.enabled
        val notificationHash = listOf(tagConnections, bluetoothEnabled).hashCode()
        if(this.notificationHash != notificationHash || force) {
            this.notificationHash = notificationHash
            val bluetoothIntent = if(!bluetoothEnabled) {
                smartThingsRepository.getEnableBluetoothIntent()
            }else null
            notifications.showNotification(
                NotificationId.FOREGROUND_SERVICE,
                NotificationChannel.FOREGROUND_SERVICE
            ) {
                if(bluetoothEnabled) {
                    it.ongoing()
                }else{
                    it.bluetoothError(bluetoothIntent)
                }
            }
        }
    }

    private fun ISmartTagSupportService.connectAndNotifyResult(deviceId: String) {
        val result = connect(deviceId)
        synchronized(tagConnectCallbacks) {
            tagConnectCallbacks.forEach { it.value.onTagConnectResult(deviceId, result) }
        }
    }
    
    private fun log(message: String) {
        if(debugEnabled.value != true) return
        Log.d("UTAG", message)
        //Only log to file on debug builds
        if(!BuildConfig.DEBUG) return
        openFileOutput("log.txt", Context.MODE_APPEND).use {
            it.write("${LocalDateTime.now()}: $message".toByteArray())
            it.write("\n".toByteArray())
        }
    }

    private inner class IUTagServiceImpl: IUTagService.Stub() {
        override fun addTagStateCallback(callback: ITagStateCallback): String {
            val callbackId = UUID.randomUUID().toString()
            val connectedDeviceIds = synchronized(tagConnections) {
                tagConnections.filterValues {
                    it is ConnectedTagConnection
                }.keys.toTypedArray()
            }
            val scannedDeviceIds = synchronized(tagConnections) {
                tagConnections.filterValues {
                    it is ScannedTagConnection
                }.keys.toTypedArray()
            }
            callback.onConnectedTagsChanged(connectedDeviceIds, scannedDeviceIds)
            synchronized(tagStateCallbacks) {
                tagStateCallbacks[callbackId] = callback
            }
            return callbackId
        }

        override fun removeTagStateCallback(callbackId: String) {
            synchronized(tagStateCallbacks) {
                tagStateCallbacks.remove(callbackId)
            }
        }

        override fun addTagStatusCallback(callback: ITagStatusCallback): String {
            val callbackId = UUID.randomUUID().toString()
            synchronized(tagStatusCallbacks) {
                tagStatusCallbacks[callbackId] = callback
            }
            return callbackId
        }

        override fun removeTagStatusCallback(callbackId: String) {
            synchronized(tagStatusCallbacks) {
                tagStatusCallbacks.remove(callbackId)
            }
        }

        override fun addTagConnectResultCallback(callback: ITagConnectResultCallback): String {
            val callbackId = UUID.randomUUID().toString()
            synchronized(tagConnectCallbacks) {
                tagConnectCallbacks[callbackId] = callback
            }
            return callbackId
        }

        override fun removeTagConnectResultCallback(callbackId: String?) {
            synchronized(tagStatusCallbacks) {
                tagConnectCallbacks.remove(callbackId)
            }
        }

        override fun addAutoSyncLocationCallback(callback: ITagAutoSyncLocationCallback): String {
            val callbackId = UUID.randomUUID().toString()
            synchronized(tagAutoSyncLocationCallbacks) {
                tagAutoSyncLocationCallbacks[callbackId] = callback
                //Send any currently syncing IDs
                val autoSyncingIds = synchronized(tagConnections) {
                    tagConnections.filter { it.value.isAutoSyncing }.map { it.key }
                }
                autoSyncingIds.forEach {
                    callback.onStartSync(it)
                }
            }
            return callbackId
        }

        override fun removeAutoSyncLocationCallback(callbackId: String) {
            synchronized(tagAutoSyncLocationCallbacks) {
                tagAutoSyncLocationCallbacks.remove(callbackId)
            }
        }

        override fun onGeofenceIntentReceived(intent: Intent) {
            onGeofenceReceived(intent)
        }

        override fun onLocationPermissionsChanged() {
            whenCreated {
                safeAreaSetupBus.emit(System.currentTimeMillis())
            }
        }

        override fun startTagScanNow(timeout: Long) {
            whenCreated {
                startManualScan(timeout)
            }
        }

        override fun syncLocation(deviceId: String, onDemand: Boolean, callback: IStringCallback) {
            val connection = tagConnections[deviceId]
            connection?.syncLocation(onDemand) {
                try {
                    callback.onResult(it.name)
                } catch (e: Exception) {
                    //Died
                }
            } ?: run {
                try {
                    callback.onResult(SyncResult.FAILED_TO_CONNECT.name)
                } catch (e: Exception) {
                    //Died
                }
            }
        }

        override fun getTagBatteryLevel(deviceId: String, callback: IStringCallback) {
            runEnum(deviceId, callback) {
                getBatteryLevel()
            }
        }

        override fun startTagRinging(deviceId: String, callback: IStringCallback) {
            runEnum(deviceId, callback) {
                startRinging()
            }
        }

        override fun setTagRingVolume(
            deviceId: String,
            volumeLevel: String,
            callback: IBooleanCallback
        ) {
            runBoolean(deviceId, callback) {
                setRingVolume(VolumeLevel.valueOf(volumeLevel))
            }
        }

        override fun stopTagRinging(deviceId: String, callback: IBooleanCallback) {
            runBoolean(deviceId, callback) {
                stopRinging()
            }
        }

        override fun startTagRanging(
            deviceId: String,
            config: ByteArray,
            callback: IBooleanCallback
        ) {
            runBoolean(deviceId, callback) {
                startRanging(config)
            }
        }

        override fun stopTagRanging(deviceId: String, callback: IBooleanCallback) {
            runBoolean(deviceId, callback) {
                stopRanging()
            }
        }

        override fun setButtonConfig(
            deviceId: String,
            pressEnabled: Boolean,
            holdEnabled: Boolean,
            callback: IBooleanCallback
        ) {
            runBoolean(deviceId, callback) {
                setButtonConfig(pressEnabled, holdEnabled)
            }
        }

        override fun getButtonVolumeLevel(deviceId: String, callback: IStringCallback) {
            runEnum(deviceId, callback) {
                getButtonVolume()
            }
        }

        override fun setButtonVolumeLevel(
            deviceId: String,
            volumeLevel: String,
            callback: IBooleanCallback
        ) {
            runBoolean(deviceId, callback) {
                setButtonVolume(ButtonVolumeLevel.fromValue(volumeLevel))
            }
        }

        override fun getE2EEnabled(deviceId: String, callback: IBooleanCallback) {
            runBoolean(deviceId, callback) {
                isE2EEnabled()
            }
        }

        override fun setE2EEnabled(
            deviceId: String,
            enabled: Boolean,
            callback: IBooleanCallback
        ) {
            runBoolean(deviceId, callback) {
                setE2EEnabled(enabled)
            }
        }

        override fun getLostModeUrl(deviceId: String, callback: IStringCallback) {
            return runString(deviceId, callback) {
                getLostModeUrl()
            }
        }

        override fun setLostModeUrl(deviceId: String, url: String, callback: IBooleanCallback) {
            return runBoolean(deviceId, callback) {
                setLostModeUrl(url)
            }
        }

        override fun startTagReadRssi(
            deviceId: String,
            refreshRate: Long,
            callback: IGattRssiCallback
        ): Boolean {
            val connection = getTagConnection(deviceId) ?: return false
            return connection.startReadRemoteRssi(refreshRate, callback)
        }

        override fun stopTagReadRssi(deviceId: String): Boolean {
            val connection = getTagConnection(deviceId) ?: return false
            return connection.stopReadRemoteRssi()
        }

        override fun killProcess() {
            exitProcess(0)
        }

        private fun runBoolean(
            deviceId: String,
            callback: IBooleanCallback,
            block: suspend ConnectedTagConnection.() -> Boolean?
        ) {
            val connection = getTagConnection(deviceId) ?: run {
                callback.onResult(false)
                return
            }
            whenCreated {
                try {
                    callback.onResult(block(connection) ?: false)
                }catch (e: Exception) {
                    //Died
                }
            }
        }

        private fun runEnum(
            deviceId: String,
            callback: IStringCallback,
            block: suspend ConnectedTagConnection.() -> Enum<*>?
        ) {
            val connection = getTagConnection(deviceId) ?: run {
                try {
                    callback.onResult(null)
                }catch (e: Exception) {
                    //Died
                }
                return
            }
            whenCreated {
                try {
                    callback.onResult(block(connection)?.name)
                }catch (e: Exception) {
                    //Died
                }
            }
        }

        private fun runString(
            deviceId: String,
            callback: IStringCallback,
            block: suspend ConnectedTagConnection.() -> String?
        ) {
            val connection = getTagConnection(deviceId) ?: run {
                try {
                    callback.onResult(null)
                }catch (e: Exception) {
                    //Died
                }
                return
            }
            whenCreated {
                try {
                    callback.onResult(block(connection))
                }catch (e: Exception) {
                    //Died
                }
            }
        }

        private fun getTagConnection(deviceId: String): ConnectedTagConnection? {
            return tagConnections[deviceId] as? ConnectedTagConnection
        }
    }

}