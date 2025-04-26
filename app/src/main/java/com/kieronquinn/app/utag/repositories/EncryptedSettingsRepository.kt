package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.util.Base64
import androidx.annotation.StringRes
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.LocationStaleness
import com.kieronquinn.app.utag.providers.EncryptedSettingsProvider
import com.kieronquinn.app.utag.repositories.BaseSettingsRepository.UTagSetting
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.PinTimeout
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.RefreshPeriod
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.UtsSensitivity
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository.WidgetRefreshPeriod
import com.kieronquinn.app.utag.utils.preferences.SharedPreferencesResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.time.ZonedDateTime
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

interface EncryptedSettingsRepository: BaseSettingsRepository {

    /**
     *  The base URL to use for authorisation requests.
     */
    @IgnoreInBackup
    val authServerUrl: UTagSetting<String>

    /**
     *  Main login token, does not expire and cannot be refreshed. Equivalent to the AAS token in
     *  Google services.
     */
    @IgnoreInBackup
    val userAuthToken: UTagSetting<String>

    /**
     *  Email used to login, returned from initial auth request
     */
    @IgnoreInBackup
    val loginId: UTagSetting<String>

    /**
     *  Token with `offline.access` scope, used for requests to the Samsung Find endpoints. 24 hour
     *  expiry time, after which [offlineAccessRefreshToken] should be used to generate a new token.
     */
    @IgnoreInBackup
    val offlineAccessToken: UTagSetting<String>

    /**
     *  Refresh token for [offlineAccessToken], 90 day expiry time. After that, [userAuthToken]
     *  should be used to generate a new token.
     */
    @IgnoreInBackup
    val offlineAccessRefreshToken: UTagSetting<String>

    /**
     *  Token with `iot.client` scope, used for requests to the SmartThings endpoints. 24 hour
     *  expiry time, after which [iotRefreshToken] should be used to generate a new token.
     */
    @IgnoreInBackup
    val iotToken: UTagSetting<String>

    /**
     *  Refresh token for [iotToken], 90 day expiry time. After that, [userAuthToken] should be
     *  used to generate a new token.
     */
    @IgnoreInBackup
    val iotRefreshToken: UTagSetting<String>

    /**
     *  The user's ID, which is provided as part of the login process but not in subsequent
     *  refreshes.
     */
    @IgnoreInBackup
    val userId: UTagSetting<String>

    /**
     *  The user's country, which comes from [UserRepository.getUserInfo], but is cached to be used
     *  in request headers. If this is not provided, we will use either `USA` or `GBR`, based on
     *  [authServerUrl]
     */
    @IgnoreInBackup
    val authCountryCode: UTagSetting<String>

    /**
     *  The user's saved PIN, if they chose to save it
     */
    @IgnoreInBackup
    val savedPin: UTagSetting<String>

    /**
     *  How long to keep the user's entered PIN in memory before it is invalidated
     */
    @IgnoreInBackup
    val pinTimeout: UTagSetting<PinTimeout>

    /**
     *  Whether the biometric prompt is enabled
     */
    @IgnoreInBackup
    val biometricPromptEnabled: UTagSetting<Boolean>

    /**
     *  Whether to require biometrics rather than allowing device credential to change the PIN
     */
    @IgnoreInBackup
    val biometricsRequiredToChangePin: UTagSetting<Boolean>

    /**
     *  The last device ID the user had selected in the map, which will be shown again if they
     *  launch the find shortcut without a selected ID
     */
    @IgnoreInBackup
    val lastSelectedDeviceIdOnMap: UTagSetting<String>

    /**
     *  How often to refresh the location of connected tags
     */
    val locationRefreshPeriod: UTagSetting<RefreshPeriod>

    /**
     *  Whether to refresh locations when battery saver is enabled
     */
    val locationOnBatterySaver: UTagSetting<Boolean>

    /**
     *  The acceptable staleness of a location to use in location updates. Defaults to 30 seconds.
     */
    val locationStaleness: UTagSetting<LocationStaleness>

    /**
     *  How often to refresh widgets in the background
     */
    val widgetRefreshPeriod: UTagSetting<WidgetRefreshPeriod>

    /**
     *  Whether to refresh widgets when battery saver is enabled
     */
    val widgetRefreshOnBatterySaver: UTagSetting<Boolean>

    /**
     *  Whether to show & be able to enable [debugModeEnabled]
     */
    @IgnoreInBackup
    val debugModeVisible: UTagSetting<Boolean>

    /**
     *  Enables some verbose logging & debug UI features
     */
    @IgnoreInBackup
    val debugModeEnabled: UTagSetting<Boolean>

    /**
     *  Enables passive scanning and parsing of unknown Tags, and notifying if they're following.
     */
    val utsScanEnabled: UTagSetting<Boolean>

    /**
     *  How sensitive unknown tag scan warnings should be
     */
    val utsSensitivity: UTagSetting<UtsSensitivity>

    /**
     *  Enables contributions to the FME network
     */
    val networkContributionsEnabled: UTagSetting<Boolean>

    /**
     *  The number of times uTag has sent a location for a non-owner Tag to Chaser
     */
    val chaserCount: UTagSetting<Int>

    /**
     *  Whether overmature offline prevention is enabled for all tags
     */
    val overmatureOfflinePreventionEnabled: UTagSetting<Boolean>

    /**
     *  The encryption key used to encrypt sensitive data stored in the Room database.
     */
    fun getDatabaseEncryptionKey(): SecretKey

    /**
     *  The encryption IV used to encrypt sensitive data stored in the Room database.
     */
    fun getDatabaseEncryptionIV(): IvParameterSpec

    /**
     *  Prepare the Key and IV, before they're used
     */
    fun ensureKeyAndIV()

    /**
     *  Combination of [debugModeVisible] and [debugModeEnabled], `true` when both are set
     */
    fun isDebugModeEnabled(scope: CoroutineScope): StateFlow<Boolean?>

    enum class RefreshPeriod(@StringRes val label: Int, private val step: Int? = null) {
        FIVE_MINUTES(R.string.refresh_period_five_minutes, 5),
        TEN_MINUTES(R.string.refresh_period_ten_minutes, 10),
        FIFTEEN_MINUTES(R.string.refresh_period_fifteen_minutes, 15),
        THIRTY_MINUTES(R.string.refresh_period_thirty_minutes, 30),
        SIXTY_MINUTES(R.string.refresh_period_sixty_minutes, 60),
        NEVER(R.string.refresh_period_never);

        /**
         *  Gets the next [ZonedDateTime] for this period, or `null` if the period is set to
         *  [NEVER]
         */
        fun getNext(): ZonedDateTime? {
            if(step == null) return null
            val now = ZonedDateTime.now()
            val currentMinute = now.minute
            val minutes = (0..59 step step)
            val nextMinute = minutes.firstOrNull { it > currentMinute }
            return if(nextMinute != null) {
                now.withMinute(nextMinute)
            }else{
                now.withMinute(0).plusHours(1)
            }.withSecond(0).withNano(0)
        }
    }

    enum class WidgetRefreshPeriod(@StringRes val label: Int, private val step: Int? = null) {
        ONE_HOUR(R.string.refresh_period_sixty_minutes, 1),
        TWO_HOURS(R.string.refresh_period_two_hours, 2),
        THREE_HOURS(R.string.refresh_period_three_hours, 3),
        FOUR_HOURS(R.string.refresh_period_four_hours, 4),
        SIX_HOURS(R.string.refresh_period_six_hours, 6),
        TWELVE_HOURS(R.string.refresh_period_twelve_hours, 12),
        NEVER(R.string.refresh_period_never);

        /**
         *  Gets the next [ZonedDateTime] for this period, or `null` if the period is set to
         *  [NEVER]
         */
        fun getNext(): ZonedDateTime? {
            if(step == null) return null
            val now = ZonedDateTime.now()
            val currentHour = now.hour
            val hours = (0..23 step step)
            val nextHour = hours.firstOrNull { it > currentHour }
            return if(nextHour != null) {
                now.withHour(nextHour)
            }else{
                now.withHour(0).plusDays(1)
            }.withMinute(0).withSecond(0).withNano(0)
        }
    }

    enum class PinTimeout(val minutes: Int, @StringRes val label: Int) {
        NEVER(Int.MAX_VALUE, R.string.settings_encryption_pin_timeout_never),
        ONE_MINUTE(1, R.string.settings_encryption_pin_timeout_one_minute),
        TWO_MINUTES(2, R.string.settings_encryption_pin_timeout_two_minutes),
        FIVE_MINUTES(5, R.string.settings_encryption_pin_timeout_five_minutes),
        TEN_MINUTES(10, R.string.settings_encryption_pin_timeout_ten_minutes),
        FIFTEEN_MINUTES(15, R.string.settings_encryption_pin_timeout_fifteen_minutes),
        THIRTY_MINUTES(30, R.string.settings_encryption_pin_timeout_thirty_minutes),
        SIXTY_MINUTES(60, R.string.settings_encryption_pin_timeout_sixty_minutes),
    }

    enum class UtsSensitivity(val duration: Long, val distance: Double, @StringRes val label: Int) {
        VERY_HIGH(15, 250.0, R.string.settings_uts_sensitivity_very_high),
        HIGH(30, 500.0, R.string.settings_uts_sensitivity_high),
        NORMAL(60, 1000.0, R.string.settings_uts_sensitivity_normal),
        LOW(120, 2500.0, R.string.settings_uts_sensitivity_low),
        VERY_LOW(240, 5000.0, R.string.settings_uts_sensitivity_very_low),
    }

}

class EncryptedSettingsRepositoryImpl(
    context: Context
): BaseSettingsRepositoryImpl(), EncryptedSettingsRepository {

    companion object {
        private const val KEY_AUTH_SERVER_URL = "auth_server_url"
        private const val KEY_USER_AUTH_TOKEN = "user_auth_token"
        private const val KEY_IOT_TOKEN = "iot_token"
        private const val KEY_IOT_REFRESH_TOKEN = "iot_refresh_token"
        private const val KEY_LOGIN_ID = "login_id"
        private const val KEY_OFFLINE_ACCESS_TOKEN = "offline_access_token"
        private const val KEY_OFFLINE_ACCESS_REFRESH_TOKEN = "offline_access_refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_COUNTRY_CODE = "auth_country_code"
        private const val KEY_SAVED_PIN = "saved_pin"
        private const val KEY_BIOMETRIC_PROMPT_ENABLED = "biometric_prompt_enabled"
        private const val DEFAULT_BIOMETRIC_PROMPT_ENABLED = false
        private const val KEY_BIOMETRICS_REQUIRED_TO_CHANGE_PIN = "biometrics_required_to_change_pin"
        private const val DEFAULT_BIOMETRICS_REQUIRED_TO_CHANGE_PIN = false

        private const val KEY_PIN_TIMEOUT = "pin_timeout"
        private val DEFAULT_PIN_TIMEOUT = PinTimeout.NEVER

        private const val KEY_LAST_SELECTED_DEVICE_ID_ON_MAP = "last_selected_device_id_on_map"

        private const val KEY_ENCRYPTION_KEY = "encryption_key"
        private const val KEY_ENCRYPTION_IV = "encryption_iv"

        private const val KEY_LOCATION_REFRESH_PERIOD = "location_refresh_period"
        private val DEFAULT_LOCATION_REFRESH_PERIOD = RefreshPeriod.THIRTY_MINUTES

        private const val KEY_LOCATION_ON_BATTERY_SAVER = "location_on_battery_saver"
        private const val DEFAULT_LOCATION_ON_BATTERY_SAVER = false

        private const val KEY_LOCATION_STALENESS = "location_staleness"
        internal val DEFAULT_LOCATION_STALENESS = LocationStaleness.ONE_MINUTE

        private const val KEY_WIDGET_REFRESH_PERIOD = "widget_refresh_period"
        private val DEFAULT_WIDGET_REFRESH_PERIOD = WidgetRefreshPeriod.THREE_HOURS

        private const val KEY_WIDGET_REFRESH_ON_BATTERY_SAVER = "widget_on_battery_saver"
        private const val DEFAULT_WIDGET_REFRESH_ON_BATTERY_SAVER = false

        private const val KEY_DEBUG_MODE_VISIBLE = "debug_mode_visible"
        private const val DEFAULT_DEBUG_MODE_VISIBLE = false

        private const val KEY_DEBUG_MODE_ENABLED = "debug_mode_enabled"
        private const val DEFAULT_DEBUG_MODE_ENABLED = false

        private const val KEY_UTS_SCAN_ENABLED = "uts_scan_enabled"
        private const val DEFAULT_UTS_SCAN_ENABLED = false

        private const val KEY_UTS_SENSITIVITY = "uts_sensitivity"
        private val DEFAULT_UTS_SENSITIVITY = UtsSensitivity.NORMAL

        private const val KEY_NETWORK_CONTRIBUTIONS_ENABLED = "network_contributions_enabled"
        private const val DEFAULT_NETWORK_CONTRIBUTIONS_ENABLED = false

        private const val KEY_CHASER_COUNT = "chaser_count"
        private const val DEFAULT_CHASER_COUNT = 0

        private const val KEY_OVERMATURE_OFFLINE_PREVENTION = "overmature_offline_prevention"
        private const val DEFAULT_OVERMATURE_OFFLINE_PREVENTION = true
    }

    override val authServerUrl = string(KEY_AUTH_SERVER_URL, "")
    override val userAuthToken = string(KEY_USER_AUTH_TOKEN, "")
    override val iotToken = string(KEY_IOT_TOKEN, "")
    override val iotRefreshToken = string(KEY_IOT_REFRESH_TOKEN, "")
    override val loginId = string(KEY_LOGIN_ID, "")
    override val offlineAccessToken = string(KEY_OFFLINE_ACCESS_TOKEN, "")
    override val offlineAccessRefreshToken = string(KEY_OFFLINE_ACCESS_REFRESH_TOKEN, "")
    override val userId = string(KEY_USER_ID, "")
    override val authCountryCode = string(KEY_AUTH_COUNTRY_CODE, "")
    override val savedPin = string(KEY_SAVED_PIN, "")
    override val pinTimeout = enum(KEY_PIN_TIMEOUT, DEFAULT_PIN_TIMEOUT)

    override val biometricPromptEnabled =
        boolean(KEY_BIOMETRIC_PROMPT_ENABLED, DEFAULT_BIOMETRIC_PROMPT_ENABLED)

    override val biometricsRequiredToChangePin =
        boolean(KEY_BIOMETRICS_REQUIRED_TO_CHANGE_PIN, DEFAULT_BIOMETRICS_REQUIRED_TO_CHANGE_PIN)

    override val lastSelectedDeviceIdOnMap = string(KEY_LAST_SELECTED_DEVICE_ID_ON_MAP, "")

    override val locationRefreshPeriod = enum(
        KEY_LOCATION_REFRESH_PERIOD,
        DEFAULT_LOCATION_REFRESH_PERIOD
    )

    override val locationOnBatterySaver = boolean(
        KEY_LOCATION_ON_BATTERY_SAVER,
        DEFAULT_LOCATION_ON_BATTERY_SAVER
    )

    override val locationStaleness = enum(KEY_LOCATION_STALENESS, DEFAULT_LOCATION_STALENESS)

    override val widgetRefreshPeriod = enum(
        KEY_WIDGET_REFRESH_PERIOD,
        DEFAULT_WIDGET_REFRESH_PERIOD
    )

    override val widgetRefreshOnBatterySaver = boolean(
        KEY_WIDGET_REFRESH_ON_BATTERY_SAVER,
        DEFAULT_WIDGET_REFRESH_ON_BATTERY_SAVER
    )

    override val debugModeVisible = boolean(
        KEY_DEBUG_MODE_VISIBLE,
        DEFAULT_DEBUG_MODE_VISIBLE
    )

    override val debugModeEnabled = boolean(
        KEY_DEBUG_MODE_ENABLED,
        DEFAULT_DEBUG_MODE_ENABLED
    )

    override val utsScanEnabled = boolean(
        KEY_UTS_SCAN_ENABLED,
        DEFAULT_UTS_SCAN_ENABLED
    )

    override val utsSensitivity = enum(
        KEY_UTS_SENSITIVITY,
        DEFAULT_UTS_SENSITIVITY
    )

    override val networkContributionsEnabled = boolean(
        KEY_NETWORK_CONTRIBUTIONS_ENABLED,
        DEFAULT_NETWORK_CONTRIBUTIONS_ENABLED
    )

    override val chaserCount = int(
        KEY_CHASER_COUNT,
        DEFAULT_CHASER_COUNT
    )

    override val overmatureOfflinePreventionEnabled = boolean(
        KEY_OVERMATURE_OFFLINE_PREVENTION,
        DEFAULT_OVERMATURE_OFFLINE_PREVENTION
    )

    private val encryptionKey = string(KEY_ENCRYPTION_KEY, "")
    private val encryptionIV = string(KEY_ENCRYPTION_IV, "")

    override val sharedPreferences by lazy {
        SharedPreferencesResolver(context, EncryptedSettingsProvider::class.java)
    }

    @Synchronized
    override fun getDatabaseEncryptionKey(): SecretKey {
        return loadEncryptionKey() ?: saveEncryptionKey()
    }

    @Synchronized
    override fun getDatabaseEncryptionIV(): IvParameterSpec {
        return loadEncryptionIV() ?: saveEncryptionIV()
    }

    override fun ensureKeyAndIV() {
        getDatabaseEncryptionKey()
        getDatabaseEncryptionIV()
    }

    override fun isDebugModeEnabled(scope: CoroutineScope): StateFlow<Boolean?> {
        return combine(
            debugModeVisible.asFlow(),
            debugModeEnabled.asFlow()
        ) { flows ->
            flows.all { it }
        }.stateIn(scope, SharingStarted.Eagerly, null)
    }

    private fun loadEncryptionKey(): SecretKey? {
        val b64Key = encryptionKey.getSync()
        if(b64Key.isEmpty()) return null
        val key = Base64.decode(b64Key, Base64.DEFAULT)
        return SecretKeySpec(key, "AES")
    }

    private fun saveEncryptionKey(): SecretKey {
        return KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey().also {
            encryptionKey.setSync(Base64.encodeToString(it.encoded, Base64.DEFAULT))
        }
    }

    private fun loadEncryptionIV(): IvParameterSpec? {
        val b64IV = encryptionIV.getSync()
        if(b64IV.isEmpty()) return null
        val iv = Base64.decode(b64IV, Base64.DEFAULT)
        return IvParameterSpec(iv)
    }

    private fun saveEncryptionIV(): IvParameterSpec {
        val bytes = ByteArray(16)
        SecureRandom().apply {
            nextBytes(bytes)
        }
        return IvParameterSpec(bytes).also {
            encryptionIV.setSync(Base64.encodeToString(bytes, Base64.DEFAULT))
        }
    }

    /**
     *  Use reflection to load all settings fields not marked with [IgnoreInBackup], which can be
     *  backed up and restored from
     */
    private fun getBackupFields(): List<UTagSetting<*>> {
        return EncryptedSettingsRepository::class.memberProperties.filter {
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