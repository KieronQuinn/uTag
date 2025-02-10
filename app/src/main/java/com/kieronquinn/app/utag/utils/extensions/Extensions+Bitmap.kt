package com.kieronquinn.app.utag.utils.extensions

import android.graphics.Bitmap
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Compresses the bitmap to a byte array for serialization.
 */
fun Bitmap.compress(): ByteArray? {
    val out = ByteArrayOutputStream(getExpectedBitmapSize())
    return try {
        compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        out.toByteArray()
    } catch (e: IOException) {
        null
    }
}

/**
 * Try go guesstimate how much space the icon will take when serialized to avoid unnecessary
 * allocations/copies during the write (4 bytes per pixel).
 */
private fun Bitmap.getExpectedBitmapSize(): Int {
    return width * height * 4
}

fun Bitmap.scale(scale: Float): Bitmap {
    return scaleAndRecycle((width * scale).roundToInt(), (height * scale).roundToInt())
}

fun Bitmap.scaleAndRecycle(width: Int, height: Int): Bitmap {
    if(getWidth() == width && getHeight() == height) return this
    return scale(width, height)
}