package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle
import com.google.android.gms.maps.model.MarkerOptions
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.EncryptedValueConverter.ENCRYPTION_TRANSFORMATION
import com.kieronquinn.app.utag.providers.EncryptedFileProvider
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.utils.extensions.compress
import com.kieronquinn.app.utag.utils.extensions.generateGoogleMap
import com.kieronquinn.app.utag.utils.extensions.isDarkMode
import java.io.File
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

interface StaticMapRepository {

    /**
     *  Generates a static map image with the given size and options, writes it to an encrypted file
     *  and returns a URI for [EncryptedFileProvider] to open the file
     */
    suspend fun generateStaticMapBitmap(
        filename: String,
        width: Int,
        height: Int,
        zoomLevel: Float? = null,
        zoomOut: Boolean = false,
        style: MapStyle? = null,
        theme: MapTheme? = null,
        moveWatermark: Boolean = false,
        markers: GoogleMap.() -> List<MarkerOptions>
    ): Bitmap?

    /**
     *  As [generateStaticMapBitmap] but writes to an encrypted file and returns the URI to open it
     */
    suspend fun generateStaticMapUri(
        filename: String,
        width: Int,
        height: Int,
        zoomLevel: Float? = null,
        zoomOut: Boolean = false,
        style: MapStyle? = null,
        theme: MapTheme? = null,
        moveWatermark: Boolean = false,
        markers: GoogleMap.() -> List<MarkerOptions>
    ): Uri?

    /**
     *  Deletes a previously created encrypted file. Required when a notification displaying the
     *  map is dismissed, or the widget is removed.
     */
    fun clearFile(filename: String)

    /**
     *  Deletes ALL encrypted files, for when encryption has failed so the keys will no longer be
     *  valid
     */
    fun clearAllFiles()

}

class StaticMapRepositoryImpl(
    private val context: Context,
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    settingsRepository: SettingsRepository
): StaticMapRepository {

    private val encryptedImagesDirectory = File(context.filesDir, "images").apply {
        mkdirs()
    }

    private val mapStyle = settingsRepository.mapStyle
    private val mapTheme = settingsRepository.mapTheme

    private val encryptionKey: SecretKey
        get() = encryptedSettingsRepository.getDatabaseEncryptionKey()

    private val encryptionIv: IvParameterSpec
        get() = encryptedSettingsRepository.getDatabaseEncryptionIV()

    private val darkMapTheme by lazy {
        loadRawResourceStyle(context, R.raw.mapstyle_dark)
    }

    private val mapPadding by lazy {
        val padding = context.resources.getDimensionPixelSize(R.dimen.margin_8)
        Rect(padding, padding, padding, padding)
    }

    override suspend fun generateStaticMapBitmap(
        filename: String,
        width: Int,
        height: Int,
        zoomLevel: Float?,
        zoomOut: Boolean,
        style: MapStyle?,
        theme: MapTheme?,
        moveWatermark: Boolean,
        markers: GoogleMap.() -> List<MarkerOptions>
    ): Bitmap? {
        val modifier: suspend GoogleMap.() -> Unit = {
            val mapStyle = style ?: mapStyle.get()
            val mapTheme = theme ?: mapTheme.get()
            mapType = mapStyle.style
            applyTheme(mapTheme, mapStyle)
        }
        return context.generateGoogleMap(
            width = width,
            height = height,
            padding = mapPadding,
            maxZoomLevel = zoomLevel,
            zoomOut = zoomOut,
            modifier = modifier,
            markers = markers,
            moveWatermark = moveWatermark
        )
    }

    override suspend fun generateStaticMapUri(
        filename: String,
        width: Int,
        height: Int,
        zoomLevel: Float?,
        zoomOut: Boolean,
        style: MapStyle?,
        theme: MapTheme?,
        moveWatermark: Boolean,
        markers: GoogleMap.() -> List<MarkerOptions>
    ): Uri? {
        val map = generateStaticMapBitmap(
            filename,
            width,
            height,
            zoomLevel,
            zoomOut,
            style,
            theme,
            moveWatermark,
            markers
        ) ?: return null
        //Encrypt the map image and write it to file
        val bytes = map.compress()?.encrypt() ?: run {
            map.recycle()
            return null
        }
        val file = File(encryptedImagesDirectory, "$filename.enc")
        file.writeBytes(bytes)
        map.recycle()
        //Return a URI for the encrypted file provider, which will decrypt the file as required
        return EncryptedFileProvider.createUri(file)
    }

    override fun clearFile(filename: String) {
        File(encryptedImagesDirectory, "$filename.enc").delete()
    }

    override fun clearAllFiles() {
        encryptedImagesDirectory.listFiles()?.forEach {
            it.delete()
        }
    }

    private fun ByteArray.encrypt(): ByteArray? {
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, encryptionKey, encryptionIv)
        }
        return try {
            cipher.doFinal(this)
        }catch (e: Exception) {
            null
        }
    }

    private fun GoogleMap.applyTheme(theme: MapTheme, style: MapStyle) {
        if(style != MapStyle.NORMAL) return
        val themeToApply = when(theme) {
            MapTheme.SYSTEM -> {
                if(context.isDarkMode) {
                    MapTheme.DARK
                }else{
                    MapTheme.LIGHT
                }
            }
            else -> theme
        }
        setMapStyle(if(themeToApply == MapTheme.DARK) darkMapTheme else null)
    }

}