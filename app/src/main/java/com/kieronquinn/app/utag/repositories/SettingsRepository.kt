package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.icu.util.LocaleData
import androidx.annotation.StringRes
import com.google.android.gms.maps.GoogleMap
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.providers.SettingsProvider
import com.kieronquinn.app.utag.repositories.BaseSettingsRepository.UTagSetting
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.repositories.SettingsRepository.Units
import com.kieronquinn.app.utag.utils.extensions.getRandomString
import com.kieronquinn.app.utag.utils.preferences.SharedPreferencesResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

interface SettingsRepository: BaseSettingsRepository {

    /**
     *  Whether the user has agreed to Samsung's privacy policies & terms. On uTag, this is done
     *  during setup before the user signs in, and if required is sent to the server after sign in.
     *
     *  If the user signs out, they will have to agree again, but if they go back and then forward
     *  again during setup, this prevents it being shown multiple times.
     *
     *  Since this is only the local state, it's kept unencrypted.
     */
    @IgnoreInBackup
    val hasAgreedToPolicies: UTagSetting<Boolean>

    /**
     *  Whether the user has seen the intro/guide screen for location history
     */
    @IgnoreInBackup
    val hasSeenLocationHistoryIntro: UTagSetting<Boolean>

    /**
     *  Whether to check for updates automatically
     */
    @IgnoreInBackup
    val autoUpdatesEnabled: UTagSetting<Boolean>

    /**
     *  Whether to subscribe to location updates and show a location marker on the map
     */
    @IgnoreInBackup
    val isMyLocationEnabled: UTagSetting<Boolean>

    /**
     *  The style to apply to the find map
     */
    val mapStyle: UTagSetting<MapStyle>

    /**
     *  The theme to apply to the find map
     */
    val mapTheme: UTagSetting<MapTheme>

    /**
     *  Whether to show buildings on maps
     */
    val mapShowBuildings: UTagSetting<Boolean>

    /**
     *  If enabled, swaps Location History for Search Nearby on map screen
     */
    val mapSwapLocationHistory: UTagSetting<Boolean>

    /**
     *  Use UWB for precise finding if available
     */
    val useUwb: UTagSetting<Boolean>

    /**
     *  Allows UWB over a longer distance, which may be unreliable if the Tag is not in line of
     *  sight.
     */
    val allowLongDistanceUwb: UTagSetting<Boolean>

    /**
     *  The units to use in nearby search for distance. [Units.SYSTEM] uses
     *  [LocaleData.getMeasurementSystem] to get the default for the current Locale
     */
    val units: UTagSetting<Units>

    /**
     *  Whether to automatically dismiss left behind notifications when the Tag comes back in range
     */
    val autoDismissNotifications: UTagSetting<Boolean>

    /**
     *  Whether the user has seen the Find my Device shared tag warning, which is only displayed
     *  once.
     */
    @IgnoreInBackup
    val hasSeenFindMyDeviceWarning: UTagSetting<Boolean>

    /**
     *  Whether the user has seen the Automation shared tag warning, which is only displayed once.
     */
    @IgnoreInBackup
    val hasSeenAutomationWarning: UTagSetting<Boolean>

    /**
     *  Set when we know the user has granted restricted settings, ignored in backups, so should
     *  provide a relatively solid way of knowing if it's been granted previously.
     */
    @IgnoreInBackup
    val hasGrantedRestrictedSettings: UTagSetting<Boolean>

    /**
     *  Whether Firebase Analytics & Crashlytics should be enabled
     */
    @IgnoreInBackup
    val analyticsEnabled: UTagSetting<Boolean>

    /**
     *  Whether to show fake locations and disable some destructive features (eg. deleting history)
     */
    @IgnoreInBackup
    val contentCreatorModeEnabled: UTagSetting<Boolean>

    /**
     *  Language override for legacy devices, ignored on Android 13+, default is empty (system)
     */
    @IgnoreInBackup
    val locale: UTagSetting<String>

    /**
     *  Gets randomised, fake Android ID
     */
    fun getAndroidId(): String

    enum class MapStyle(val style: Int, @StringRes val label: Int) {
        NORMAL(GoogleMap.MAP_TYPE_NORMAL, R.string.settings_map_style_normal),
        TERRAIN(GoogleMap.MAP_TYPE_TERRAIN, R.string.settings_map_style_terrain),
        SATELLITE(GoogleMap.MAP_TYPE_HYBRID, R.string.settings_map_style_satellite)
    }

    enum class MapTheme(@StringRes val label: Int) {
        SYSTEM(R.string.settings_map_theme_system),
        LIGHT(R.string.settings_map_theme_light),
        DARK(R.string.settings_map_theme_dark)
    }

    enum class Units(@StringRes val label: Int) {
        SYSTEM(R.string.settings_location_units_system),
        METRIC(R.string.settings_location_units_metric),
        IMPERIAL(R.string.settings_location_units_imperial)
    }

}

class SettingsRepositoryImpl(context: Context): BaseSettingsRepositoryImpl(), SettingsRepository {

    companion object {
        private const val KEY_HAS_SEEN_LOCATION_HISTORY_INTRO = "has_seen_location_history_intro"
        private const val DEFAULT_HAS_SEEN_LOCATION_HISTORY_INTRO = false

        private const val KEY_HAS_AGREED_TO_POLICIES = "has_agreed_to_policies"
        private const val DEFAULT_HAS_AGREED_TO_POLICIES = false

        private const val KEY_AUTO_UPDATES_ENABLED = "auto_updates_enabled"
        private const val DEFAULT_AUTO_UPDATES_ENABLED = true

        private const val KEY_IS_MY_LOCATION_ENABLED = "is_my_location_enabled"
        private const val DEFAULT_IS_MY_LOCATION_ENABLED = true //Default enabled but needs permission

        private const val KEY_MAP_STYLE = "map_style"
        private val DEFAULT_MAP_STYLE = MapStyle.NORMAL

        private const val KEY_MAP_THEME = "map_theme"
        private val DEFAULT_MAP_THEME = MapTheme.SYSTEM

        private const val KEY_MAP_SHOW_BUILDINGS = "map_show_buildings"
        private const val DEFAULT_MAP_SHOW_BUILDINGS = true

        private const val KEY_MAP_SWAP_LOCATION_HISTORY = "map_swap_location_history"
        private const val DEFAULT_MAP_SWAP_LOCATION_HISTORY = false

        private const val KEY_USE_UWB = "use_uwb"
        private const val DEFAULT_USE_UWB = true

        private const val KEY_ALLOW_LONG_DISTANCE_UWB = "long_distance_uwb"
        private const val DEFAULT_ALLOW_LONG_DISTANCE_UWB = false

        private const val KEY_UNITS = "units"
        private val DEFAULT_UNITS = Units.SYSTEM

        private const val KEY_HAS_SEEN_FIND_MY_DEVICE_WARNING = "has_seen_find_my_device_warning"
        private const val DEFAULT_HAS_SEEN_FIND_MY_DEVICE_WARNING = false

        private const val KEY_HAS_SEEN_AUTOMATION_WARNING = "has_seen_automation_warning"
        private const val DEFAULT_HAS_SEEN_AUTOMATION_WARNING = false

        private const val KEY_HAS_GRANTED_RESTRICTED_SETTINGS = "has_granted_restricted_settings"
        private const val DEFAULT_HAS_GRANTED_RESTRICTED_SETTINGS = false

        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        private const val DEFAULT_ANALYTICS_ENABLED = false

        private const val KEY_CONTENT_CREATOR_MODE_ENABLED = "content_creator_mode_enabled"
        private const val DEFAULT_CONTENT_CREATOR_MODE_ENABLED = false

        private const val KEY_AUTO_DISMISS_NOTIFICATIONS = "auto_dismiss_notifications"
        private const val DEFAULT_AUTO_DISMISS_NOTIFICATIONS = false

        private const val KEY_LOCALE = "locale"
        private const val DEFAULT_LOCALE = ""

        private const val KEY_ANDROID_ID = "android_id"
        private const val DEFAULT_ANDROID_ID = ""
    }

    override val sharedPreferences by lazy {
        SharedPreferencesResolver(context, SettingsProvider::class.java)
    }

    override val hasSeenLocationHistoryIntro =
        boolean(KEY_HAS_SEEN_LOCATION_HISTORY_INTRO, DEFAULT_HAS_SEEN_LOCATION_HISTORY_INTRO)

    override val hasAgreedToPolicies =
        boolean(KEY_HAS_AGREED_TO_POLICIES, DEFAULT_HAS_AGREED_TO_POLICIES)

    override val autoUpdatesEnabled =
        boolean(KEY_AUTO_UPDATES_ENABLED, DEFAULT_AUTO_UPDATES_ENABLED)

    override val isMyLocationEnabled =
        boolean(KEY_IS_MY_LOCATION_ENABLED, DEFAULT_IS_MY_LOCATION_ENABLED)

    override val mapStyle = enum(KEY_MAP_STYLE, DEFAULT_MAP_STYLE)

    override val mapTheme = enum(KEY_MAP_THEME, DEFAULT_MAP_THEME)

    override val mapShowBuildings = boolean(KEY_MAP_SHOW_BUILDINGS, DEFAULT_MAP_SHOW_BUILDINGS)

    override val mapSwapLocationHistory =
        boolean(KEY_MAP_SWAP_LOCATION_HISTORY, DEFAULT_MAP_SWAP_LOCATION_HISTORY)

    override val useUwb = boolean(KEY_USE_UWB, DEFAULT_USE_UWB)

    override val allowLongDistanceUwb =
        boolean(KEY_ALLOW_LONG_DISTANCE_UWB, DEFAULT_ALLOW_LONG_DISTANCE_UWB)

    override val units = enum(KEY_UNITS, DEFAULT_UNITS)

    override val autoDismissNotifications = boolean(
        KEY_AUTO_DISMISS_NOTIFICATIONS,
        DEFAULT_AUTO_DISMISS_NOTIFICATIONS
    )

    override val hasSeenFindMyDeviceWarning = boolean(
        KEY_HAS_SEEN_FIND_MY_DEVICE_WARNING,
        DEFAULT_HAS_SEEN_FIND_MY_DEVICE_WARNING
    )

    override val hasSeenAutomationWarning = boolean(
        KEY_HAS_SEEN_AUTOMATION_WARNING,
        DEFAULT_HAS_SEEN_AUTOMATION_WARNING
    )

    override val hasGrantedRestrictedSettings = boolean(
        KEY_HAS_GRANTED_RESTRICTED_SETTINGS,
        DEFAULT_HAS_GRANTED_RESTRICTED_SETTINGS
    )

    override val analyticsEnabled = boolean(
        KEY_ANALYTICS_ENABLED,
        DEFAULT_ANALYTICS_ENABLED
    )

    override val contentCreatorModeEnabled = boolean(
        KEY_CONTENT_CREATOR_MODE_ENABLED,
        DEFAULT_CONTENT_CREATOR_MODE_ENABLED
    )

    override val locale = string(KEY_LOCALE, DEFAULT_LOCALE)

    private val androidId = string(KEY_ANDROID_ID, DEFAULT_ANDROID_ID)

    @Synchronized
    override fun getAndroidId(): String {
        return loadAndroidId() ?: saveAndroidId()
    }

    private fun loadAndroidId(): String? {
        val androidId = this.androidId.getSync()
        return androidId.takeIf { it.isNotBlank() }
    }

    private fun saveAndroidId(): String {
        val androidId = getRandomString(16)
        this.androidId.setSync(androidId)
        return androidId
    }

    /**
     *  Use reflection to load all settings fields not marked with [IgnoreInBackup], which can be
     *  backed up and restored from
     */
    private fun getBackupFields(): List<UTagSetting<*>> {
        return SettingsRepository::class.memberProperties.filter {
            it.findAnnotations<IgnoreInBackup>().isEmpty()
        }.mapNotNull {
            it.get(this) as? UTagSetting<*>
        }
    }

    override suspend fun getBackup(): Map<String, String> {
        return getBackupFields().mapNotNull {
            Pair(it.key(), it.serialize() ?: return@mapNotNull null)
        }.toMap()
    }

    override suspend fun restoreBackup(settings: Map<String, String>) = withContext(Dispatchers.IO) {
        val properties = getBackupFields().associateBy { it.key() }
        settings.forEach {
            properties[it.key]?.deserialize(it.value)
        }
    }

}