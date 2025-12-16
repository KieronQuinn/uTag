package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.bumptech.glide.Glide
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.DeviceInfo
import com.kieronquinn.app.utag.model.database.cache.CacheItem.CacheType
import com.kieronquinn.app.utag.networking.model.smartthings.GetDeviceResponse
import com.kieronquinn.app.utag.networking.services.DeviceService
import com.kieronquinn.app.utag.networking.services.DeviceService.Companion.GET_DEVICES_URL
import com.kieronquinn.app.utag.repositories.CacheRepository.Companion.getCache
import com.kieronquinn.app.utag.utils.extensions.dip
import com.kieronquinn.app.utag.utils.extensions.get
import com.kieronquinn.app.utag.utils.extensions.scaleAndRecycle
import com.kieronquinn.app.utag.utils.extensions.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.Retrofit

interface DeviceRepository {

    suspend fun getDeviceIds(): List<String>?
    suspend fun getDeviceInfo(deviceId: String): DeviceInfo?
    suspend fun getDevices(includeNotOwned: Boolean = false): List<DeviceInfo>?

}

class DeviceRepositoryImpl(
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    private val cacheRepository: CacheRepository,
    context: Context,
    retrofit: Retrofit
): DeviceRepository, KoinComponent {

    companion object {
        private const val DEVICE_ICON_SIZE_IN_TEMPLATE = 131 //131px
        private const val DEVICE_ICON_OFFSET_IN_TEMPLATE_X = 22f //22px
        private const val DEVICE_ICON_OFFSET_IN_TEMPLATE_Y = 19f //19px
        private const val TEMPLATE_SCALED_WIDTH = 56 //dp
        private const val TEMPLATE_SCALED_HEIGHT = 66 //dp
        private const val DEVICE_TYPE_TAG = "x.com.st.d.tag"
    }

    private val deviceService = DeviceService.createService(context, retrofit)
    private val smartTagRepository by inject<SmartTagRepository>()
    private val resources = context.resources
    private val glide = Glide.with(context)
    private val bitmapCache = HashMap<BitmapCacheEntry, Pair<Bitmap, Bitmap>>()

    private val okHttpClient = OkHttpClient().newBuilder().apply {
        followRedirects(true)
    }.build()

    private suspend fun getTagDevices(includeNotOwned: Boolean = false): List<GetDeviceResponse>? {
        val allDevices = ArrayList<GetDeviceResponse>()
        var url: String? = GET_DEVICES_URL
        while(url != null) {
            val devices = deviceService.getDevices(url).get(name = "devices")
                ?: return cacheRepository.getCache(CacheType.DEVICES)
            allDevices.addAll(devices.items)
            url = devices.links.next?.href
        }
        cacheRepository.setCache(CacheType.DEVICES, data = allDevices)
        return allDevices.filter {
            it.ocfDeviceType == DEVICE_TYPE_TAG && (it.isAllowed() || includeNotOwned)
        }
    }

    override suspend fun getDeviceIds(): List<String>? {
        return getTagDevices()?.also {
            cacheKnownTags(it)
        }?.map { it.deviceId }
    }

    override suspend fun getDeviceInfo(deviceId: String): DeviceInfo? {
        val userId = encryptedSettingsRepository.userId.getOrNull() ?: return null
        return deviceService.getDevice(deviceId).get(CacheType.DEVICE_INFO, deviceId, name = "info")
            ?.toDeviceInfo(userId)
    }

    override suspend fun getDevices(includeNotOwned: Boolean): List<DeviceInfo>? {
        val userId = encryptedSettingsRepository.userId.getOrNull() ?: return null
        return getTagDevices(includeNotOwned)?.map {
            it.toDeviceInfo(userId)
        }
    }

    private suspend fun GetDeviceResponse.toDeviceInfo(userId: String): DeviceInfo {
        val isOwner = bleD2D.metadata.onboardedBy.saGuid == userId
        val hasUwb = components.any { components ->
            components.capabilities.any { capability ->
                capability.id == "tag.uwbActivation" && capability.version != null
            }
        }
        val iconUrl = getFinalUrl(icons.colouredIcon)
        val disconnectedIconUrl = getFinalUrl(icons.disconnectedIcon)
        val shareable = if(isOwner) {
            bleD2D.metadata.shareable.enabled
        }else{
            bleD2D.metadata.shareable.enabled && bleD2D.metadata.shareable.members?.any {
                it.saGuid == userId
            } == true
        }
        return DeviceInfo(
            ownerId,
            deviceId,
            name,
            label,
            iconUrl,
            bleD2D.metadata.vendor.mnId,
            bleD2D.metadata.vendor.setupId,
            bleD2D.metadata.vendor.modelName,
            disconnectedIconUrl,
            iconUrl.loadMarkerIcons(),
            isOwner,
            bleD2D.metadata.battery?.level,
            (bleD2D.metadata.battery?.updated)?.let { updated -> updated * 1000L },
            shareable,
            bleD2D.metadata.shareable.enabled,
            bleD2D.metadata.searchingStatus,
            bleD2D.metadata.e2eEncryption.enabled,
            hasUwb,
            bleD2D.metadata.petWalking.enabled,
            bleD2D.metadata.remoteRing.enabled
        )
    }

    private suspend fun cacheKnownTags(devices: List<GetDeviceResponse>) {
        val ids = devices.associate { Pair(it.deviceId, it.label) }
        smartTagRepository.cacheKnownTags(ids)
    }

    private suspend fun String.loadMarkerIcons(): Pair<Bitmap, Bitmap> {
        val cacheEntry = BitmapCacheEntry(
            this,
            DEVICE_ICON_SIZE_IN_TEMPLATE,
            DEVICE_ICON_SIZE_IN_TEMPLATE
        )
        bitmapCache[cacheEntry]?.let { return it }
        val deviceIcon = withContext(Dispatchers.IO) {
            try {
                glide.asBitmap().url(this@loadMarkerIcons)
                    .submit(DEVICE_ICON_SIZE_IN_TEMPLATE, DEVICE_ICON_SIZE_IN_TEMPLATE).get()
            }catch (e: Exception) {
                null
            }
        }
        return Pair(
            resources.applyTemplate(deviceIcon, R.drawable.ic_marker_selected),
            resources.applyTemplate(deviceIcon, R.drawable.ic_marker_unselected)
        ).also {
            bitmapCache[cacheEntry] = it
        }
    }

    private fun Resources.applyTemplate(deviceIcon: Bitmap?, templateRes: Int): Bitmap {
        val template = BitmapFactory.decodeResource(resources, templateRes)
        val out = if(deviceIcon != null) {
            val marker = Bitmap.createBitmap(template.width, template.height, template.config!!)
            val canvas = Canvas(marker)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(template, 0f, 0f, paint)
            canvas.drawBitmap(
                deviceIcon,
                DEVICE_ICON_OFFSET_IN_TEMPLATE_X,
                DEVICE_ICON_OFFSET_IN_TEMPLATE_Y,
                paint
            )
            template.recycle()
            marker
        }else template
        return out.scaleAndRecycle(dip(TEMPLATE_SCALED_WIDTH), dip(TEMPLATE_SCALED_HEIGHT))
    }

    /**
     *  Loads the final icon URL and switches to the higher resolution image where available
     */
    private suspend fun getFinalUrl(redirectingUrl: String): String {
        return withContext(Dispatchers.IO) {
            //Always use the cache when available here to save on network requests
            cacheRepository.getCache<String>(CacheType.IMAGE_REDIRECT, redirectingUrl)?.let {
                return@withContext it
            }
            try {
                val response = okHttpClient.newCall(
                    Request.Builder()
                        .addHeader("Accept", "*/*")
                        .url(redirectingUrl)
                        .build()
                ).execute()
                response.request.url.toString()
                    .replace("@1x.png", "@4x.png").also {
                        cacheRepository.setCache(CacheType.IMAGE_REDIRECT, redirectingUrl, it)
                    }
            }catch (e: Exception) {
                redirectingUrl
            }
        }
    }

    private suspend fun GetDeviceResponse.isAllowed(): Boolean {
        val userId = encryptedSettingsRepository.userId.getOrNull() ?: return false
        val isOwner = bleD2D.metadata.onboardedBy.saGuid == userId
        val shareable = bleD2D.metadata.shareable.enabled
        return isOwner || shareable
    }

    private data class BitmapCacheEntry(
        val url: String,
        val width: Int,
        val height: Int
    )

}