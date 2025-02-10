package com.kieronquinn.app.utag.repositories

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.StringRes
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.kieronquinn.app.utag.Application.Companion.isMainProcess
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.EncryptedValueConverter.ENCRYPTION_TRANSFORMATION
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.model.database.WidgetConfig
import com.kieronquinn.app.utag.repositories.NotificationRepository.PendingIntentId
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState.Loaded.LocationState
import com.kieronquinn.app.utag.repositories.WidgetRepository.AppWidgetConfig
import com.kieronquinn.app.utag.repositories.WidgetRepositoryImpl.WidgetTagState.Error.ErrorType
import com.kieronquinn.app.utag.service.UTagForegroundService
import com.kieronquinn.app.utag.service.UTagForegroundService.Companion.ACTION_REFRESH_WIDGET
import com.kieronquinn.app.utag.service.UTagForegroundService.Companion.EXTRA_APP_WIDGET_ID
import com.kieronquinn.app.utag.ui.activities.WidgetErrorMessageActivity
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.formatTime
import com.kieronquinn.app.utag.utils.extensions.getWidgetSizes
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import com.kieronquinn.app.utag.utils.extensions.toStringSet
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper.RoomEncryptionFailedCallback
import com.kieronquinn.app.utag.xposed.extensions.TagActivity_createIntent
import com.kieronquinn.app.utag.xposed.extensions.applySecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import dev.oneuiproject.oneui.R as OneuiR

interface WidgetRepository: RoomEncryptionFailedCallback {

    /**
     *  Returns if there are any widgets of this type
     */
    fun hasWidgets(): Flow<Boolean>

    /**
     *  Updates all widgets, via [SmartTagRepository]
     */
    fun updateWidgets()

    /**
     *  Updates all widgets, as [updateWidgets], but sets the current widget as loading first
     */
    fun updateWidget(appWidgetId: Int)

    /**
     *  Updates a widget's map image size, without refreshing
     */
    fun onWidgetSizeChanged(appWidgetId: Int)

    /**
     *  Updates a widget's map image theme, without refreshing
     */
    fun onWidgetThemeChanged()

    /**
     *  Returns the raw RemoteViews for a widget, used in the preview
     */
    suspend fun getRemoteViews(
        tagStates: List<TagState>,
        config: AppWidgetConfig,
        width: Int,
        height: Int
    ): RemoteViews?

    /**
     *  Delete the preview images used when configuring this widget
     */
    fun clearPreviewImages(appWidgetId: Int)

    /**
     *  Get a current config from the database
     */
    suspend fun getWidget(appWidgetId: Int): AppWidgetConfig?

    /**
     *  Create or update a config in the database
     */
    suspend fun updateWidget(appWidgetConfig: AppWidgetConfig)

    /**
     *  Delete all data related to the widget in the table, as well as its images
     */
    fun onWidgetsDeleted(appWidgetIds: List<Int>)

    /**
     *  Returns a list of launchers which have bound widgets, used to know if an app can access
     *  the encrypted file provider.
     */
    fun getAllowedPackages(): Set<String>

    /**
     *  Decrypts a given stored encrypted widget image
     */
    fun decryptImage(bytes: ByteArray): ByteArray

    data class AppWidgetConfig(
        val appWidgetId: Int,
        val packageName: String,
        val deviceIds: Set<String> = emptySet(),
        val openDeviceId: String? = null,
        val statusDeviceId: String? = null,
        val mapStyle: MapStyle = MapStyle.NORMAL,
        val mapTheme: MapTheme = MapTheme.SYSTEM
    )

}

class WidgetRepositoryImpl(
    private val staticMapRepository: StaticMapRepository,
    private val smartTagRepository: SmartTagRepository,
    private val context: Context,
    encryptedSettingsRepository: EncryptedSettingsRepository,
    database: UTagDatabase
): WidgetRepository {

    companion object {
        private const val MAX_ZOOM_LEVEL = 17f
        private const val FILENAME_PREFIX_WIDGET = "widget"
    }

    private val widgetConfigTable = database.widgetConfigTable()
    private val refreshSizesRequest = MutableStateFlow<RefreshSizesRequest?>(null)
    private val refreshThemeRequest = MutableStateFlow(System.currentTimeMillis())
    private var cachedRemoteViews = HashMap<Int, (Boolean) -> RemoteViews?>()
    private val remoteViewsLock = Mutex()
    private val scope = MainScope()

    private val decryptionCipher by lazy {
        Cipher.getInstance(ENCRYPTION_TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                encryptedSettingsRepository.getDatabaseEncryptionKey(),
                encryptedSettingsRepository.getDatabaseEncryptionIV()
            )
        }
    }

    private val appWidgetManager =
        context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager

    private val loadingRemoteViews by lazy {
        RemoteViews(context.packageName, R.layout.widget_utag_loading)
    }

    /**
     *  All current widget configs from the database. Used to lookup current states.
     */
    private val allWidgetConfigs = widgetConfigTable.getAllConfigs().map { configs ->
        configs.map { it.toAppWidgetConfig() }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     *  Split widget configs from the database, mapped to their app widget IDs
     */
    private val widgetConfigs = widgetConfigTable.getAppWidgetIds().map { ids ->
        ids.associateWith {
            widgetConfigTable.getConfig(it).map { config ->
                config?.toAppWidgetConfig()
            }
        }
    }

    /**
     *  All current device IDs used in widgets in the database, as a set
     */
    private val deviceIds = widgetConfigTable.getDeviceIds().map { ids ->
        ids.map { it.toStringSet() }.flatten().toSet()
    }

    /**
     *  All current tag states for the [deviceIds]
     */
    private val tagStates = deviceIds.map {
        if(it.isEmpty()) return@map emptyMap()
        it.associateWith { deviceId -> smartTagRepository.getTagState(deviceId) }
    }

    /**
     *  Widget configs mapped to their required states and widget sizes
     */
    private val widgetConfigsWithStates = widgetConfigs.filterNotNull().map { flows ->
        flows.map { flow ->
            val refreshRequest = refreshSizesRequest.filter {
                it == null || it.id == flow.key
            }
            combine(
                flow.value,
                tagStates,
                refreshRequest,
                refreshThemeRequest
            ) { config, tagStates, _, _ ->
                if(config == null) return@combine null
                val tags = tagStates.filter { config.deviceIds.contains(it.key) }.map { it.value }
                val sizes = appWidgetManager.getWidgetSizes(context, config.appWidgetId)
                SizedWidgetConfigWithStates(config, sizes, tags)
            }
        }
    }

    /**
     *  Merges all required configs into a single flow and calls [loadRemoteViews] for each widget
     */
    private val remoteViews = widgetConfigsWithStates.mapLatest { flows ->
        flows.map { flow ->
            flow.flatMapLatest { config ->
                if(config == null) return@flatMapLatest flowOf(null)
                combine(*config.tagStates.toTypedArray()) { states ->
                    Pair(config, states)
                }.debounce(500L).map {
                    val widgetConfig = it.first
                    val states = it.second.toList()
                    val width = widgetConfig.sizes?.first?.width ?: return@map null
                    val height = widgetConfig.sizes.first.height
                    val landscapeWidth = widgetConfig.sizes.second.width
                    val landscapeHeight = widgetConfig.sizes.second.height
                    loadRemoteViews(
                        widgetConfig,
                        states,
                        width,
                        height,
                        landscapeWidth,
                        landscapeHeight
                    )
                }
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     *  Loads RemoteViews into a widget and sends to [AppWidgetManager]. This is done on a separate
     *  coroutine to prevent the flow cutting it off and breaking the refresh, which would cause
     *  race conditions, so it's also locked.
     */
    private fun loadRemoteViews(
        widgetConfig: SizedWidgetConfigWithStates,
        states: List<TagState>,
        width: Int,
        height: Int,
        landscapeWidth: Int,
        landscapeHeight: Int
    ) = scope.launch {
        remoteViewsLock.withLock {
            val remoteViewsBuilder = getRemoteViews(
                widgetConfig.config,
                states.mapNotNull { state ->
                    state.toWidgetTagState(widgetConfig.config.statusDeviceId)
                },
                width,
                height,
                landscapeWidth,
                landscapeHeight
            )
            val remoteViews = remoteViewsBuilder.invoke(false)
            appWidgetManager.updateAppWidget(widgetConfig.config.appWidgetId, remoteViews)
            cachedRemoteViews[widgetConfig.config.appWidgetId] = remoteViewsBuilder
        }
    }

    override fun hasWidgets(): Flow<Boolean> {
        return allWidgetConfigs.filterNotNull().map { it.isNotEmpty() }
    }

    override fun updateWidgets() {
        scope.launch {
            smartTagRepository.refreshTagStates()
        }
    }

    override fun updateWidget(appWidgetId: Int) {
        val current = cachedRemoteViews[appWidgetId]
        if(current != null) {
            appWidgetManager.updateAppWidget(appWidgetId, current(true))
        }else{
            appWidgetManager.updateAppWidget(appWidgetId, loadingRemoteViews)
        }
        updateWidgets()
    }

    override fun onWidgetSizeChanged(appWidgetId: Int) {
        scope.launch {
            appWidgetManager.updateAppWidget(appWidgetId, loadingRemoteViews)
            refreshSizesRequest.emit(RefreshSizesRequest(appWidgetId))
        }
    }

    override fun onWidgetThemeChanged() {
        scope.launch {
            allWidgetConfigs.value?.map { it.appWidgetId }?.toIntArray()?.let {
                appWidgetManager.updateAppWidget(it, loadingRemoteViews)
            }
            refreshThemeRequest.emit(System.currentTimeMillis())
        }
    }

    override suspend fun getRemoteViews(
        tagStates: List<TagState>,
        config: AppWidgetConfig,
        width: Int,
        height: Int
    ): RemoteViews? {
        val widgetTagStates = tagStates.mapNotNull {
            it.toWidgetTagState(config.statusDeviceId)
        }
        return getRemoteViews(config, widgetTagStates, width, height, forPreview = true)
            .invoke(false)
    }

    override fun clearPreviewImages(appWidgetId: Int) {
        clearImages(appWidgetId, true)
    }

    override suspend fun getWidget(appWidgetId: Int): AppWidgetConfig? {
        return allWidgetConfigs.firstNotNull().firstOrNull { it.appWidgetId == appWidgetId }
    }

    override suspend fun updateWidget(config: AppWidgetConfig) {
        withContext(Dispatchers.IO) {
            widgetConfigTable.insert(
                WidgetConfig(
                    config.appWidgetId,
                    config.packageName,
                    config.deviceIds.toEncryptedValue(),
                    config.openDeviceId?.toEncryptedValue(),
                    config.statusDeviceId?.toEncryptedValue(),
                    config.mapStyle,
                    config.mapTheme
                )
            )
        }
    }

    override fun onWidgetsDeleted(appWidgetIds: List<Int>) {
        scope.launch(Dispatchers.IO) {
            appWidgetIds.forEach {
                widgetConfigTable.delete(it)
                clearImages(it, false)
            }
        }
    }

    override fun getAllowedPackages(): Set<String> {
        return runBlocking {
            allWidgetConfigs.firstNotNull().map { it.packageName }.toSet()
        }
    }

    @Synchronized
    override fun decryptImage(bytes: ByteArray): ByteArray {
        return decryptionCipher.doFinal(bytes)
    }

    private fun clearImages(appWidgetId: Int, preview: Boolean) {
        val suffix = if(preview) "P" else ""
        staticMapRepository.clearFile("${FILENAME_PREFIX_WIDGET}_${appWidgetId}_$suffix")
        staticMapRepository.clearFile("${FILENAME_PREFIX_WIDGET}_${appWidgetId}_L$suffix")
    }

    private suspend fun getRemoteViews(
        config: AppWidgetConfig,
        tagStates: List<WidgetTagState>,
        width: Int,
        height: Int,
        widthLandscape: Int? = null,
        heightLandscape: Int? = null,
        forPreview: Boolean = false
    ): (isLoading: Boolean) -> RemoteViews? {
        val deviceIds = tagStates.map { it.deviceId }
        //If any Tag has an error, the error icon will be displayed at least
        val firstError = tagStates.firstOrNull {
            it is WidgetTagState.Error
        } as? WidgetTagState.Error
        //Influences the toast message that appears when the icon is clicked
        val hasEncryptionError = tagStates.any {
            it is WidgetTagState.Error && it.type == ErrorType.ENCRYPTION
        }
        //If all Tags are error, we display a full size error message
        val isFatalError = tagStates.all { it is WidgetTagState.Error }
        val timestamp = tagStates.filterIsInstance<WidgetTagState.Loaded>().firstOrNull()
            ?.location?.timestamp
        val hash = config.hashCode()
        val openDeviceId = config.openDeviceId ?: deviceIds.first()
        val clickPendingIntent = getClickPendingIntent(config.appWidgetId, openDeviceId)
        if(isFatalError && firstError != null || timestamp == null) {
            return {
                getErrorRemoteViews(
                    when {
                        hasEncryptionError -> ErrorType.ENCRYPTION
                        firstError != null -> firstError.type
                        else -> ErrorType.LOCATION
                    },
                    config.appWidgetId,
                    it,
                    clickPendingIntent,
                    forPreview
                )
            }
        }
        val locations = tagStates.mapNotNull { (it as? WidgetTagState.Loaded)?.location }
        val image = locations.getMapImage(
            width,
            height,
            config.mapStyle,
            config.mapTheme,
            config.appWidgetId,
            false,
            hash,
            timestamp,
            forPreview
        ) ?: return {
            getErrorRemoteViews(
                ErrorType.LOCATION,
                config.appWidgetId,
                it,
                clickPendingIntent,
                forPreview
            )
        }
        val landscapeImage = if(widthLandscape != null && heightLandscape != null) {
            locations.getMapImage(
                widthLandscape,
                heightLandscape,
                config.mapStyle,
                config.mapTheme,
                config.appWidgetId,
                true,
                hash,
                timestamp,
                forPreview
            ) ?: return {
                getErrorRemoteViews(
                    ErrorType.LOCATION,
                    config.appWidgetId,
                    it,
                    clickPendingIntent,
                    forPreview
                )
            }
        }else null
        //Try to use the user-selected status ID if it's available
        val statusTimestamp = tagStates.firstOrNull { it.deviceId == config.statusDeviceId }?.let {
            (it as? WidgetTagState.Loaded)?.location?.time
        } ?: tagStates.firstNotNullOfOrNull {
            //Otherwise use the first status
            (it as? WidgetTagState.Loaded)?.location?.time
        } ?: timestamp //Otherwise use the update time
        val formattedTimestamp = context.formatTime(
            statusTimestamp, true, R.string.widget_timestamp
        )
        //Try to use the user-selected status ID if it's available
        val contentDescription = tagStates.firstOrNull { it.deviceId == config.statusDeviceId }?.let {
            (it as? WidgetTagState.Loaded)?.location?.address
        } ?: tagStates.firstNotNullOfOrNull {
            //Otherwise use the first status
            (it as? WidgetTagState.Loaded)?.location?.address
        } ?: formattedTimestamp
        //Try to use the user-selected status ID if it's available
        val tagName = tagStates.firstOrNull { it.deviceId == config.statusDeviceId }?.let {
            (it as? WidgetTagState.Loaded)?.label
        } ?: tagStates.firstNotNullOfOrNull {
            //Otherwise use the first status
            (it as? WidgetTagState.Loaded)?.label
        } ?: context.getString(R.string.app_name)
        val refreshPendingIntent = getRefreshPendingIntent(config.appWidgetId)
        val modifiers: RemoteViews.(isLoading: Boolean) -> Unit = { isLoading ->
            setTextViewText(R.id.widget_utag_map_status, formattedTimestamp)
            setOnClickPendingIntent(R.id.widget_utag_map_refresh, refreshPendingIntent)
            setOnClickPendingIntent(android.R.id.background, clickPendingIntent)
            setContentDescription(R.id.widget_utag_map_image, contentDescription)
            setContentDescription(R.id.widget_utag_map_logo, tagName)
            if(firstError != null) {
                setOnClickPendingIntent(
                    R.id.widget_utag_map_error,
                    getErrorClickPendingIntent(config.appWidgetId, firstError.type)
                )
                setViewVisibility(R.id.widget_utag_map_error, View.VISIBLE)
            }else{
                setViewVisibility(R.id.widget_utag_map_error, View.GONE)
            }
            if(isLoading) {
                setViewVisibility(R.id.widget_utag_map_refresh, View.GONE)
                setViewVisibility(R.id.widget_utag_map_progress, View.VISIBLE)
            }else{
                setViewVisibility(R.id.widget_utag_map_refresh, View.VISIBLE)
                setViewVisibility(R.id.widget_utag_map_progress, View.GONE)
            }
            if(forPreview) {
                setViewVisibility(R.id.widget_utag_map_refresh, View.GONE)
                setViewVisibility(R.id.widget_utag_map_progress, View.GONE)
            }
        }
        val remoteViews = { isLoading: Boolean ->
            if (landscapeImage != null) {
                RemoteViews(
                    RemoteViews(context.packageName, R.layout.widget_utag_map).apply {
                        modifiers(isLoading)
                        setImageViewUri(R.id.widget_utag_map_image, landscapeImage)
                    },
                    RemoteViews(context.packageName, R.layout.widget_utag_map).apply {
                        modifiers(isLoading)
                        setImageViewUri(R.id.widget_utag_map_image, image)
                    }
                )
            } else {
                RemoteViews(context.packageName, R.layout.widget_utag_map).apply {
                    modifiers(isLoading)
                    setImageViewUri(R.id.widget_utag_map_image, image)
                }
            }
        }
        return remoteViews
    }

    private fun getErrorRemoteViews(
        errorType: ErrorType,
        appWidgetId: Int,
        isLoading: Boolean,
        clickPendingIntent: PendingIntent,
        forPreview: Boolean
    ): RemoteViews? {
        return RemoteViews(context.packageName, R.layout.widget_utag_error).apply {
            setTextViewText(
                R.id.widget_utag_map_error,
                context.getString(errorType.message)
            )
            setOnClickPendingIntent(
                R.id.widget_utag_map_refresh,
                getRefreshPendingIntent(appWidgetId)
            )
            setOnClickPendingIntent(android.R.id.background, clickPendingIntent)
            if(isLoading) {
                setViewVisibility(R.id.widget_utag_map_refresh, View.GONE)
                setViewVisibility(R.id.widget_utag_map_progress, View.VISIBLE)
            }else{
                setViewVisibility(R.id.widget_utag_map_refresh, View.VISIBLE)
                setViewVisibility(R.id.widget_utag_map_progress, View.GONE)
            }
            if(forPreview) {
                //Don't show refresh icon on preview
                setImageViewResource(R.id.widget_utag_map_refresh, OneuiR.drawable.ic_oui_error)
            }
        }
    }

    private fun TagState.toWidgetTagState(selectedDeviceId: String?): WidgetTagState? {
        return when {
            this is TagState.Loaded && locationState != null -> {
                val location = when(locationState) {
                    is LocationState.Location -> Location(
                        locationState.latLng,
                        locationState.address,
                        locationState.time,
                        device.markerIcons,
                        timestamp,
                        deviceId == selectedDeviceId
                    )
                    is LocationState.PINRequired, is LocationState.NoKeys -> {
                        return WidgetTagState.Error(deviceId, ErrorType.ENCRYPTION)
                    }
                    is LocationState.NoLocation, is LocationState.NotAllowed -> {
                        return WidgetTagState.Error(deviceId, ErrorType.NO_LOCATION)
                    }
                }
                WidgetTagState.Loaded(deviceId, location, device.label)
            }
            this is TagState.Error -> WidgetTagState.Error(deviceId, ErrorType.LOCATION)
            else -> null
        }
    }

    override fun onEncryptionFailed() {
        scope.launch {
            //Nuke the database since the values will no longer decrypt
            widgetConfigTable.clear()
            //Clear all encrypted files since they will also no longer decrypt
            staticMapRepository.clearAllFiles()
        }
    }

    private fun setupWidgets() = scope.launch {
        remoteViews.collect { flows ->
            flows.forEach {
                launch {
                    it.filterNotNull().collect()
                }
            }
        }
    }

    init {
        //We only want to run these from the service process
        if(!isMainProcess()) {
            setupWidgets()
        }
    }

    private suspend fun List<Location>.getMapImage(
        width: Int,
        height: Int,
        style: MapStyle,
        theme: MapTheme,
        appWidgetId: Int,
        landscape: Boolean,
        hash: Int,
        timestamp: Long,
        preview: Boolean
    ): Uri? {
        if(isEmpty()) return null
        val zoomLevel = MAX_ZOOM_LEVEL
        val suffix = StringBuilder().apply {
            if(landscape) append("L")
            if(preview) append("P")
        }.toString()
        //If there's more than one location or we're in land, add additional padding by zooming out
        val zoomOut = landscape || size > 1
        return staticMapRepository.generateStaticMapUri(
            "${FILENAME_PREFIX_WIDGET}_${appWidgetId}_$suffix",
            width,
            height,
            zoomLevel,
            zoomOut,
            style,
            theme,
            true
        ) {
            map {
                val marker = if(it.selected) {
                    it.markers.first
                }else{
                    it.markers.second
                }
                val zIndex = if(it.selected) 1f else 0f
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(marker))
                    .anchor(0.5f, 0.96f)
                    .zIndex(zIndex)
                    .position(it.location)
            }
        }?.buildUpon()
            ?.appendQueryParameter("hash", hash.toString())
            ?.appendQueryParameter("timestamp", timestamp.toString())
            ?.build()
    }

    private fun getRefreshPendingIntent(appWidgetId: Int): PendingIntent {
        return PendingIntent.getService(
            context,
            "${PendingIntentId.WIDGET_REFRESH_PREFIX.ordinal}00$appWidgetId".toInt(),
            Intent(context, UTagForegroundService::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                `package` = context.packageName
                putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getClickPendingIntent(appWidgetId: Int, deviceId: String): PendingIntent {
        return PendingIntent.getActivity(
            context,
            "${PendingIntentId.WIDGET_CLICK_PREFIX.ordinal}00$appWidgetId".toInt(),
            TagActivity_createIntent(deviceId),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getErrorClickPendingIntent(appWidgetId: Int, errorType: ErrorType): PendingIntent {
        return PendingIntent.getActivity(
            context,
            "${PendingIntentId.WIDGET_ERROR_CLICK_PREFIX.ordinal}00$appWidgetId".toInt(),
            WidgetErrorMessageActivity.getIntent(context, errorType.toast),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private sealed class WidgetTagState(open val deviceId: String) {
        data class Loaded(
            override val deviceId: String,
            val location: Location,
            val label: String
        ): WidgetTagState(deviceId)

        data class Error(
            override val deviceId: String,
            val type: ErrorType
        ): WidgetTagState(deviceId) {
            enum class ErrorType(@StringRes val message: Int, @StringRes val toast: Int) {
                LOCATION(R.string.widget_error_location, R.string.widget_error_toast_location),
                ENCRYPTION(R.string.widget_error_encryption, R.string.widget_error_toast_encryption),
                NO_LOCATION(R.string.widget_error_no_location, R.string.widget_error_toast_location)
            }
        }
    }

    private data class Location(
        val location: LatLng,
        val address: String?,
        val time: Long,
        val markers: Pair<Bitmap, Bitmap>,
        val timestamp: Long,
        val selected: Boolean
    )

    private data class SizedWidgetConfigWithStates(
        val config: AppWidgetConfig,
        val sizes: Pair<Size, Size>?,
        val tagStates: List<Flow<TagState>>
    )

    private fun WidgetConfig.toAppWidgetConfig(): AppWidgetConfig {
        return AppWidgetConfig(
            appWidgetId = appWidgetId,
            packageName = packageName,
            deviceIds = deviceIds.toStringSet(),
            openDeviceId = openDeviceId?.let { String(it.bytes) },
            statusDeviceId = statusDeviceId?.let { String(it.bytes) },
            mapStyle = mapStyle,
            mapTheme = mapTheme
        )
    }

    private data class RefreshSizesRequest(val id: Int, val key: Long = System.currentTimeMillis())

}