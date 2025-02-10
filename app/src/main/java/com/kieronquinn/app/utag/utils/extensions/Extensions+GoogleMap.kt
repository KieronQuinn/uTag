package com.kieronquinn.app.utag.utils.extensions

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.kieronquinn.app.utag.utils.maps.LatLngInterpolator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

/**
 *  Generates a Bitmap of a Google Map of a given [width]x[height], and with the given [markers].
 *  The camera will automatically be set to show all the markers, at n-1 zoom level for sufficient
 *  padding.
 *
 *  The map can be modified with [modifier]
 */
suspend fun Context.generateGoogleMap(
    width: Int,
    height: Int,
    padding: Rect,
    maxZoomLevel: Float? = null,
    zoomOut: Boolean = false,
    moveWatermark: Boolean = false,
    modifier: suspend GoogleMap.() -> Unit = {},
    markers: suspend GoogleMap.() -> List<MarkerOptions>
): Bitmap? = withTimeout(5000L) {
    withContext(Dispatchers.Main) {
        val options = GoogleMapOptions().liteMode(true)
        val mapView = MapView(this@generateGoogleMap, options)
        mapView.onCreate(null)
        mapView.onResume()
        mapView.getMap().run {
            setPadding(padding.left, padding.top, padding.right, padding.bottom)
            mapView.layoutParams = ViewGroup.LayoutParams(width, height)
            if(moveWatermark) {
                val watermark = mapView.findViewWithTag<View>("GoogleWatermark")
                watermark.updateLayoutParams<RelativeLayout.LayoutParams> {
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
                    addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
                    addRule(RelativeLayout.ALIGN_PARENT_START, 0)
                    addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                    addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                }
            }
            mapView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
            mapView.layout(0, 0, width, height)
            modifier(this)
            val addedMarkers = markers(this)
            val bounds = LatLngBounds.builder()
            addedMarkers.forEach {
                addMarker(it)
                bounds.include(it.position)
            }
            moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 0))
            if(maxZoomLevel != null && cameraPosition.zoom > maxZoomLevel) {
                moveCamera(CameraUpdateFactory.zoomTo(maxZoomLevel))
            }
            if(zoomOut) {
                moveCamera(CameraUpdateFactory.zoomOut())
            }
            awaitReady()
            generateSnapshot().also {
                clear()
            }
        }.also {
            mapView.removeAllViews()
            mapView.onDestroy()
        }
    }
}

private suspend fun MapView.getMap() = suspendCancellableCoroutine {
    getMapAsync { map ->
        it.resume(map)
    }
}

private suspend fun GoogleMap.awaitReady() = suspendCancellableCoroutine {
    setOnMapLoadedCallback {
        it.resume(Unit)
    }
}

private suspend fun GoogleMap.generateSnapshot() = suspendCancellableCoroutine {
    snapshot { bitmap ->
        it.resume(bitmap)
    }
}

fun SupportMapFragment.getMap() = callbackFlow {
    getMapAsync {
        trySend(it)
    }
    awaitClose {
        //No-op
    }
}

fun GoogleMap.onMapStartMove() = callbackFlow {
    setOnCameraMoveStartedListener { reason ->
        if(reason == OnCameraMoveStartedListener.REASON_GESTURE) {
            trySend(Unit)
        }
    }
    awaitClose {
        //No-op
    }
}

@SuppressLint("PotentialBehaviorOverride")
fun GoogleMap.onMarkerDragged() = callbackFlow {
    setOnMarkerDragListener(object: GoogleMap.OnMarkerDragListener {
        override fun onMarkerDrag(marker: Marker) {
            trySend(marker.position)
        }

        override fun onMarkerDragEnd(p0: Marker) {
            //No-op
        }

        override fun onMarkerDragStart(p0: Marker) {
            //No-op
        }
    })
    awaitClose {
        //No-op
    }
}

@SuppressLint("PotentialBehaviorOverride")
fun GoogleMap.onMapTapped() = callbackFlow {
    setOnMapClickListener {
        trySend(it)
    }
    awaitClose {
        //No-op
    }
}

/**
 *  Extension for [Marker.getTag] / [Marker.setTag] which allows multiple objects by setting up
 *  and enforcing a [HashMap] based map for objects with String keys. This MUST only be used in all
 *  instances for a given [Marker], mixing with regular [Marker.setTag] calls will overwrite it.
 */
val Marker.tagMap
    get() = (tag as? HashMap<String, Any?>) ?: run {
        return@run HashMap<String, Any?>().also {
            tag = it
        }
    }

/**
 *  Extension for [Circle.getTag] / [Circle.setTag] which allows multiple objects by setting up
 *  and enforcing a [HashMap] based map for objects with String keys. This MUST only be used in all
 *  instances for a given [Circle], mixing with regular [Circle.setTag] calls will overwrite it.
 */
val Circle.tagMap
    get() = (tag as? HashMap<String, Any?>) ?: run {
        return@run HashMap<String, Any?>().also {
            tag = it
        }
    }

private const val TAG_KEY_ANIMATION = "animation"
private const val TAG_KEY_ANIMATION_RADIUS = "animation_radius"

/**
 *  Animates a [Marker] to a given position. This uses [tagMap] internally, so has the same
 *  restrictions around tag usage.
 */
fun Marker.animateTo(
    position: LatLng,
    interpolator: LatLngInterpolator = LatLngInterpolator.Spherical
) {
    val currentAnimation = tagMap[TAG_KEY_ANIMATION] as? ValueAnimator
    currentAnimation?.cancel()
    val startPosition = this.position
    tagMap[TAG_KEY_ANIMATION] = ValueAnimator().apply {
        addUpdateListener {
            val progress = it.animatedFraction
            this@animateTo.position = interpolator.interpolate(progress, startPosition, position)
            if(progress == 1f) {
                //Animation has finished, remove it from the tag cache
                tagMap[TAG_KEY_ANIMATION] = null
            }
        }
        setFloatValues(0f, 1f)
        duration = Resources.getSystem().getInteger(android.R.integer.config_longAnimTime).toLong()
        start()
    }
}

/**
 *  Animates a [Circle] to a given position. This uses [tagMap] internally, so has the same
 *  restrictions around tag usage.
 */
fun Circle.animateTo(
    position: LatLng,
    interpolator: LatLngInterpolator = LatLngInterpolator.Spherical
) {
    val currentAnimation = tagMap[TAG_KEY_ANIMATION] as? ValueAnimator
    currentAnimation?.cancel()
    val startPosition = this.center
    tagMap[TAG_KEY_ANIMATION] = ValueAnimator().apply {
        addUpdateListener {
            val progress = it.animatedFraction
            this@animateTo.center = interpolator.interpolate(progress, startPosition, position)
            if(progress == 1f) {
                //Animation has finished, remove it from the tag cache
                tagMap[TAG_KEY_ANIMATION] = null
            }
        }
        setFloatValues(0f, 1f)
        duration = Resources.getSystem().getInteger(android.R.integer.config_longAnimTime).toLong()
        start()
    }
}

/**
 *  Animates a [Circle] to a given radius. This uses [tagMap] internally, so has the same
 *  restrictions around tag usage.
 */
fun Circle.animateTo(radius: Double) {
    val currentAnimation = tagMap[TAG_KEY_ANIMATION_RADIUS] as? ValueAnimator
    currentAnimation?.cancel()
    val startRadius = this.radius
    val diff = radius - startRadius
    tagMap[TAG_KEY_ANIMATION_RADIUS] = ValueAnimator().apply {
        addUpdateListener {
            val progress = it.animatedFraction
            this@animateTo.radius = startRadius + (diff * progress)
            if(progress == 1f) {
                //Animation has finished, remove it from the tag cache
                tagMap[TAG_KEY_ANIMATION_RADIUS] = null
            }
        }
        setFloatValues(0f, 1f)
        duration = Resources.getSystem().getInteger(android.R.integer.config_longAnimTime).toLong()
        start()
    }
}