package com.kieronquinn.app.utag.ui.screens.safearea.location

import android.app.Dialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.SphericalUtil
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentSafeAreaLocationBinding
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.LockCollapsed
import com.kieronquinn.app.utag.ui.base.ProvidesBack
import com.kieronquinn.app.utag.ui.base.ProvidesOverflow
import com.kieronquinn.app.utag.ui.screens.safearea.location.SafeAreaLocationViewModel.State
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.animateTo
import com.kieronquinn.app.utag.utils.extensions.dip
import com.kieronquinn.app.utag.utils.extensions.getMap
import com.kieronquinn.app.utag.utils.extensions.isDarkMode
import com.kieronquinn.app.utag.utils.extensions.isLandscape
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.onMapStartMove
import com.kieronquinn.app.utag.utils.extensions.onMapTapped
import com.kieronquinn.app.utag.utils.extensions.onMarkerDragged
import com.kieronquinn.app.utag.utils.extensions.scaleAndRecycle
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.extensions.widthWithMargins
import dev.oneuiproject.oneui.widget.Toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.lang.ref.WeakReference
import kotlin.math.sqrt

class SafeAreaLocationFragment: BoundFragment<FragmentSafeAreaLocationBinding>(FragmentSafeAreaLocationBinding::inflate), BackAvailable, ProvidesOverflow, ProvidesBack, LockCollapsed {

    companion object {
        internal fun getParentViewModel(fragment: Fragment): SafeAreaLocationViewModel {
            return (fragment as SafeAreaLocationFragment).viewModel
        }
    }

    private val args by navArgs<SafeAreaLocationFragmentArgs>()
    private var myLocationMarker: Marker? = null
    private var hasCameraInitiallyMoved = false
    private var mapCameraHash: Int? = null
    private var mapMarker: Marker? = null
    private var mapCircle: Circle? = null
    private var mapMarkerHash: Int? = null
    private var mapCircleHash: Int? = null
    private var appliedMapTheme: MapTheme? = null
    private var appliedMapStyle: MapStyle? = null
    private var map: WeakReference<GoogleMap>? = null

    private val viewModel by viewModel<SafeAreaLocationViewModel> {
        parametersOf(args.isSettings, args.addingDeviceId, args.currentId)
    }

    private val darkMapTheme by lazy {
        loadRawResourceStyle(requireContext(), R.raw.mapstyle_dark)
    }

    private val selectedMarker by lazy {
        val pin = BitmapFactory.decodeResource(resources, R.drawable.ic_marker)
        BitmapDescriptorFactory.fromBitmap(
            pin.scaleAndRecycle(resources.dip(40), resources.dip(44))
        )
    }

    private val mapFragment
        get() = childFragmentManager.findFragmentById(R.id.safe_area_location_map) as SupportMapFragment

    private val circleStroke by lazy {
        ContextCompat.getColor(requireContext(), R.color.oui_accent_color)
    }

    private val circleFill by lazy {
        Color.argb(64, circleStroke.red, circleStroke.green, circleStroke.blue)
    }

    private val circleStrokeWidth by lazy {
        resources.dip(4).toFloat()
    }

    private val margin by lazy {
        resources.getDimensionPixelSize(R.dimen.margin_16)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupInsets()
        setupCard()
        setupCenter()
        setupState()
        if(!viewModel.hasShownToast) {
            viewModel.hasShownToast = true
            Toast.makeText(
                requireContext(), R.string.safe_area_location_name_toast, Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        mapMarker = null
        mapCircle = null
        myLocationMarker = null
        map?.get()?.clear()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_safe_area, menu)
        menu.findItem(R.id.safe_area_delete).isVisible = args.currentId.isNotEmpty()
        menu.findItem(R.id.safe_area_padding).isVisible = args.currentId.isEmpty()
        menu.findItem(R.id.safe_area_save).actionView
            ?.findViewById<Button>(R.id.action_layout_save_button)
            ?.setOnClickListener { viewModel.onSaveClicked() }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.safe_area_delete -> showDeleteDialog()
        }
        return true
    }

    override fun onBackPressed(): Boolean {
        return if(viewModel.hasChanges()) {
            showSaveDialog()
            true
        }else false
    }

    private fun setupMap() = whenResumed {
        with(mapFragment.getMap().first()) {
            map = WeakReference(this)
            isBuildingsEnabled = false
            uiSettings.isMapToolbarEnabled = false
            launch {
                onMapStartMove().collect {
                    viewModel.onMapMoved()
                }
            }
            launch {
                onMarkerDragged().collect {
                    viewModel.onCenterChanged(it, false)
                }
            }
            launch {
                onMapTapped().collect {
                    viewModel.onCenterChanged(it, true)
                }
            }
        }
    }

    private fun setupInsets() = with(binding) {
        root.onApplyInsets { _, insets ->
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            viewModel.onInsetsChanged(systemInsets)
            if(requireContext().isLandscape()) {
                safeAreaLocationCard.root.updateLayoutParams<MarginLayoutParams> {
                    updateMargins(bottom = systemInsets.bottom)
                }
            }
        }
    }

    private fun setupCard() = with(binding.safeAreaLocationCard) {
        root.clipToOutline = true
    }

    private fun setupCenter() = with(binding.safeAreaCenter) {
        whenResumed {
            onClicked {
                viewModel.onCenterClicked()
            }
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) = with(binding) {
        when(state) {
            is State.Loading -> safeAreaLocationMapFlipper.setDisplayedChildIfNeeded(1)
            is State.Loaded -> {
                safeAreaLocationMapFlipper.setDisplayedChildIfNeeded(0)
                handleMap(state)
            }
            is State.NoLocation -> showErrorDialog()
        }
    }

    private fun handleMap(state: State.Loaded) = whenResumed {
        with(mapFragment.getMap().first()) {
            applyMapPadding(state.insets ?: Insets.NONE)
            val markerHash = state.center.hashCode()
            if(mapMarkerHash != markerHash) {
                mapMarkerHash = markerHash
                mapMarker = mapMarker?.also {
                    if(state.mapOptions?.shouldAnimateMap == true) {
                        it.animateTo(state.center)
                    }else {
                        it.position = state.center
                    }
                } ?: addMarker(
                    MarkerOptions()
                        .position(state.center)
                        .anchor(0.5f, 0.93f)
                        .zIndex(Float.MAX_VALUE)
                        .draggable(true)
                        .icon(selectedMarker)
                )
            }
            val circleHash = arrayOf(state.radius, state.center).hashCode()
            if(mapCircleHash != circleHash) {
                mapCircleHash = circleHash
                mapCircle = mapCircle?.also {
                    if(state.mapOptions?.shouldAnimateMap == true) {
                        it.animateTo(state.center)
                        it.animateTo(state.radius.toDouble())
                    }else{
                        it.center = state.center
                        it.radius = state.radius.toDouble()
                    }
                } ?: addCircle(
                    CircleOptions()
                        .center(state.center)
                        .zIndex(Float.MIN_VALUE)
                        .strokeColor(circleStroke)
                        .fillColor(circleFill)
                        .strokeWidth(circleStrokeWidth)
                )
            }
            handleMyLocation(state)
            if(state.mapOptions != null) {
                applyStyle(state.mapOptions.style)
                applyTheme(state.mapOptions.theme, state.mapOptions.style)
            }
            if(state.mapOptions?.shouldMoveMap == true) {
                val cameraHash = arrayOf(state.radius, state.center).hashCode()
                if(mapCameraHash != cameraHash) {
                    val bounds = state.center.calculateLatLngBounds(state.radius.toDouble())
                    mapCameraHash = cameraHash
                    val update = CameraUpdateFactory.newLatLngBounds(bounds, margin * 4)
                    if (!hasCameraInitiallyMoved) {
                        hasCameraInitiallyMoved = true
                        moveCamera(update)
                    } else {
                        animateCamera(update)
                    }
                }
            }
        }
    }

    private fun LatLng.calculateLatLngBounds(radius: Double): LatLngBounds {
        val northEast =
            SphericalUtil.computeOffset(this, radius * sqrt(2f), 45.0)
        val southWest =
            SphericalUtil.computeOffset(this, radius * sqrt(2f), 225.0)
        return LatLngBounds.builder()
            .include(northEast)
            .include(southWest)
            .build()
    }

    private fun GoogleMap.handleMyLocation(state: State.Loaded) {
        val location = state.mapOptions?.location
        if(location != null) {
            val latLng = LatLng(location.latitude, location.longitude)
            myLocationMarker?.animateTo(latLng) ?: run {
                val icon = BitmapDescriptorFactory
                    .fromResource(R.drawable.ic_marker_location)
                myLocationMarker = addMarker(
                    MarkerOptions()
                        .icon(icon)
                        .anchor(0.5f, 0.5f)
                        .position(latLng)
                ).also {
                    it?.zIndex = 0f
                }
            }
        }else{
            myLocationMarker?.remove()
            myLocationMarker = null
        }
    }

    @Synchronized
    private fun GoogleMap.applyTheme(theme: MapTheme, style: MapStyle) {
        if(style != MapStyle.NORMAL) return
        val themeToApply = when(theme) {
            MapTheme.SYSTEM -> {
                if(requireContext().isDarkMode) {
                    MapTheme.DARK
                }else{
                    MapTheme.LIGHT
                }
            }
            else -> theme
        }
        if(themeToApply == appliedMapTheme) return
        setMapStyle(if(themeToApply == MapTheme.DARK) darkMapTheme else null)
        appliedMapTheme = themeToApply
    }

    @Synchronized
    private fun GoogleMap.applyStyle(style: MapStyle) {
        if(style == appliedMapStyle) return
        mapType = style.style
        appliedMapStyle = style
    }

    private fun GoogleMap.applyMapPadding(insets: Insets) = with(binding) {
        if(requireContext().isLandscape()) {
            //Map is to the right of the card
            val leftInset = safeAreaLocationCard.root.widthWithMargins
            setPadding(leftInset, 0, 0, insets.bottom)
        }else{
            //Map is above the card
            val bottomInset = safeAreaLocationCard.root.height
            setPadding(0, 0, 0, bottomInset)
        }
    }

    private fun showSaveDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.safe_area_dialog_save_title)
            setMessage(R.string.safe_area_dialog_save_content)
            setPositiveButton(R.string.safe_area_dialog_save_save) { _, _ ->
                viewModel.onSaveClicked()
            }
            setNegativeButton(R.string.safe_area_dialog_save_dont_save) { _, _ ->
                viewModel.onCloseClicked()
            }
        }.show()
    }

    private fun showDeleteDialog() {
        val name = (viewModel.state.value as? State.Loaded)?.name ?: return
        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.safe_area_dialog_delete_title)
            setMessage(getString(R.string.safe_area_dialog_delete_content, name))
            setPositiveButton(R.string.safe_area_dialog_delete_delete) { _, _ ->
                viewModel.onDeleteClicked()
            }
            setNegativeButton(R.string.safe_area_dialog_delete_cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
        dialog.getButton(Dialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.negative_red))
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.safe_area_location_error_dialog_title)
            setMessage(R.string.safe_area_location_error_dialog_content)
            setCancelable(false)
            setPositiveButton(R.string.safe_area_location_error_dialog_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.onCloseClicked()
            }
        }.show()
    }

}