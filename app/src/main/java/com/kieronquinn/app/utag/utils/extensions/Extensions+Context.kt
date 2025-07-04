package com.kieronquinn.app.utag.utils.extensions

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.LocaleManager
import android.app.Service
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.UpdateRepository.Companion.CONTENT_TYPE_APK
import com.kieronquinn.app.utag.ui.activities.MainActivity
import com.kieronquinn.app.utag.xposed.extensions.hasPermission
import com.kieronquinn.app.utag.xposed.extensions.isPackageInstalled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.java.KoinJavaComponent.inject
import java.util.Locale
import kotlin.math.acos

val Context.isDarkMode: Boolean
    get() {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

//Hidden action, but can still be received
private const val ACTION_CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED"

fun Context.getDarkMode(
    scope: CoroutineScope
) = broadcastReceiverAsFlow(IntentFilter(ACTION_CONFIGURATION_CHANGED)).map {
    isDarkMode
}.stateIn(scope, SharingStarted.Eagerly, isDarkMode)

val Context.onAccent: Int
    get() = ContextCompat.getColor(this, R.color.on_accent)


val Context.onAccentInverse: Int
    get() = ContextCompat.getColor(this, R.color.on_accent_inverse)

fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun Context.hasLocationPermissions(): Boolean {
    return hasPermission(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    )
}

fun Context.hasBackgroundLocationPermission(): Boolean {
    return hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
}

fun Context.getAttrColor(@AttrRes attr: Int): Int {
    val obtainStyledAttributes: TypedArray = obtainStyledAttributes(intArrayOf(attr))
    val color = obtainStyledAttributes.getColor(0, 0)
    obtainStyledAttributes.recycle()
    return color
}

fun Context.getActionBarSize(): Int {
    val typedValue = TypedValue().apply {
        theme.resolveAttribute(
            android.R.attr.actionBarSize, this, true
        )
    }
    return TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
}

fun Context.navigateTo(location: LatLng) {
    try {
        val locationIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
        }
        startActivity(locationIntent)
    }catch (e: ActivityNotFoundException) {
        val fallbackLocationIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(
                "http://maps.google.com/maps?daddr=${location.latitude},${location.longitude}"
            )
        }
        startActivity(fallbackLocationIntent)
    }
}

fun Context.hasNotificationPermission(): Boolean {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return checkCallingOrSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

@Suppress("DEPRECATION")
fun Context.getVibrator(): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver,
    intentFilter: IntentFilter
): Intent? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, intentFilter, RECEIVER_EXPORTED)
    }else{
        registerReceiver(receiver, intentFilter)
    }
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.broadcastReceiverAsFlow(
    intentFilter: IntentFilter
) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    registerReceiverCompat(receiver, intentFilter)
    awaitClose {
        unregisterReceiver(receiver)
    }
}

private var lastBluetoothState: Boolean? = null

fun Context.bluetoothEnabledAsFlow(scope: CoroutineScope): StateFlow<BluetoothState> {
    val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    return broadcastReceiverAsFlow(
        IntentFilter(ACTION_STATE_CHANGED)
    ).map {
        val enabled = bluetoothAdapter.isEnabled
        val change = lastBluetoothState != enabled
        lastBluetoothState = enabled
        BluetoothState(enabled, change)
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        BluetoothState(bluetoothAdapter.isEnabled, false)
    )
}

data class BluetoothState(val enabled: Boolean, val change: Boolean)

//Safe to use getRunningServices for our own service
@Suppress("deprecation")
fun Context.isServiceRunning(serviceClass: Class<out Service>): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.getRunningServices(Integer.MAX_VALUE).any {
        it?.service?.className == serviceClass.name
    }
}

fun Context.getInclination(delay: Long = 500L): Flow<Int?> {
    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    return if(accelerometerSensor != null && magnetometerSensor != null) {
        combine(
            sensorManager.listener(accelerometerSensor, delay),
            sensorManager.listener(magnetometerSensor, delay)
        ) { accelerometer, magnetometer ->
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)
            SensorManager.getRotationMatrix(
                rotationMatrix,
                inclinationMatrix,
                accelerometer,
                magnetometer
            )
            Math.round(Math.toDegrees(acos(rotationMatrix[8].toDouble()))).toInt()
        }
    }else flowOf(null)
}

/**
 *  Apps installed with the Session-based Package Installer are exempt from restrictions. We can
 *  check if we were installed with the session based installer by checking if the install package
 *  is not the same as the regular package installer. This varies by device, so we look it up with
 *  [getPackageInstallerPackageName]
 */
@Suppress("DEPRECATION")
fun Context.wasInstalledWithSession(): Boolean {
    val packageManager = packageManager
    //No default installer set = always show UI to be safe
    val defaultInstaller = packageManager.getPackageInstallerPackageName() ?: return false
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName != defaultInstaller
        } else {
            packageManager.getInstallerPackageName(packageName) != defaultInstaller
        }
    }catch (e: NameNotFoundException) {
        false
    }
}

fun Context.showAppInfo() {
    val component = ComponentName(this, MainActivity::class.java)
    val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    launcherApps.startAppDetailsActivity(
        component,
        Process.myUserHandle(),
        Rect(0, 0, 0, 0),
        ActivityOptions.makeBasic().toBundle()
    )
}

fun getCameraLaunchIntent(secure: Boolean): Intent {
    val action = if(secure) MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
    else MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
    return Intent(action).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

//https://cs.android.com/android/platform/superproject/+/master:cts/tests/tests/packageinstaller/install/src/android/packageinstaller/install/cts/InstallSourceInfoTest.kt;l=95
@Suppress("DEPRECATION")
private fun PackageManager.getPackageInstallerPackageName(): String? {
    val installerIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
    installerIntent.setDataAndType(Uri.parse("content://com.example/"), CONTENT_TYPE_APK)
    return installerIntent.resolveActivityInfo(this, PackageManager.MATCH_DEFAULT_ONLY)
        ?.packageName
}

private fun SensorManager.listener(sensor: Sensor, delay: Long) = callbackFlow<FloatArray> {
    var lastEmission = 0L
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val now = System.currentTimeMillis()
            if(now - lastEmission > delay) {
                lastEmission = now
                trySend(event.values)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            //No-op
        }
    }
    registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    awaitClose {
        unregisterListener(listener)
    }
}

fun Context.copyToClipboard(text: String, label: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

@SuppressLint("DiscouragedApi")
fun Context.getSupportedLocales(): List<Locale> {
    val id = resources.getIdentifier(
        "_generated_res_locale_config",
        "xml",
        packageName
    )
    val localeXml = resources.getXml(id)
    val locales = ArrayList<String>()
    var event = localeXml.next()
    while(event != XmlResourceParser.END_DOCUMENT) {
        if(event == XmlResourceParser.START_TAG && localeXml.name == "locale") {
            locales.add(localeXml.getAttributeValue(0))
        }
        event = localeXml.next()
    }
    return locales.map {
        Locale.forLanguageTag(it)
    }
}

fun Context.getSelectedLanguage(supportedLocales: List<String>): Locale? {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = getSystemService(Context.LOCALE_SERVICE) as LocaleManager
        val locales = localeManager.getApplicationLocales(packageName)
        locales.getFirstMatch(supportedLocales.toTypedArray())
    }else{
        val settingsRepository by inject<SettingsRepository>(SettingsRepository::class.java)
        settingsRepository.locale.getSync().takeIf { it.isNotEmpty() }?.let {
            Locale.forLanguageTag(it)
        }
    }
}

/**
 *  Generates a Chrome user agent based on the version currently installed on the device. If
 *  Chrome has been disabled or uninstalled, uses the fallback version number instead.
 *
 *  This is used during login as Chrome is a browser supported by Samsung's login and is the package
 *  we spoof when creating the auth request. Currently Samsung don't check the UA (only the package
 *  we send), but this would be an obvious discrepancy that could otherwise be used to block uTag.
 */
fun Context.getFakeChromeUA(): String {
    val chromeVersion = try {
        packageManager.getPackageInfo("com.android.chrome", 0).versionName
            ?.reduceChromeVersion()
    } catch (e: NameNotFoundException) {
        null
    } ?: "133.0.0.0"
    return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
}

/**
 *  Remove version info except the major version, per [documentation](https://developers.google.com/privacy-sandbox/blog/user-agent-reduction-android-model-and-version)
 */
private fun String.reduceChromeVersion(): String? {
    if(!contains(".")) return null
    val majorVersion = split(".")[0]
    return "$majorVersion.0.0.0"
}

/**
 *  Checks for the existence of the Samsung Account system app. When installed, this app prevents
 *  sign in on the re-signed modded SmartThings, so only rooted users can proceed.
 */
fun Context.isOneUI(): Boolean {
    return packageManager.isPackageInstalled(
        "com.osp.app.signin",
        PackageManager.MATCH_SYSTEM_ONLY or PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_UNINSTALLED_PACKAGES
    )
}

fun Context.isOneUICompatible(): Boolean {
    return try {
        val info = packageManager.getPermissionInfo(
            "com.sec.android.permission.PERSONAL_MEDIA",
            0
        )
        //If the permission is not originally declared by SmartThings, then the mod won't work
        info.packageName == PACKAGE_NAME_ONECONNECT
    }catch (e: NameNotFoundException) {
        //If the permission is not found at all, then the mod will work fine
        true
    }
}

fun Context.wrapAsApplicationContext(): Context {
    return object: ContextWrapper(this) {
        override fun getApplicationContext(): Context {
            return this
        }
    }
}