package com.kieronquinn.app.utag.repositories

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.kieronquinn.app.utag.Application.Companion.isMainProcess
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.database.HistoryWidgetConfig
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository.AppWidgetConfig
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository.Companion.ACTION_REFRESH_HISTORY_WIDGETS
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepositoryImpl.HistoryWidgetState.Error.ErrorType
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.HistoryState
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.LocationHistoryPoint
import com.kieronquinn.app.utag.repositories.NotificationRepository.PendingIntentId
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.service.UTagForegroundService
import com.kieronquinn.app.utag.service.UTagForegroundService.Companion.ACTION_REFRESH_WIDGET
import com.kieronquinn.app.utag.service.UTagForegroundService.Companion.EXTRA_APP_WIDGET_ID
import com.kieronquinn.app.utag.ui.activities.WidgetErrorMessageActivity
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.utils.extensions.dip
import com.kieronquinn.app.utag.utils.extensions.dp
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.utils.extensions.formatTime
import com.kieronquinn.app.utag.utils.extensions.getWidgetSizes
import com.kieronquinn.app.utag.utils.extensions.scale
import com.kieronquinn.app.utag.utils.extensions.scaleAndRecycle
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper.RoomEncryptionFailedCallback
import com.kieronquinn.app.utag.xposed.extensions.TagActivity_createIntent
import com.kieronquinn.app.utag.xposed.extensions.applySecurity
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import dev.oneuiproject.oneui.R as OneuiR

interface HistoryWidgetRepository: RoomEncryptionFailedCallback {

    companion object {
        internal const val ACTION_REFRESH_HISTORY_WIDGETS =
            "${BuildConfig.APPLICATION_ID}.action.REFRESH_HISTORY_WIDGETS"

        fun refreshAll(context: Context) {
            context.sendBroadcast(Intent(ACTION_REFRESH_HISTORY_WIDGETS).apply {
                `package` = context.packageName
                applySecurity(context)
            })
        }
    }

    /**
     *  Returns if there are any widgets of this type
     */
    fun hasWidgets(): Flow<Boolean>

    /**
     *  Updates all widgets
     */
    fun updateWidgets()

    /**
     *  Updates a specific widget, setting it as loading first
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
        historyState: HistoryState,
        tagState: TagState.Loaded,
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

    data class AppWidgetConfig(
        val appWidgetId: Int,
        val packageName: String,
        val deviceId: String? = null,
        val mapStyle: MapStyle = MapStyle.NORMAL,
        val mapTheme: MapTheme = MapTheme.SYSTEM
    )

}

class HistoryWidgetRepositoryImpl(
    private val staticMapRepository: StaticMapRepository,
    private val locationHistoryRepository: LocationHistoryRepository,
    private val context: Context,
    smartTagRepository: SmartTagRepository,
    encryptionRepository: EncryptionRepository,
    database: UTagDatabase
): HistoryWidgetRepository {

    companion object {
        private const val MAX_ZOOM_LEVEL = 17f
        private const val FILENAME_PREFIX_LOCATION_WIDGET = "location_widget"
    }

    private val widgetConfigTable = database.locationWidgetConfigTable()
    private val refreshSizesRequest = MutableStateFlow<RefreshSizesRequest?>(null)
    private val refreshIdRequest = MutableStateFlow<RefreshIdRequest?>(null)
    private val refreshThemeRequest = MutableStateFlow(System.currentTimeMillis())
    private var cachedRemoteViews = HashMap<Int, (Boolean) -> RemoteViews?>()
    private val remoteViewsLock = Mutex()
    private val scope = MainScope()

    private val refreshAllRequest = context.broadcastReceiverAsFlow(
        IntentFilter(ACTION_REFRESH_HISTORY_WIDGETS)
    ).map {
        it.verifySecurity(context.packageName)
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val appWidgetManager =
        context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager

    private val loadingRemoteViews by lazy {
        RemoteViews(context.packageName, R.layout.widget_utag_loading)
    }

    private val lineColour = ContextCompat.getColor(context, R.color.map_line_fallback)
    private val lineWidth = 2.dp.toFloat()

    private val markerPoint by lazy {
        BitmapDescriptorFactory.fromBitmap(
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_marker_point),
                context.resources.dip(18),
                context.resources.dip(18),
                true
            ).copy(Bitmap.Config.ARGB_8888, true)
        )
    }

    private val selectedMarker by lazy {
        val pin = BitmapFactory.decodeResource(context.resources, R.drawable.ic_marker)
        BitmapDescriptorFactory.fromBitmap(
            pin.scaleAndRecycle(context.resources.dip(40), context.resources.dip(44))
        )
    }

    /**
     *  All current widget configs from the database. Used to lookup current states.
     */
    private val allWidgetConfigs = widgetConfigTable.getAllConfigs().map { configs ->
        configs.map { it.toAppWidgetConfig() }.associateBy { it.appWidgetId }
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

    private val deviceIds = allWidgetConfigs.map { configs ->
        configs?.values?.map { it.deviceId }?.toSet() ?: emptySet()
    }

    private val historyStates = deviceIds.map { ids ->
        ids.associateWith { id ->
            combine(
                id?.let { smartTagRepository.getTagState(id) } ?: flowOf(null),
                refreshAllRequest,
                refreshIdRequest.filter { it == null || it.id == id },
                encryptionRepository.pinStateChangeBus
            ) { tagState, _, _, _ ->
                if (id == null) return@combine null
                Pair(
                    tagState as? TagState.Loaded,
                    locationHistoryRepository.getLocationHistory(id, 1)
                )
            }
        }
    }

    private val widgetConfigsWithStates = widgetConfigs.filterNotNull().map { flows ->
        flows.map { flow ->
            val refreshRequest = refreshSizesRequest.filter {
                it == null || it.id == flow.key
            }
            combine(
                flow.value,
                historyStates,
                refreshRequest,
                refreshThemeRequest
            ) { config, historyStates, _, _ ->
                if (config == null) return@combine null
                val history = historyStates[config.deviceId] ?: return@combine null
                val sizes = appWidgetManager.getWidgetSizes(context, config.appWidgetId)
                SizedWidgetConfigWithStates(config, sizes, history)
            }
        }
    }

    /**
     *  Transformed [widgetConfigsWithStates] into output [RemoteViews], mapped to their app widget
     *  ID
     */
    private val remoteViews = widgetConfigsWithStates.mapLatest { flows ->
        flows.map { flow ->
            flow.flatMapLatest { config ->
                if (config == null) return@flatMapLatest flowOf(null)
                config.historyState.mapNotNull {
                    Pair(config, it?.toHistoryWidgetTagState() ?: return@mapNotNull null)
                }.debounce(500L).map {
                    val widgetConfig = it.first
                    val state = it.second
                    val width = widgetConfig.sizes?.first?.width ?: return@map null
                    val height = widgetConfig.sizes.first.height
                    val landscapeWidth = widgetConfig.sizes.second.width
                    val landscapeHeight = widgetConfig.sizes.second.height
                    loadRemoteViews(
                        widgetConfig,
                        state,
                        width,
                        height,
                        landscapeWidth,
                        landscapeHeight
                    )
                }
            }
        }
    }

    private fun loadRemoteViews(
        config: SizedWidgetConfigWithStates,
        historyState: HistoryWidgetState,
        width: Int,
        height: Int,
        widthLandscape: Int? = null,
        heightLandscape: Int? = null
    ) = scope.launch {
        remoteViewsLock.withLock {
            val remoteViewsBuilder = getRemoteViews(
                config.config,
                historyState,
                width,
                height,
                widthLandscape,
                heightLandscape
            )
            val remoteViews = remoteViewsBuilder.invoke(false)
            cachedRemoteViews[config.config.appWidgetId] = remoteViewsBuilder
            appWidgetManager.updateAppWidget(config.config.appWidgetId, remoteViews)
        }
    }

    override fun hasWidgets(): Flow<Boolean> {
        return allWidgetConfigs.filterNotNull().map { it.isNotEmpty() }
    }

    override fun updateWidgets() {
        HistoryWidgetRepository.refreshAll(context)
    }

    override fun updateWidget(appWidgetId: Int) {
        val current = cachedRemoteViews[appWidgetId]
        val deviceId = allWidgetConfigs.value?.get(appWidgetId)?.deviceId ?: run {
            //Didn't find a config, refresh all instead
            updateWidgets()
            return
        }
        if (current != null) {
            appWidgetManager.updateAppWidget(appWidgetId, current(true))
        } else {
            appWidgetManager.updateAppWidget(appWidgetId, loadingRemoteViews)
        }
        scope.launch {
            refreshIdRequest.emit(RefreshIdRequest(deviceId))
        }
    }

    override fun onWidgetSizeChanged(appWidgetId: Int) {
        scope.launch {
            appWidgetManager.updateAppWidget(appWidgetId, loadingRemoteViews)
            refreshSizesRequest.emit(RefreshSizesRequest(appWidgetId))
        }
    }

    override fun onWidgetThemeChanged() {
        scope.launch {
            allWidgetConfigs.value?.map { it.value.appWidgetId }?.toIntArray()?.let {
                appWidgetManager.updateAppWidget(it, loadingRemoteViews)
            }
            refreshThemeRequest.emit(System.currentTimeMillis())
        }
    }

    override suspend fun getRemoteViews(
        historyState: HistoryState,
        tagState: TagState.Loaded,
        config: AppWidgetConfig,
        width: Int,
        height: Int
    ): RemoteViews? {
        val widgetHistoryState = Pair(tagState, historyState).toHistoryWidgetTagState()
            ?: return null
        return getRemoteViews(config, widgetHistoryState, width, height, forPreview = true)
            .invoke(false)
    }

    override fun clearPreviewImages(appWidgetId: Int) {
        clearImages(appWidgetId, true)
    }

    override suspend fun getWidget(appWidgetId: Int): AppWidgetConfig? {
        return allWidgetConfigs.firstNotNull()[appWidgetId]
    }

    override suspend fun updateWidget(appWidgetConfig: AppWidgetConfig) {
        withContext(Dispatchers.IO) {
            widgetConfigTable.insert(
                HistoryWidgetConfig(
                    appWidgetConfig.appWidgetId,
                    appWidgetConfig.packageName,
                    appWidgetConfig.deviceId?.toEncryptedValue(),
                    appWidgetConfig.mapStyle,
                    appWidgetConfig.mapTheme
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
            allWidgetConfigs.firstNotNull().map { it.value.packageName }.toSet()
        }
    }

    private fun clearImages(appWidgetId: Int, preview: Boolean) {
        val suffix = if (preview) "P" else ""
        staticMapRepository.clearFile("${FILENAME_PREFIX_LOCATION_WIDGET}_${appWidgetId}_$suffix")
        staticMapRepository.clearFile("${FILENAME_PREFIX_LOCATION_WIDGET}_${appWidgetId}_L$suffix")
    }

    private fun Pair<TagState.Loaded?, HistoryState>.toHistoryWidgetTagState(): HistoryWidgetState? {
        val tagState = first
        val state = second
        val today = LocalDate.now()
        return when {
            state is HistoryState.Loaded && tagState != null -> {
                HistoryWidgetState.Loaded(
                    state.deviceId,
                    state.timestamp,
                    tagState,
                    state.items.filter { it.isOnDay(today) },
                    state.decryptFailed
                )
            }

            state is HistoryState.Error -> {
                HistoryWidgetState.Error(state.deviceId, state.timestamp, ErrorType.LOCATION)
            }

            tagState == null -> {
                HistoryWidgetState.Error(state.deviceId, state.timestamp, ErrorType.LOCATION)
            }

            else -> null
        }
    }

    private suspend fun getRemoteViews(
        config: AppWidgetConfig,
        historyState: HistoryWidgetState,
        width: Int,
        height: Int,
        widthLandscape: Int? = null,
        heightLandscape: Int? = null,
        forPreview: Boolean = false
    ): (isLoading: Boolean) -> RemoteViews? {
        val timestamp = historyState.timestamp
        val deviceId = config.deviceId ?: return { null }
        val hash = config.hashCode()
        val hasEncryptionError =
            (historyState as? HistoryWidgetState.Loaded)?.decryptFailed ?: false
        val clickPendingIntent = getClickPendingIntent(config.appWidgetId, deviceId)
        val lastMarker = (historyState as? HistoryWidgetState.Loaded)
            ?.tagState?.device?.markerIcons?.first
        if (historyState !is HistoryWidgetState.Loaded) {
            return {
                val type =
                    (historyState as? HistoryWidgetState.Error)?.type ?: ErrorType.LOCATION
                getErrorRemoteViews(
                    type,
                    config.appWidgetId,
                    it,
                    clickPendingIntent,
                    forPreview
                )
            }
        }
        if (historyState.items.isEmpty()) {
            return {
                getErrorRemoteViews(
                    ErrorType.NO_LOCATION,
                    config.appWidgetId,
                    it,
                    clickPendingIntent,
                    forPreview
                )
            }
        }
        val locations = historyState.items.sortedBy { it.timestamp() }
        val image = locations.getMapImage(
            width,
            height,
            config.mapStyle,
            config.mapTheme,
            config.appWidgetId,
            false,
            hash,
            timestamp,
            lastMarker,
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
        val landscapeImage = if (widthLandscape != null && heightLandscape != null) {
            locations.getMapImage(
                widthLandscape,
                heightLandscape,
                config.mapStyle,
                config.mapTheme,
                config.appWidgetId,
                true,
                hash,
                timestamp,
                lastMarker,
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
        } else null
        //Use the last location time or the update time if that's not available
        val statusTimestamp = locations.lastOrNull()?.timestamp() ?: timestamp
        val formattedTimestamp = context.formatTime(
            statusTimestamp, true, R.string.widget_timestamp
        )
        val contentDescription = locations.lastOrNull()?.address ?: formattedTimestamp
        val tagName = historyState.tagState.device.label
        val refreshPendingIntent = getRefreshPendingIntent(config.appWidgetId)
        val modifiers: RemoteViews.(isLoading: Boolean) -> Unit = { isLoading ->
            setTextViewText(R.id.widget_utag_map_status, formattedTimestamp)
            setOnClickPendingIntent(R.id.widget_utag_map_refresh, refreshPendingIntent)
            setOnClickPendingIntent(android.R.id.background, clickPendingIntent)
            setContentDescription(R.id.widget_utag_map_image, contentDescription)
            setContentDescription(R.id.widget_utag_map_logo, tagName)
            if (hasEncryptionError) {
                setOnClickPendingIntent(
                    R.id.widget_utag_map_error,
                    getErrorClickPendingIntent(config.appWidgetId, ErrorType.ENCRYPTION)
                )
                setViewVisibility(R.id.widget_utag_map_error, View.VISIBLE)
            } else {
                setViewVisibility(R.id.widget_utag_map_error, View.GONE)
            }
            if (isLoading) {
                setViewVisibility(R.id.widget_utag_map_refresh, View.GONE)
                setViewVisibility(R.id.widget_utag_map_progress, View.VISIBLE)
            } else {
                setViewVisibility(R.id.widget_utag_map_refresh, View.VISIBLE)
                setViewVisibility(R.id.widget_utag_map_progress, View.GONE)
            }
            if (forPreview) {
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

    private suspend fun List<LocationHistoryPoint>.getMapImage(
        width: Int,
        height: Int,
        style: MapStyle,
        theme: MapTheme,
        appWidgetId: Int,
        landscape: Boolean,
        hash: Int,
        timestamp: Long,
        lastMarker: Bitmap?,
        preview: Boolean
    ): Uri? {
        if(isEmpty()) return null
        val suffix = StringBuilder().apply {
            if(landscape) append("L")
            if(preview) append("P")
        }.toString()
        //If there's more than one location or we're in land, add additional padding by zooming out
        val zoomOut = landscape || size > 1
        return staticMapRepository.generateStaticMapUri(
            "${FILENAME_PREFIX_LOCATION_WIDGET}_${appWidgetId}_$suffix",
            width,
            height,
            MAX_ZOOM_LEVEL,
            zoomOut,
            style,
            theme,
            true
        ) {
            mapIndexed { index, point ->
                val nextPoint = getOrNull(index + 1)
                if(nextPoint != null) {
                    addPolyline(
                        PolylineOptions().add(point.location, nextPoint.location)
                            .color(lineColour)
                            .startCap(RoundCap())
                            .endCap(RoundCap())
                            .width(lineWidth)
                    )
                }
                val marker = when {
                    nextPoint == null && lastMarker != null -> {
                        BitmapDescriptorFactory.fromBitmap(lastMarker.scale(0.5f))
                    }
                    nextPoint == null -> selectedMarker
                    else -> markerPoint
                }
                val anchorY = if(nextPoint == null) 1f else 0.5f
                MarkerOptions()
                    .icon(marker)
                    .anchor(0.5f, anchorY)
                    .zIndex(index.toFloat())
                    .position(point.location)
            }
        }?.buildUpon()
            ?.appendQueryParameter("hash", hash.toString())
            ?.appendQueryParameter("timestamp", timestamp.toString())
            ?.build()
    }

    private fun getErrorRemoteViews(
        errorType: ErrorType,
        appWidgetId: Int,
        isLoading: Boolean,
        clickPendingIntent: PendingIntent,
        forPreview: Boolean
    ): RemoteViews {
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

    private sealed class HistoryWidgetState(
        open val deviceId: String,
        open val timestamp: Long
    ) {
        data class Loaded(
            override val deviceId: String,
            override val timestamp: Long,
            val tagState: TagState.Loaded,
            val items: List<LocationHistoryPoint>,
            val decryptFailed: Boolean
        ): HistoryWidgetState(deviceId, timestamp)

        data class Error(
            override val deviceId: String,
            override val timestamp: Long,
            val type: ErrorType
        ): HistoryWidgetState(deviceId, timestamp) {
            enum class ErrorType(@StringRes val message: Int, @StringRes val toast: Int) {
                LOCATION(R.string.widget_error_locations, R.string.widget_error_toast_location),
                ENCRYPTION(R.string.widget_error_encryption, R.string.widget_error_toast_encryption),
                NO_LOCATION(R.string.widget_error_no_locations, R.string.widget_error_toast_location)
            }
        }
    }

    private fun HistoryWidgetConfig.toAppWidgetConfig(): AppWidgetConfig {
        return AppWidgetConfig(
            appWidgetId = appWidgetId,
            packageName = packageName,
            deviceId = deviceId?.let { String(it.bytes) },
            mapStyle = mapStyle,
            mapTheme = mapTheme
        )
    }

    private data class SizedWidgetConfigWithStates(
        val config: AppWidgetConfig,
        val sizes: Pair<Size, Size>?,
        val historyState: Flow<Pair<TagState.Loaded?, HistoryState>?>
    )

    private data class RefreshSizesRequest(val id: Int, val key: Long = System.currentTimeMillis())
    private data class RefreshIdRequest(val id: String, val key: Long = System.currentTimeMillis())

}