package com.kieronquinn.app.utag.xposed

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.Instrumentation
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.ContentProvider
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.ParcelUuid
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import com.kieronquinn.app.utag.model.LocationStaleness
import com.kieronquinn.app.utag.service.ILocationCallback
import com.kieronquinn.app.utag.service.IServiceConnection
import com.kieronquinn.app.utag.service.IUTagSmartThingsForegroundService
import com.kieronquinn.app.utag.xposed.Xposed.Companion.SCAN_TYPE_UTAG
import com.kieronquinn.app.utag.xposed.Xposed.Companion.SERVICE_ID
import com.kieronquinn.app.utag.xposed.Xposed.Companion.SharedPrefsKey.Companion.areAllKeysPresent
import com.kieronquinn.app.utag.xposed.core.BuildConfig
import com.kieronquinn.app.utag.xposed.extensions.TagActivity_createIntent
import com.kieronquinn.app.utag.xposed.extensions.UTagPassiveModeProvider_isInPassiveMode
import com.kieronquinn.app.utag.xposed.extensions.UTagXposedProvider_isSmartThingsModded
import com.kieronquinn.app.utag.xposed.extensions.UTagXposedProvider_showSetupToast
import com.kieronquinn.app.utag.xposed.extensions.UnsupportedIntentActivity_createIntent
import com.kieronquinn.app.utag.xposed.extensions.XposedCrashReportProvider_reportNonFatal
import com.kieronquinn.app.utag.xposed.extensions.applySecurity
import com.kieronquinn.app.utag.xposed.extensions.getLocation
import com.kieronquinn.app.utag.xposed.extensions.getParcelableCompat
import com.kieronquinn.app.utag.xposed.extensions.getRequiredOneConnectPermissions
import com.kieronquinn.app.utag.xposed.extensions.hasPermission
import com.kieronquinn.app.utag.xposed.extensions.isInForeground
import com.kieronquinn.app.utag.xposed.extensions.isPackageInstalled
import com.kieronquinn.app.utag.xposed.extensions.isStandaloneModule
import com.kieronquinn.app.utag.xposed.extensions.provideContext
import com.kieronquinn.app.utag.xposed.extensions.runSafely
import com.kieronquinn.app.utag.xposed.extensions.runWithClearedIdentity
import com.kieronquinn.app.utag.xposed.utils.PauseResumeLifecycleCallbacks
import com.kieronquinn.app.utag.xposed.utils.SigBypass
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.neonorbit.dexplore.DexFactory
import io.github.neonorbit.dexplore.Dexplore
import io.github.neonorbit.dexplore.ReferencePool
import io.github.neonorbit.dexplore.filter.ClassFilter
import io.github.neonorbit.dexplore.filter.DexFilter
import io.github.neonorbit.dexplore.filter.MethodFilter
import io.github.neonorbit.dexplore.filter.ReferenceTypes
import io.github.neonorbit.dexplore.result.ClassData
import io.github.neonorbit.dexplore.result.MethodData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Modifier
import java.util.UUID
import kotlin.system.exitProcess


class Xposed: IXposedHookLoadPackage {

    companion object {
        private val CALLING_PACKAGES_ALLOWLIST = setOf(
            "com.samsung.android.oneconnect.onboarding.launcher.prepare.checker.category.",
            "com.samsung.android.oneconnect.pluginsupport.installation.",
            "com.samsung.android.oneconnect.webplugin.jsinterface.",
            "com.samsung.android.oneconnect.feature.blething.tag.ble.",
            "com.samsung.android.oneconnect.domainlayer.repository.entity.",
            "com.samsung.android.oneconnect.companionservice.spec.entity.",
            "com.samsung.android.oneconnect.domainlayer.domain.notification.",
            "com.samsung.android.oneconnect.notification.",
            "com.samsung.android.oneconnect.diagnostics.model.device.ocf.",
            "com.samsung.android.oneconnect.support.connection.",
            "com.samsung.android.oneconnect.adddevice.ui.",
        )

        private val ACTIVITY_WARNING_ALLOWLIST = setOf(
            "com.samsung.android.oneconnect.ui.scmain.SCMainActivity",
            "com.samsung.android.oneconnect.webplugin.WebPluginActivity",
        )

        private val VMF_DENYLIST = setOf(
            "com.samsung.one.plugin.trkplugin_125300005"
        )

        private const val CSS_OVERRIDES =
            "#SmartButtonActionCard,#button_service_list{height:auto!important}" +
                    "#findYourPhoneCard,#findYourPhone,#buttonService{display:none}"

        private const val SHARED_PREFS_NAME = "utag"
        private const val PACKAGE_SAMSUNG_ACCOUNT = "com.osp.app.signin"
        private const val CLASS_CLOUD_NOTIFICATION_MANAGER =
            "com.samsung.android.oneconnect.notification.CloudNotificationManager"

        private const val LOCATION_TIMEOUT = 30_000L //30 seconds
        //Last version before lockups started
        private const val PLATFORM_VERSION_OVERRIDE = 101600001

        //Copied from main module
        const val APPLICATION_ID = "com.kieronquinn.app.utag"
        const val PACKAGE_NAME_ONECONNECT = "com.samsung.android.oneconnect"
        const val PACKAGE_NAME_UTAG = "com.kieronquinn.app.utag"
        private const val SERVICE_ID = "0000FD5A-0000-1000-8000-00805F9B34FB"
        const val METHOD_IS_IN_PASSIVE_MODE = "is_in_passive_mode"
        const val METHOD_IS_SMARTTHINGS_MODDED = "is_smartthings_modded"
        const val METHOD_SHOW_SETUP_TOAST = "show_setup_toast"

        private val COMPONENT_FME = ComponentName(
            "com.samsung.android.oneconnect", "com.samsung.android.plugin.fme.MainActivity"
        )

        private const val ACTIVITY_SHORTCUT =
            "com.samsung.android.oneconnect.ui.DummyActivityForShortcut"

        const val ACTION_TAG_DEVICE_STATUS_CHANGED =
            "$APPLICATION_ID.action.TAG_DEVICE_STATUS_CHANGED"

        const val ACTION_SCAN_RECEIVED = "$APPLICATION_ID.action.SCAN_RECEIVED"
        const val ACTION_TAG_SCAN_RECEIVED = "$APPLICATION_ID.action.TAG_SCAN_RECEIVED"
        const val ACTION_SMARTTHINGS_PAUSED = "$APPLICATION_ID.action.SMARTTHINGS_PAUSED"
        const val ACTION_SMARTTHINGS_RESUMED = "$APPLICATION_ID.action.SMARTTHINGS_RESUMED"
        const val ACTION_HOOKING_FINISHED = "$APPLICATION_ID.action.HOOKING_FINISHED"
        const val ACTION_ONECONNECT_MESSAGE = "$APPLICATION_ID.action.ONECONNECT_MESSAGE"

        const val CAPSULE_PROVIDER_RESULT = "result"
        const val CAPSULE_PROVIDER_EXTRA_INTENT = "intent"
        const val CAPSULE_PROVIDER_EXTRA_CONNECTION = "connection"
        const val CAPSULE_PROVIDER_EXTRA_START_FIRST = "start_first"
        const val CAPSULE_PROVIDER_EXTRA_STALENESS = "staleness"

        const val SCAN_TYPE_UTAG = 0x8824
        const val SCAN_ID_UTAG = 0x8824
        const val SCAN_ID_UTAG_ALT = 0x8825
        const val SCAN_TYPE_UTAG_ALT = 8
        const val EXTRA_NOTIFICATION_CONTENT = "utag_notification_content"
        const val EXTRA_BINDER = "utag_binder"
        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_SERVICE_DATA = "service_data"
        const val EXTRA_BLE_MAC = "ble_mac"
        const val EXTRA_RSSI = "rssi"
        const val EXTRA_CHARACTERISTICS = "characteristics"
        const val EXTRA_VALUE = "value"
        const val EXTRA_INTENT = "intent"
        const val EXTRA_RESULT = "result"

        enum class CapsuleProviderMethod {
            GET_VERSION_CODE,
            GET_VERSION_NAME,
            BIND_SERVICE,
            UNBIND_SERVICE,
            HAS_REQUIRED_PERMISSIONS,
            GET_LOCATION,
            IS_FOREGROUND,
            GET_ENABLE_BLUETOOTH_INTENT,
            ARE_HOOKS_SETUP
        }

        private enum class SharedPrefsKey(private val key: String) {
            SHARED_PREF_KEY_SYSTEM_INFO_METHOD("system_info_method"),
            SHARED_PREF_KEY_SYSTEM_INFO_METHOD_ALT("system_info_method_alt"),
            SHARED_PREF_KEY_DEBUG_CLASS("debug_class"),
            SHARED_PREF_KEY_QCSERVICE_RUNNABLE_METHOD("qcservice_runnable_method"),
            SHARED_PREF_KEY_SMART_TAG_CONNECT_METHOD("smart_tag_connect_method"),
            SHARED_PREF_KEY_SCAN_NOTIFY_METHOD("scan_notify_method"),
            SHARED_PREF_KEY_SCAN_REPOSITORY_METHOD("scan_repository_method"),
            SHARED_PREF_KEY_PUBLISH_DEVICE_STATUS_METHOD("publish_device_status_method"),
            SHARED_PREF_KEY_SCAN_CALLBACK_CLASS("scan_callback_class"),
            SHARED_PREF_KEY_DISCONNECT_METHOD("disconnect_method"),
            SHARED_PREF_KEY_FORCE_DISCONNECT_METHOD("force_disconnect_method"),
            SHARED_PREF_KEY_FIREBASE_PUSH_INTENT("firebase_push_intent"),
            SHARED_PREF_KEY_IS_FMM_SUPPORTED("is_fmm_supported"),
            ;

            fun getKey(version: Long): String {
                return "${key}_$version"
            }

            companion object {
                fun SharedPreferences.areAllKeysPresent(version: Long): Boolean {
                    return entries.all { contains(it.getKey(version)) }
                }
            }
        }
    }

    private val boundServices = HashMap<Int, ServiceConnection>()
    private var overrideScanParams = false

    private var uTagScanCallback: ScanCallback? = null
    private val scanLock = Mutex()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var packageInfo: PackageInfo
    private lateinit var dexplore: Dexplore

    private val lifecycleCallbacks = object: PauseResumeLifecycleCallbacks() {
        override fun onActivityResumed(activity: Activity) {
            activity.sendBroadcast(ACTION_SMARTTHINGS_RESUMED)
        }

        override fun onActivityPaused(activity: Activity) {
            activity.sendBroadcast(ACTION_SMARTTHINGS_PAUSED)
        }

        /**
         *  Send a broadcast for the foreground state *maybe* changing. This will call back to the
         *  capsule provider using [CapsuleProviderMethod.IS_FOREGROUND] later to see if the state
         *  has actually changed.
         */
        private fun Activity.sendBroadcast(action: String) {
            sendBroadcast(Intent(action).apply {
                `package` = PACKAGE_NAME_UTAG
                applySecurity(this@sendBroadcast)
            })
        }
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != PACKAGE_NAME_ONECONNECT) return
        lpparam.hookApplicationCreate()
        lpparam.hookContext()
    }

    private fun LoadPackageParam.hookApplicationCreate() {
        XposedHelpers.findAndHookMethod(
            "com.samsung.android.oneconnect.BaseApplication",
            classLoader,
            "onCreate",
            object: XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val application = param.thisObject as Application
                    application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
                }
            }
        )
    }

    private fun Context.handleLoadApplication(lpparam: LoadPackageParam) {
        val isStandalone = isStandaloneModule()
        val isMainProcess = Application.getProcessName() == PACKAGE_NAME_ONECONNECT
        if(!isStandalone) {
            //We have to go via the provider as the signature is spoofed
            val isModded = UTagXposedProvider_isSmartThingsModded(this)
            if(isModded) {
                XposedBridge.log("uTag is modded and hooked, aborting hooking! Disable the Xposed module or reinstall from Google Play to fix this.")
                return
            }
        }else{
            SigBypass.doSigBypass(this, lpparam.classLoader)
        }
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        dexplore = DexFactory.load(lpparam.appInfo.sourceDir)
        packageInfo = packageManager.getPackageInfo(packageName, 0)
        val requiresSetup = !sharedPreferences.areAllKeysPresent(packageInfo.longVersionCode)
        if(requiresSetup && isMainProcess) {
            //Has to go via provider since it has a higher chance of having notification permission
            UTagXposedProvider_showSetupToast(this)
        }
        hookViewMap()
        hookActivity()
        hookWebView()
        deleteVmfDenylist()
        lpparam.hookRootChecks()
        lpparam.hookIsFmmSupported(this)
        lpparam.hookStartScan()
        lpparam.hookTagDozeModeService()
        lpparam.hookCapsuleProvider()
        lpparam.hookSystemInfo(this)
        lpparam.hookQcServiceRunnable(this)
        lpparam.hookPublishDeviceStatus(this)
        lpparam.hookSmartTagGattConnecter(this)
        lpparam.hookDeviceBleThingsManager(this)
        lpparam.hookScanCallback(this)
        lpparam.hookDebug(this)
        lpparam.hookCheckDisconnect(this)
        lpparam.hookOneConnectPushNotifications(this)
        lpparam.hookSamsungAccount()
        lpparam.hookShortcutActivity()
        lpparam.hookPlatformVersion()
        if(requiresSetup) {
            sendBroadcast(Intent(ACTION_HOOKING_FINISHED).apply {
                applySecurity(this@handleLoadApplication)
                `package` = PACKAGE_NAME_UTAG
            })
        }
    }

    private fun Context.deleteVmfDenylist() {
        val filesDir = filesDir?.takeIf { it.exists() } ?: return
        val rootDir = filesDir.parentFile?.takeIf { it.exists() } ?: return
        val vmfDir = File(rootDir, "vmf").takeIf { it.exists() } ?: return
        val apkDir = File(vmfDir, "apk").takeIf { it.exists() } ?: return
        VMF_DENYLIST.forEach { name ->
            File(apkDir, name).takeIf { file -> file.exists() }?.deleteRecursively()
        }
    }

    /**
     *  SmartThings checks for rooted devices but only on non-debug builds. We spoof a debug build
     *  but only for the SCMainActivity caller.
     */
    private fun LoadPackageParam.hookRootChecks() {
        XposedHelpers.findAndHookMethod(
            "android.app.ContextImpl",
            classLoader,
            "getApplicationInfo",
            object: XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val isFromMainActivity = getCallingInformation()?.third?.any {
                        it == "com.samsung.android.oneconnect.ui.scmain.SCMainActivity"
                    } ?: return
                    if(isFromMainActivity) {
                        val info = param.result as ApplicationInfo
                        info.flags = info.flags or ApplicationInfo.FLAG_DEBUGGABLE
                    }
                }
            }
        )
    }

    private fun LoadPackageParam.hookOneConnectPushNotifications(context: Context) {
        val savedMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_FIREBASE_PUSH_INTENT)
        val method = if(savedMethod == null) {
            val classFilter = ClassFilter.Builder()
                .setClasses("com.google.firebase.messaging.FirebaseMessagingService")
                .build()
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("FirebaseApp has not being initialized. Device might be in direct boot mode. Skip exporting delivery metrics to Big Query")
                }
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_FIREBASE_PUSH_INTENT, it)
            }
        }else{
            savedMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to hook oneconnect push method (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        XposedBridge.hookMethod(
            method,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    val intent = param?.args?.get(0) as? Intent ?: return
                    val context = param.thisObject as Service
                    val broadcastIntent = Intent(ACTION_ONECONNECT_MESSAGE).apply {
                        `package` = APPLICATION_ID
                        applySecurity(context)
                        putExtra(EXTRA_INTENT, intent)
                    }
                    context.sendBroadcast(broadcastIntent)
                }
            }
        )
    }

    /**
     *  Makes checks for the Samsung Account app fail, which is part of the process of forcing
     *  SmartThings on OneUI to use the web-based login.
     */
    private fun LoadPackageParam.hookSamsungAccount() {
        XposedHelpers.findAndHookMethod(
            Intent::class.java,
            "setPackage",
            String::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    if(param.args[0] == PACKAGE_SAMSUNG_ACCOUNT) {
                        param.args[0] = "${PACKAGE_SAMSUNG_ACCOUNT}2"
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "android.app.ApplicationPackageManager",
            classLoader,
            "getPackageInfo",
            String::class.java,
            Integer.TYPE,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    if(param.args[0] == PACKAGE_SAMSUNG_ACCOUNT) {
                        param.throwable = NameNotFoundException()
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "android.app.ApplicationPackageManager",
            classLoader,
            "getApplicationInfo",
            String::class.java,
            Integer.TYPE,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    if(param.args[0] == PACKAGE_SAMSUNG_ACCOUNT) {
                        param.throwable = NameNotFoundException()
                    }
                }
            }
        )
    }

    /**
     *  Injects CSS to hide the find phone option from the WebView. The option is instead available
     *  in uTag's UI.
     */
    private fun hookWebView() {
        XposedHelpers.findAndHookMethod(
            WebViewClient::class.java,
            "onPageFinished",
            WebView::class.java,
            String::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val webView = param.args[0] as WebView
                    val url = param.args[1] as String
                    if(url.contains("com.samsung.android.oneconnect/vmf/apk")) {
                        val js = "var style = document.createElement('style'); style.innerHTML = " +
                                "'$CSS_OVERRIDES'; document.head.appendChild(style);"
                        webView.evaluateJavascript(js, null)
                    }
                }
            }
        )
    }

    /**
     *  SmartThings contains a foreground service which is normally used for automation running.
     *  We hijack an instance of this service, bind to it, and keep it running with a custom
     *  notification to keep SmartThings running. The notification content is passed in the Intent,
     *  conveniently partially by default, but also via some hooking for the content. We also check
     *  if the service is ours by looking for this custom content in the last onStartCommand intent
     *  or bind intent.
     */
    private fun LoadPackageParam.hookTagDozeModeService() {
        val clazz = "com.samsung.android.oneconnect.manager.TagDozeModeService"
        var lastIntent: Intent? = null
        val isUtag = {
            lastIntent?.hasExtra(EXTRA_NOTIFICATION_CONTENT) == true
        }
        XposedHelpers.findAndHookMethod(
            clazz,
            classLoader,
            "onStartCommand",
            Intent::class.java,
            Integer.TYPE,
            Integer.TYPE,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    //We need to use this intent to check if this service is for uTag
                    lastIntent = param.args[0] as Intent
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    if(isUtag()) {
                        /*
                            By default, SmartThings will stop this service after a timeout, but this
                            only happens if it's not been stopped already. This is determined by a
                            boolean global field, so we set it to false without actually stopping
                            the service, and thus it never gets stopped after the timeout.

                            The service also checks this value in the stop action, so our service
                            instance is immune to that too.
                         */
                        val isRunningField = param.thisObject::class.java.fields.firstOrNull {
                            it.type == Boolean::class.java
                        } ?: return
                        isRunningField.set(param.thisObject, false)
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            clazz,
            classLoader,
            "onBind",
            Intent::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val service = param.thisObject as Service
                    val intent = param.args[0] as? Intent ?: return
                    //Stop this service instance if the uTag foreground service is force stopped
                    val deathRecipient = DeathRecipient {
                        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
                        service.stopSelf()
                    }
                    if(intent.hasExtra(EXTRA_BINDER)) {
                        val binder = intent.extras?.getBinder(EXTRA_BINDER)
                        binder?.linkToDeath(deathRecipient, 0)
                    }
                    if(intent.hasExtra(EXTRA_NOTIFICATION_CONTENT)) {
                        //Return a binder so we can monitor this service's state
                        param.result = UTagSmartThingsForegroundService(WeakReference(service))
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            Notification.Builder::class.java,
            "setContentText",
            CharSequence::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val notificationContent = lastIntent?.getStringExtra(EXTRA_NOTIFICATION_CONTENT)
                        ?: return
                    if (getCallingInformation()?.first == clazz) {
                        //Replace notification text with our own
                        param.args[0] = notificationContent as CharSequence
                        //Hide the time since this notification will be shown indefinitely
                        val builder = param.thisObject as Notification.Builder
                        builder.setShowWhen(false)
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            Notification.BigTextStyle::class.java,
            "bigText",
            CharSequence::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val notificationContent = lastIntent?.getStringExtra(EXTRA_NOTIFICATION_CONTENT)
                        ?: return
                    if (getCallingInformation()?.first == clazz) {
                        //Replace notification big text with our own
                        param.args[0] = notificationContent as CharSequence
                    }
                }
            }
        )
    }

    /**
     *  By default, SmartThings doesn't apply scan filters to its Bluetooth LE scans. This is fine
     *  for its normal use case, since it never intends to scan while the screen is off, since
     *  OneUI supports passive BLE scanning. We don't have this luxury, so when [overrideScanParams]
     *  is set and the scan filter list is empty, we override it to be a filter for the Tag's
     *  [SERVICE_ID]. This is set by passing a special scan type ([SCAN_TYPE_UTAG]) into the
     *  `startDiscovery` call, which only uTag will do, and is reset in all `stopDiscovery` calls.
     */
    private fun LoadPackageParam.hookStartScan() {
        XposedHelpers.findAndHookMethod(
            BluetoothLeScanner::class.java,
            "startScan",
            List::class.java,
            ScanSettings::class.java,
            ScanCallback::class.java,
            object: XC_MethodHook() {
                @SuppressLint("MissingPermission")
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    synchronized(scanLock) {
                        if (param.args[0] == null && overrideScanParams) {
                            overrideScanParams = false
                            //Set scan mode to low power since we'll be keeping running
                            param.args[1] = ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                                .setReportDelay(0)
                                .build()
                            param.args[0] = listOf(
                                ScanFilter.Builder()
                                    .setServiceUuid(ParcelUuid.fromString(SERVICE_ID.lowercase()))
                                    .build()
                            )
                            //Add callback to stored set
                            uTagScanCallback = param.args[2] as ScanCallback
                        }
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "com.samsung.android.oneconnect.core.QcServiceImpl",
            classLoader,
            "startDiscovery",
            Integer.TYPE,
            Integer.TYPE,
            Boolean::class.java,
            Boolean::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    if(param.args[0] == SCAN_TYPE_UTAG) {
                        //This request has come from uTag, set the scan override
                        overrideScanParams = true
                        //And set the scan type to what it should actually be
                        param.args[0] = 8
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "com.samsung.android.oneconnect.core.QcServiceImpl",
            classLoader,
            "stopDiscovery",
            Integer.TYPE,
            Boolean::class.java,
            object: XC_MethodHook() {
                @SuppressLint("MissingPermission")
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    synchronized(scanLock) {
                        if (param.args[0] == SCAN_TYPE_UTAG) {
                            //Ignore this call
                            param.result = true
                        }
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            BluetoothLeScanner::class.java,
            "stopScan",
            ScanCallback::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    if(param.args[0] == uTagScanCallback) {
                        //We always want to be scanning, so prevent this call
                        param.result = true
                    }
                }
            }
        )
    }

    /**
     *  Replaces the ContentProvider call which checks for FMM support when a push notification is
     *  received with a fake call to our own provider which always returns true. This is the final
     *  step in allowing push notifications to work.
     */
    private fun LoadPackageParam.hookIsFmmSupported(context: Context) {
        val savedMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_IS_FMM_SUPPORTED)
        val method = if(savedMethod == null) {
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("isSupportFindMyMobileFeature")
                }
                .build()
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("isSupportFindMyMobileFeature")
                }
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_IS_FMM_SUPPORTED, it)
            }
        }else{
            savedMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to hook isFmmPackageInstalled (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        XposedBridge.hookMethod(
            method,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val calling = getCallingInformation()?.first ?: return
                    if(calling == CLASS_CLOUD_NOTIFICATION_MANAGER) {
                        param.result = true
                    }
                }
            }
        )
    }

    private fun hookActivity() {
        XposedHelpers.findAndHookMethod(
            Activity::class.java,
            "onResume",
            object: XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val activity = param.thisObject as Activity
                    val isUtagInstalled = activity.packageManager
                        .isPackageInstalled(PACKAGE_NAME_UTAG)
                    val showWarning = ACTIVITY_WARNING_ALLOWLIST
                        .contains(param.thisObject::class.java.name)
                    if(showWarning && !isUtagInstalled) {
                        activity.showUTagUninstalledDialog()
                    }
                }
            }
        )
    }

    private fun LoadPackageParam.hookShortcutActivity() {
        XposedHelpers.findAndHookMethod(
            ACTIVITY_SHORTCUT,
            classLoader,
            "onCreate",
            Bundle::class.java,
            object: XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val activity = param.thisObject as Activity
                    val intent = activity.intent ?: return
                    val code = intent.getStringExtra("code") ?: return
                    if(code.startsWith("MO_FME_V_")) {
                        val data = intent.getStringExtra("data")?.let {
                            try {
                                JSONObject(it)
                            }catch (e: JSONException) {
                                null
                            }
                        } ?: return
                        val deviceId = data.getString("stDevId") ?: return
                        val intent = TagActivity_createIntent(deviceId)
                        activity.startActivity(intent)
                        activity.finish()
                    }
                }
            }
        )
    }

    private fun LoadPackageParam.hookPlatformVersion() {
        val pluginInfoRequest = XposedHelpers.findClass(
            "com.samsung.android.pluginplatform.service.store.StorePluginInfoRequest",
            classLoader
        )
        XposedBridge.hookAllMethods(
            pluginInfoRequest,
            "getPlatformVersion",
            object: XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val currentVersion = (param.result as String).toLongOrNull() ?: return
                    if(currentVersion > PLATFORM_VERSION_OVERRIDE) {
                        param.result = PLATFORM_VERSION_OVERRIDE.toString()
                    }
                }
            }
        )
    }

    private fun Activity.showUTagUninstalledDialog() {
        AlertDialog.Builder(this).apply {
            //No access to resources at this point, so English only
            setTitle("uTag not installed")
            if(isStandaloneModule()) {
                setMessage("uTag is no longer installed, the modded build of SmartThings will not work properly. Please uninstall & reinstall SmartThings from the Play Store or reinstall uTag.")
            }else{
                setMessage("uTag is no longer installed, SmartThings mods will not work properly. SmartThings will now close and the mods will be disabled.")
            }
            setCancelable(false)
            setPositiveButton("Close") { _, _ ->
                //Kill all activities
                exitProcess(0)
            }
        }.show()
    }

    /**
     *  The CapsuleProvider is an exported, permission-less provider in SmartThings. By hooking its
     *  call method, we can pass data to/from the SmartThings app from uTag, without needing to
     *  wait for a broadcast response.
     */
    private fun LoadPackageParam.hookCapsuleProvider() {
        XposedHelpers.findAndHookMethod(
            "com.samsung.android.sdk.bixby2.provider.CapsuleProvider",
            classLoader,
            "call",
            String::class.java,
            String::class.java,
            Bundle::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val contentProvider = param.thisObject as ContentProvider
                    val method = param.args[0] as String
                    val arg = param.args[1] as? String
                    val extras = param.args[2] as? Bundle
                    contentProvider.handleCapsuleProvider(method, arg, extras)?.let {
                        param.result = it
                    }
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun ContentProvider.handleCapsuleProvider(
        method: String,
        arg: String?,
        extras: Bundle?
    ): Bundle? {
        val capsuleMethod = CapsuleProviderMethod.entries.firstOrNull { it.name == method }
        if(capsuleMethod != null) {
            //Attempting to call a custom method, check that the caller is uTag and throw if not
            if(callingPackage != APPLICATION_ID) {
                throw SecurityException("Unauthorised access")
            }
        }
        return when(capsuleMethod) {
            CapsuleProviderMethod.GET_VERSION_CODE -> {
                bundleOf(CAPSULE_PROVIDER_RESULT to BuildConfig.XPOSED_CODE)
            }
            CapsuleProviderMethod.GET_VERSION_NAME -> {
                bundleOf(CAPSULE_PROVIDER_RESULT to BuildConfig.XPOSED_NAME)
            }
            CapsuleProviderMethod.BIND_SERVICE -> {
                val binder = extras?.getBinder(CAPSULE_PROVIDER_EXTRA_CONNECTION)
                val startFirst = extras?.getBoolean(CAPSULE_PROVIDER_EXTRA_START_FIRST) ?: false
                val connection = binder?.let {
                    IServiceConnection.Stub.asInterface(it)
                }
                val intent = extras?.getParcelableCompat(
                    CAPSULE_PROVIDER_EXTRA_INTENT, Intent::class.java
                )
                val hash = binder.hashCode()
                return if(connection != null && intent != null) {
                    if(startFirst) {
                        provideContext().startService(intent)
                    }
                    bindService(intent, connection, hash)
                    bundleOf(CAPSULE_PROVIDER_RESULT to true)
                }else{
                    bundleOf(CAPSULE_PROVIDER_RESULT to false)
                }
            }
            CapsuleProviderMethod.UNBIND_SERVICE -> {
                val binder = extras?.getBinder(CAPSULE_PROVIDER_EXTRA_CONNECTION)
                val hash = binder?.hashCode()
                return if(hash != null) {
                    unbindService(hash)
                    bundleOf(CAPSULE_PROVIDER_RESULT to true)
                }else{
                    bundleOf(CAPSULE_PROVIDER_RESULT to false)
                }
            }
            CapsuleProviderMethod.HAS_REQUIRED_PERMISSIONS -> {
                runWithClearedIdentity {
                    val result = provideContext().hasPermission(*getRequiredOneConnectPermissions())
                    bundleOf(CAPSULE_PROVIDER_RESULT to result)
                }
            }
            CapsuleProviderMethod.GET_LOCATION -> {
                val binder = extras?.getBinder(CAPSULE_PROVIDER_EXTRA_CONNECTION)
                val staleness = extras?.getString(CAPSULE_PROVIDER_EXTRA_STALENESS)?.let {
                    LocationStaleness.getOrNull(it)
                } ?: LocationStaleness.NONE
                val connection = binder?.let {
                    ILocationCallback.Stub.asInterface(it)
                }
                return if(connection != null) {
                    runWithClearedIdentity {
                        getLocation(connection, staleness)
                    }
                    bundleOf(CAPSULE_PROVIDER_RESULT to true)
                }else{
                    bundleOf(CAPSULE_PROVIDER_RESULT to false)
                }
            }
            CapsuleProviderMethod.IS_FOREGROUND -> {
                val isInForeground = provideContext().isInForeground() ?: false
                return bundleOf(CAPSULE_PROVIDER_RESULT to isInForeground)
            }
            CapsuleProviderMethod.GET_ENABLE_BLUETOOTH_INTENT -> {
                val pendingIntent = PendingIntent.getActivity(
                    provideContext(),
                    SCAN_ID_UTAG,
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
                return bundleOf(CAPSULE_PROVIDER_RESULT to pendingIntent)
            }
            CapsuleProviderMethod.ARE_HOOKS_SETUP -> {
                val sharedPreferences = provideContext()
                    .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                val version = provideContext().packageManager
                    .getPackageInfo(provideContext().packageName, 0).longVersionCode
                return bundleOf(
                    CAPSULE_PROVIDER_RESULT to sharedPreferences.areAllKeysPresent(version)
                )
            }
            else -> null
        }
    }

    private fun ContentProvider.bindService(
        intent: Intent,
        connection: IServiceConnection,
        hash: Int
    ) {
        var serviceConnection: ServiceConnection? = null
        serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                boundServices[hash] = this
                connection.runSafely {
                    onServiceConnected(service, name)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                boundServices.remove(hash)
                connection.runSafely {
                    onServiceDisconnected(name)
                }
                serviceConnection = null
            }
        }
        provideContext().bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    private fun ContentProvider.unbindService(hash: Int) {
        val serviceConnection = boundServices[hash] ?: return
        provideContext().unbindService(serviceConnection)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun ContentProvider.getLocation(
        callback: ILocationCallback,
        staleness: LocationStaleness
    ) = GlobalScope.launch {
        val result = withTimeoutOrNull(LOCATION_TIMEOUT) {
            provideContext().getLocation(staleness = staleness).first()
        }
        callback.runSafely {
            onResult(result)
        }
    }

    private fun hookViewMap() {
        XposedHelpers.findAndHookMethod(
            Instrumentation::class.java,
            "execStartActivity",
            Context::class.java,
            IBinder::class.java,
            IBinder::class.java,
            Activity::class.java,
            Intent::class.java,
            Integer.TYPE,
            Bundle::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val intent = param.args[4] as? Intent ?: return
                    //New intent format
                    if(intent.component == COMPONENT_FME) {
                        val targetId = intent.getStringExtra("EXTRA_KEY_EXTRA_DATA")?.let {
                            JSONObject(it).getString("targetDeviceId")
                        } ?: return
                        param.args[4] = TagActivity_createIntent(targetId)
                        return
                    }
                    //Old intent format
                    if(!intent.isFmeIntent()) return
                    val targetId = intent.data?.getQueryParameter("target_id") ?: return
                    val source = intent.data?.getQueryParameter("arguments")?.let {
                        val json = JSONObject(it)
                        when {
                            json.has("source") -> json.getString("source")
                            json.has("type") -> json.getString("type")
                            else -> null
                        }
                    } ?: return
                    param.args[4] = when(source) {
                        "tagplugin", "agreement" -> TagActivity_createIntent(targetId)
                        else -> UnsupportedIntentActivity_createIntent(source)
                    }
                }
            }
        )
    }

    private fun Intent.isFmeIntent(): Boolean {
        val data = data ?: return false
        if(data.scheme != "scapp") return false
        if(data.authority != "launch") return false
        if(data.getQueryParameter("action") != "service") return false
        return data.getQueryParameter("service_code") == "FME"
    }

    /**
     *  Hooks the initial attaching of Application context, then calls [handleLoadApplication] with
     *  it, to allow hooking methods which require a Context
     */
    private fun LoadPackageParam.hookContext() {
        var hasHookedContext = false
        XposedHelpers.findAndHookMethod(
            ContextWrapper::class.java,
            "attachBaseContext",
            Context::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    if(hasHookedContext) return
                    hasHookedContext = true
                    if(param.thisObject !is Application) return
                    val context = param.args[0] as Context
                    context.handleLoadApplication(this@hookContext)
                }
            }
        )
    }

    /**
     *  Uses Dexplore to find the SystemInfo class, and hook the method which does the
     *  manufacturer check. To save on searching, the method signature is committed to SmartThings'
     *  shared prefs for each version, and loaded if it exists already.
     */
    private fun LoadPackageParam.hookSystemInfo(context: Context) {
        val savedMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_SYSTEM_INFO_METHOD)
        val savedMethodAlt = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_SYSTEM_INFO_METHOD_ALT)
        val methods = if(savedMethod == null) {
            val dexFilter = DexFilter.Builder().build()
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool -> pool.contains("Nexus") }
                .build()
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool -> pool.contains("Nexus") }
                .setParamSize(0)
                .setModifiers(Modifier.PUBLIC)
                .build()
            val methods = dexplore.findMethods(dexFilter, classFilter, methodFilter, 2)
            methods.getOrNull(0)?.let {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_SYSTEM_INFO_METHOD, it)
            }
            methods.getOrNull(1)?.let {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_SYSTEM_INFO_METHOD_ALT, it)
            }
            methods
        }else{
            listOfNotNull(savedMethod, savedMethodAlt)
        }.mapNotNull { it.loadMethod(classLoader) }
        if(methods.isEmpty()) {
            context.logException("uTag: Failed to hook SystemInfo (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        methods.forEach {
            XposedBridge.hookMethod(
                it,
                object: XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        val callingClass = getCallingInformation()?.first ?: return
                        //Classes on the allowlist are forced to true to enable features without crashing
                        if (CALLING_PACKAGES_ALLOWLIST.any { callingClass.startsWith(it) }) {
                            param.result = true
                        }else{
                            //Allow use on OneUI by faking a non-Samsung device where not required
                            param.result = false
                        }
                    }
                }
            )
        }
    }

    /**
     *  Uses Dexplore to find the Logger class, then sets the two debug boolean global fields to
     *  true when [BuildConfig.DEBUG] is set
     */
    private fun LoadPackageParam.hookDebug(context: Context) {
        val savedClass = getSavedClass(SharedPrefsKey.SHARED_PREF_KEY_DEBUG_CLASS)
        val debugClass = if(savedClass == null) {
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool -> pool.contains("PRINT_SECURE_LOG : ") }
                .build()
            dexplore.findClass(classFilter)?.also {
                saveClass(SharedPrefsKey.SHARED_PREF_KEY_DEBUG_CLASS, it)
            }
        }else{
            savedClass
        }?.loadClass(classLoader) ?: run {
            context.logException("uTag: Failed to hook debug (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        if(BuildConfig.DEBUG) {
            debugClass.declaredFields.filter {
                it.type == Boolean::class.java
            }.forEach {
                it.set(null, true)
            }
        }
    }

    /**
     *  Uses Dexplore to find the QcService runnable class, and hook the method which checks for
     *  whether the service should be stopped, and neutralise it. We want to prevent ST from being
     *  killed as much as possible.
     */
    private fun LoadPackageParam.hookQcServiceRunnable(context: Context) {
        val savedMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_QCSERVICE_RUNNABLE_METHOD)
        val method = if(savedMethod == null) {
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains(" isSyncAllProceeding:")
                }
                .build()
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains(" isSyncAllProceeding:")
                }
                .setParamSize(0)
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_QCSERVICE_RUNNABLE_METHOD, it)
            }
        }else{
            savedMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to hook QcServiceRunnable (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        XposedBridge.hookMethod(
            method,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    param.result = false
                }
            }
        )
    }

    /**
     *  Hooks calls to disconnect Tags and redirects them to force disconect, to actually disconnect
     *  them. SmartThings usually rejects disconnect calls due to priority connections, which we
     *  don't use, so we'd rather they actually disconnect and redirecting them is the easiest
     *  way to do this.
     */
    private fun LoadPackageParam.hookCheckDisconnect(context: Context) {
        val savedMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_DISCONNECT_METHOD)
        val method = if(savedMethod == null) {
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("] | Characteristics: [")
                }
                .build()
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("Disconnect non priority connections.")
                }
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_DISCONNECT_METHOD, it)
            }
        }else{
            savedMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to hook disconnect method (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        val savedForceMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_FORCE_DISCONNECT_METHOD)
        val forceMethod = if(savedForceMethod == null) {
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("] | Characteristics: [")
                }
                .build()
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("forceDisconnect")
                }
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_FORCE_DISCONNECT_METHOD, it)
            }
        }else{
            savedForceMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to get force disconnect method (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        XposedBridge.hookMethod(
            method,
            object: XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    return forceMethod.invoke(param.thisObject, param.args[0])
                }
            }
        )
    }

    /**
     *  Uses Dexplore to find the GattActionManagerImpl.publishDeviceStatus method, which we
     *  use to intercept button presses as the service method does not work.
     */
    private fun LoadPackageParam.hookPublishDeviceStatus(context: Context) {
        val savedMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_PUBLISH_DEVICE_STATUS_METHOD)
        val method = if(savedMethod == null) {
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("] | Characteristics: [")
                }
                .build()
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("publishDeviceStatus") && pool.contains("id is null.")
                }
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_PUBLISH_DEVICE_STATUS_METHOD, it)
            }
        }else{
            savedMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to hook PublishDeviceStatus (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        XposedBridge.hookMethod(
            method,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val deviceId = param.args.firstNotNullOfOrNull { it as? String } ?: return
                    val characteristics = param.args.firstNotNullOfOrNull { it as? UUID } ?: return
                    val value = param.args[2] as ByteArray
                    val intent = Intent(ACTION_TAG_DEVICE_STATUS_CHANGED).apply {
                        applySecurity(context)
                        putExtra(EXTRA_DEVICE_ID, deviceId)
                        putExtra(EXTRA_CHARACTERISTICS, characteristics.toString())
                        putExtra(EXTRA_VALUE, value)
                        `package` = APPLICATION_ID
                    }
                    context.sendBroadcast(intent)
                }
            }
        )
    }

    /**
     *  Uses Dexplore to find the SmartTagGattConnecter.connect method, which we use to to
     *  intercept calls to connect to Tags and disable when Passive Mode is enabled
     */
    private fun LoadPackageParam.hookSmartTagGattConnecter(context: Context) {
        val savedMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_SMART_TAG_CONNECT_METHOD)
        val method = if(savedMethod == null) {
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains(" | uuidConnectionAvailable: ")
                }
                .build()
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains(" | uuidConnectionAvailable: ")
                }
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_SMART_TAG_CONNECT_METHOD, it)
            }
        }else{
            savedMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to hook PublishDeviceStatus (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        XposedBridge.hookMethod(
            method,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val deviceId = param.args.last() as String
                    if(UTagPassiveModeProvider_isInPassiveMode(context, deviceId)) {
                        //Cancel connection
                        param.result = Unit
                    }
                }
            }
        )
    }

    private fun LoadPackageParam.hookDeviceBleThingsManager(context: Context) {
        val savedNotifyMethod = getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_SCAN_NOTIFY_METHOD)
        val classFilter = ClassFilter.Builder()
            .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
            .setReferenceFilter { pool: ReferencePool ->
                pool.contains("updateDeviceBleThings:for in deviceTagConnectionCallbackHashMap")
            }
            .build()
        val notifyMethod = if(savedNotifyMethod == null) {
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("updateDeviceBleThings:for in deviceTagConnectionCallbackHashMap")
                }
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_SCAN_NOTIFY_METHOD, it)
            }
        }else{
            savedNotifyMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to hook DeviceBleThingsManager (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        val savedRepositoryMethod =
            getSavedMethod(SharedPrefsKey.SHARED_PREF_KEY_SCAN_REPOSITORY_METHOD)
        val repositoryMethod = if(savedRepositoryMethod == null) {
            val methodFilter = MethodFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool ->
                    pool.contains("smartTagRepository")
                }
                .setModifiers(Modifier.PUBLIC)
                .build()
            dexplore.findMethod(classFilter, methodFilter)?.also {
                saveMethod(SharedPrefsKey.SHARED_PREF_KEY_SCAN_REPOSITORY_METHOD, it)
            }
        }else{
            savedRepositoryMethod
        }?.loadMethod(classLoader) ?: run {
            context.logException("uTag: Failed to hook DeviceBleThingsManager (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        val smartTagCachedResource = XposedHelpers.findClass(
            "com.samsung.android.oneconnect.feature.blething.tag.repository.SmartTagCachedResource",
            classLoader
        )
        val lookupMethods = smartTagCachedResource.getAllInterfaces().firstNotNullOfOrNull { int ->
            int.declaredMethods.filter {
                it.returnType.methods.any { m -> m.name == "getServiceData" }
            }.takeIf { it.isNotEmpty() }
        }
        XposedBridge.hookMethod(
            notifyMethod,
            object: XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val tagInfo = param.args[0]
                    val deviceId = tagInfo.javaClass.fields.first().get(tagInfo) as String
                    val repository = repositoryMethod.invoke(param.thisObject)
                    val deviceInfo = lookupMethods?.firstNotNullOfOrNull {
                        it.invoke(repository, deviceId)
                    } ?: return
                    val serviceData = XposedHelpers.callMethod(
                        deviceInfo,
                        "getServiceData"
                    ) as? ByteArray ?: return
                    val bleMac = XposedHelpers.callMethod(
                        deviceInfo,
                        "getBleMac"
                    ) as? String ?: return
                    val rssi = XposedHelpers.callMethod(
                        deviceInfo,
                        "getRssi"
                    ) as? Int ?: return
                    val intent = Intent(ACTION_TAG_SCAN_RECEIVED).apply {
                        applySecurity(context)
                        `package` = APPLICATION_ID
                        putExtra(EXTRA_DEVICE_ID, deviceId)
                        putExtra(EXTRA_SERVICE_DATA, serviceData)
                        putExtra(EXTRA_BLE_MAC, bleMac)
                        putExtra(EXTRA_RSSI, rssi)
                    }
                    context.sendBroadcast(intent)
                }
            }
        )
    }

    /**
     *  Uses Dexplore to find the ScanCallback class, to send all scan results to uTag
     */
    private fun LoadPackageParam.hookScanCallback(context: Context) {
        val savedClass = getSavedClass(SharedPrefsKey.SHARED_PREF_KEY_SCAN_CALLBACK_CLASS)
        val scanCallbackClass = if(savedClass == null) {
            val classFilter = ClassFilter.Builder()
                .setReferenceTypes(ReferenceTypes.STRINGS_ONLY)
                .setReferenceFilter { pool: ReferencePool -> pool.contains("Ignore  device is null") }
                .build()
            dexplore.findClass(classFilter)?.also {
                saveClass(SharedPrefsKey.SHARED_PREF_KEY_SCAN_CALLBACK_CLASS, it)
            }
        }else{
            savedClass
        }?.loadClass(classLoader) ?: run {
            context.logException("uTag: Failed to hook ScanCallback (${packageInfo.versionName}, ${BuildConfig.XPOSED_CODE})")
            return
        }
        XposedHelpers.findAndHookMethod(
            scanCallbackClass,
            "onScanResult",
            Integer.TYPE,
            ScanResult::class.java,
            object: XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val scanResult = param.args[1] as? ScanResult ?: return
                    if(scanResult.device == null) return
                    val intent = Intent(ACTION_SCAN_RECEIVED).apply {
                        applySecurity(context)
                        putExtra(EXTRA_RESULT, scanResult)
                        `package` = PACKAGE_NAME_UTAG
                    }
                    context.sendBroadcast(intent)
                }
            }
        )
    }

    private fun Class<*>.getAllInterfaces(): List<Class<*>> {
        return interfaces.flatMap { it.getAllInterfaces().plus(it) }
    }

    private fun getSavedMethod(key: SharedPrefsKey): MethodData? {
        val serialisedMethod = sharedPreferences.getString(
            key.getKey(packageInfo.longVersionCode),
            null
        ) ?: return null
        return MethodData.deserialize(serialisedMethod)
    }

    private fun getSavedClass(key: SharedPrefsKey): ClassData? {
        val serialisedClass = sharedPreferences.getString(
            key.getKey(packageInfo.longVersionCode), null
        ) ?: return null
        return ClassData.deserialize(serialisedClass)
    }

    private fun saveMethod(key: SharedPrefsKey, methodData: MethodData) {
        val serialisedMethod = methodData.serialize()
        sharedPreferences.edit().putString(
            key.getKey(packageInfo.longVersionCode),
            serialisedMethod
        ).commit()
    }

    private fun saveClass(key: SharedPrefsKey, classData: ClassData) {
        val serialisedMethod = classData.serialize()
        sharedPreferences.edit().putString(
            key.getKey(packageInfo.longVersionCode),
            serialisedMethod
        ).commit()
    }

    private fun getCallingInformation(): Triple<String, String, List<String>>? {
        val classes = Thread.currentThread().stackTrace.map { Pair(it.className, it.methodName) }
        val lspIndex = classes.indexOfFirst { it.first == "LSPHooker_" }
        if(lspIndex == -1 || lspIndex == classes.size) return null
        val classList = classes.map { it.first }
        val result = classes[lspIndex + 1]
        return Triple(result.first, result.second, classList)
    }
    
    private fun Context.logException(title: String, throwable: Throwable? = null) {
        XposedBridge.log(title)
        XposedCrashReportProvider_reportNonFatal(this, XposedException(title, throwable))
    }

    private class XposedException(title: String, throwable: Throwable?): Throwable(title, throwable)

    private inner class UTagSmartThingsForegroundService(
        private val service: WeakReference<Service>
    ): IUTagSmartThingsForegroundService.Stub() {
        override fun ping(): Boolean {
            return true
        }

        override fun stop() {
            service.get()?.run {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        override fun stopProcess() {
            //Kills the :core process, forcing a restart without killing SmartThings entirely
            exitProcess(0)
        }
    }

}