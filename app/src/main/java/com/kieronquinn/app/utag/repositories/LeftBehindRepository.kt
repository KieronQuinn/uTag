package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.utag.providers.LeftBehindSmartspacerTargetProvider
import com.kieronquinn.app.utag.repositories.LeftBehindRepository.LeftBehindTag
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState
import com.kieronquinn.app.utag.utils.extensions.dp

interface LeftBehindRepository {

    /**
     *  Current Left Behind Tags
     */
    val leftBehindTags: Map<String, LeftBehindTag>

    /**
     *  Generates a map image for a Tag with [tagState], if a location is available.
     */
    suspend fun getLeftBehindMap(tagState: TagState.Loaded): Bitmap?

    /**
     *  Add a Left Behind Tag to the list and update the Smartspacer Target if required
     */
    fun addLeftBehindTag(leftBehindTag: LeftBehindTag)

    /**
     *  Dismiss a left behind Tag and update the Smartspacer Target if required
     */
    fun dismissLeftBehindTag(deviceId: String)

    data class LeftBehindTag(
        val deviceId: String,
        val title: String,
        val subtitle: String,
        val map: Bitmap?
    )

}

class LeftBehindRepositoryImpl(
    private val context: Context,
    private val staticMapRepository: StaticMapRepository,
    private val settingsRepository: SettingsRepository
): LeftBehindRepository {

    companion object {
        private const val MAX_ZOOM_LEVEL = 17f
        private const val FILENAME_PREFIX_NOTIFICATION = "notification"
    }

    private val imageWidth = 400.dp
    private val imageHeight = 150.dp

    override val leftBehindTags = HashMap<String, LeftBehindTag>()

    override suspend fun getLeftBehindMap(tagState: TagState.Loaded): Bitmap? {
        val location = tagState.getLocation() ?: return null
        return staticMapRepository.generateStaticMapBitmap(
            filename = "${FILENAME_PREFIX_NOTIFICATION}_${tagState.device.deviceId}",
            width = imageWidth,
            height = imageHeight,
            zoomLevel = MAX_ZOOM_LEVEL,
            style = settingsRepository.mapStyle.get(),
            theme = settingsRepository.mapTheme.get()
        ) {
            listOf(
                MarkerOptions()
                    .anchor(0.5f, 0.96f)
                    .icon(BitmapDescriptorFactory.fromBitmap(tagState.device.markerIcons.first))
                    .position(location.latLng)
            )
        }
    }

    override fun addLeftBehindTag(leftBehindTag: LeftBehindTag) {
        leftBehindTags[leftBehindTag.deviceId] = leftBehindTag
        SmartspacerTargetProvider.notifyChange(
            context,
            LeftBehindSmartspacerTargetProvider::class.java
        )
    }

    override fun dismissLeftBehindTag(deviceId: String) {
        if(leftBehindTags.remove(deviceId) != null) {
            SmartspacerTargetProvider.notifyChange(
                context,
                LeftBehindSmartspacerTargetProvider::class.java
            )
        }
    }

}