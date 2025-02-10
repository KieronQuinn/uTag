package com.kieronquinn.app.utag.repositories

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.location.Location
import android.os.Build
import androidx.core.os.bundleOf
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.repositories.SmartThingsRepository.ModuleState
import com.kieronquinn.app.utag.service.ILocationCallback
import com.kieronquinn.app.utag.service.IServiceConnection
import com.kieronquinn.app.utag.utils.extensions.callCapsuleProviderBooleanMethod
import com.kieronquinn.app.utag.utils.extensions.callCapsuleProviderIntMethod
import com.kieronquinn.app.utag.utils.extensions.callCapsuleProviderPendingIntentMethod
import com.kieronquinn.app.utag.utils.extensions.callCapsuleProviderStringMethod
import com.kieronquinn.app.utag.utils.extensions.wasSmartThingsInstalledByPlay
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CAPSULE_PROVIDER_EXTRA_CONNECTION
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CAPSULE_PROVIDER_EXTRA_INTENT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CAPSULE_PROVIDER_EXTRA_STALENESS
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CAPSULE_PROVIDER_EXTRA_START_FIRST
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CapsuleProviderMethod
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CapsuleProviderMethod.ARE_HOOKS_SETUP
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CapsuleProviderMethod.GET_ENABLE_BLUETOOTH_INTENT
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CapsuleProviderMethod.GET_VERSION_CODE
import com.kieronquinn.app.utag.xposed.Xposed.Companion.CapsuleProviderMethod.GET_VERSION_NAME
import com.kieronquinn.app.utag.xposed.extensions.getRequiredOneConnectPermissions
import com.kieronquinn.app.utag.xposed.extensions.isPackageInstalled
import com.kieronquinn.app.utag.xposed.extensions.isSmartThingsModded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.kieronquinn.app.utag.xposed.core.BuildConfig as XposedBuildConfig

interface SmartThingsRepository {

    /**
     *  Returns the installed SmartThings version name, or the fallback if it's not installed
     */
    val smartThingsVersion: String

    /**
     *  Returns a random UUID, which is randomised on each run, to be included in requests
     */
    val correlationId: String

    /**
     *  User Agent header to be included in SmartThings requests
     */
    val userAgent: String

    /**
     *  User Agent header to be included in OSP (auth) requests
     */
    val ospUserAgent: String

    /**
     *  Device model header to be included in SmartThings requests
     */
    val deviceModel: String

    /**
     *  OS version header to be included in SmartThings requests
     */
    val os: String

    /**
     *  Gets Intent to launch SmartThings main screen
     */
    fun getLaunchIntent(): Intent?

    /**
     *  Returns the state of the Xposed Module
     */
    suspend fun getModuleState(): ModuleState?

    /**
     *  Binds a remote service in SmartThings, bypassing permission and exported requirements
     */
    fun bindService(
        intent: Intent,
        connection: IServiceConnection,
        startFirst: Boolean = false
    ): Boolean

    /**
     *  Unbinds a previously bound remote service in SmartThings
     */
    suspend fun unbindService(connection: IServiceConnection): Boolean

    /**
     *  As [unbindService], but called on a non-restricted scope. Result will not be reported.
     */
    fun unbindServiceAsync(connection: IServiceConnection)

    /**
     *  Returns whether SmartThings has all the permissions required in
     *  [getRequiredOneConnectPermissions]
     */
    suspend fun hasRequiredPermissions(): Boolean

    /**
     *  Returns whether SmartThings is in the foreground (any activity)
     */
    suspend fun isForeground(): Boolean

    /**
     *  Returns the current location, got via SmartThings. Used to update Tag locations since the
     *  user may not have granted uTag direct location access.
     */
    suspend fun getLocation(): Location?

    /**
     *  Returns a [PendingIntent] which can be used to enable Bluetooth without uTag itself having
     *  to request Bluetooth permissions
     */
    suspend fun getEnableBluetoothIntent(): PendingIntent?

    /**
     *  Returns whether the Xposed module has its dynamic hooks set up
     */
    suspend fun areHooksSetup(): Boolean

    /**
     *  [isUTagBuild] indicates whether the build of SmartThings that's installed is signed with
     *  the uTag release key, ie. it's the prebuilt modified version.
     *
     *  If the user builds their own modded version, it's their responsibility to make sure it's
     *  updated as required, and it will show as [isUTagBuild] `false`.
     */
    sealed class ModuleState(
        open val isUTagBuild: Boolean,
        open val wasInstalledOnPlay: Boolean,
    ) {

        companion object {
            fun ModuleState?.isFatal() = this?.isFatal() ?: true
        }

        /**
         *  SmartThings is installed, but the module is not active
         */
        data class NotModded(
            override val isUTagBuild: Boolean,
            override val wasInstalledOnPlay: Boolean
        ): ModuleState(isUTagBuild, wasInstalledOnPlay) {
            override fun isFatal() = true
        }

        /**
         *  The module is installed and the version matches the current uTag version
         */
        data class Installed(
            override val isUTagBuild: Boolean,
            override val wasInstalledOnPlay: Boolean,
            val versionName: String,
            val versionCode: Int
        ): ModuleState(isUTagBuild, wasInstalledOnPlay)

        /**
         *  The module is installed, but the version is older. The user needs to force stop
         *  SmartThings (when rooted), or update the patched version (when not rooted).
         */
        data class Outdated(
            override val isUTagBuild: Boolean,
            override val wasInstalledOnPlay: Boolean,
            val versionName: String,
            val versionCode: Int
        ): ModuleState(isUTagBuild, wasInstalledOnPlay)

        /**
         *  The module is installed, but the version is newer. The user needs to update uTag.
         */
        data class Newer(
            override val isUTagBuild: Boolean,
            override val wasInstalledOnPlay: Boolean,
            val versionName: String,
            val versionCode: Int
        ): ModuleState(isUTagBuild, wasInstalledOnPlay)

        /**
         *  Whether UIs should be hidden if this error type is hit
         */
        protected open fun isFatal() = false
    }

}

class SmartThingsRepositoryImpl(
    private val context: Context,
    private val encryptedSettingsRepository: EncryptedSettingsRepository
): SmartThingsRepository {

    companion object {
        private const val FALLBACK_VERSION = "1.8.21.28"

        private fun getOSName(): String {
            return Build.VERSION_CODES::class.java.fields
                .getOrNull(Build.VERSION.SDK_INT + 1)?.name ?: Build.VERSION.RELEASE
        }
    }

    private val scope = MainScope()
    private val packageManager = context.packageManager

    override val smartThingsVersion = try {
        packageManager.getPackageInfo(PACKAGE_NAME_ONECONNECT, 0).versionName
            ?: FALLBACK_VERSION
    }catch (e: NameNotFoundException) {
        FALLBACK_VERSION
    }

    override val correlationId: String = UUID.randomUUID().toString()

    override val userAgent: String =
        "Android/OneApp/$smartThingsVersion/Main (${Build.MODEL}; Android ${getOSName()}/${Build.VERSION.RELEASE}) SmartKit/4.423.1"

    override val ospUserAgent: String =
        "Android/Oneapp/$smartThingsVersion (${Build.MODEL}; Android ${Build.VERSION.SDK_INT}/${Build.VERSION.RELEASE}) 4.11.0 QcApplication"

    override val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL} ${Build.DEVICE}"
    override val os = "Android ${Build.VERSION.RELEASE}"

    override fun getLaunchIntent(): Intent? {
        return packageManager.getLaunchIntentForPackage(PACKAGE_NAME_ONECONNECT)
    }

    override suspend fun getModuleState(): ModuleState? {
        return withContext(Dispatchers.IO) {
            //Abort if SmartThings isn't even installed
            if(!packageManager.isPackageInstalled(PACKAGE_NAME_ONECONNECT)) return@withContext null
            val versionCode = context.callCapsuleProviderIntMethod(GET_VERSION_CODE)
            val versionName = context.callCapsuleProviderStringMethod(GET_VERSION_NAME)
            //If the signature matches the SHA of the uTag release key, this is a premodded build
            val isUtagBuild = packageManager.isSmartThingsModded()
            val wasInstalledOnPlay = packageManager.wasSmartThingsInstalledByPlay()
            val requiredVersion = XposedBuildConfig.XPOSED_CODE
            when {
                versionCode == null || versionName == null -> {
                    ModuleState.NotModded(isUtagBuild, wasInstalledOnPlay)
                }
                versionCode == requiredVersion -> {
                    ModuleState.Installed(isUtagBuild, wasInstalledOnPlay, versionName, versionCode)
                }
                versionCode < requiredVersion -> {
                    ModuleState.Outdated(isUtagBuild, wasInstalledOnPlay, versionName, versionCode)
                }
                else -> ModuleState.Newer(isUtagBuild, wasInstalledOnPlay, versionName, versionCode)
            }
        }
    }

    override fun bindService(
        intent: Intent,
        connection: IServiceConnection,
        startFirst: Boolean
    ): Boolean {
        val extras = bundleOf(
            CAPSULE_PROVIDER_EXTRA_INTENT to intent,
            CAPSULE_PROVIDER_EXTRA_CONNECTION to connection.asBinder(),
            CAPSULE_PROVIDER_EXTRA_START_FIRST to startFirst
        )
        return context.callCapsuleProviderBooleanMethod(CapsuleProviderMethod.BIND_SERVICE, extras)
    }

    override suspend fun unbindService(connection: IServiceConnection): Boolean {
        return withContext(Dispatchers.IO) {
            val extras = bundleOf(
                CAPSULE_PROVIDER_EXTRA_CONNECTION to connection.asBinder()
            )
            context.callCapsuleProviderBooleanMethod(CapsuleProviderMethod.UNBIND_SERVICE, extras)
        }
    }

    override fun unbindServiceAsync(connection: IServiceConnection) {
        scope.launch {
            unbindService(connection)
        }
    }

    override suspend fun hasRequiredPermissions(): Boolean {
        return withContext(Dispatchers.IO) {
            context.callCapsuleProviderBooleanMethod(CapsuleProviderMethod.HAS_REQUIRED_PERMISSIONS)
        }
    }

    override suspend fun isForeground(): Boolean {
        return withContext(Dispatchers.IO) {
            context.callCapsuleProviderBooleanMethod(CapsuleProviderMethod.IS_FOREGROUND)
        }
    }

    override suspend fun getLocation(): Location? {
        val staleness = encryptedSettingsRepository.locationStaleness.get().name
        return suspendCoroutine {
            val callback = object: ILocationCallback.Stub() {
                override fun onResult(location: Location?) {
                    it.resume(location)
                }
            }
            val extras = bundleOf(
                CAPSULE_PROVIDER_EXTRA_CONNECTION to callback.asBinder(),
                CAPSULE_PROVIDER_EXTRA_STALENESS to staleness,
            )
            if(!context.callCapsuleProviderBooleanMethod(
                CapsuleProviderMethod.GET_LOCATION,
                extras
            )) {
                it.resume(null)
            }
        }
    }

    override suspend fun getEnableBluetoothIntent(): PendingIntent? {
        return withContext(Dispatchers.IO) {
            context.callCapsuleProviderPendingIntentMethod(GET_ENABLE_BLUETOOTH_INTENT)
        }
    }

    override suspend fun areHooksSetup(): Boolean {
        return withContext(Dispatchers.IO) {
            context.callCapsuleProviderBooleanMethod(ARE_HOOKS_SETUP)
        }
    }

}