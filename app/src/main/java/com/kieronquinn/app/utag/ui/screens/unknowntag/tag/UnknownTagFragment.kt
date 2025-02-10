package com.kieronquinn.app.utag.ui.screens.unknowntag.tag

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.google.android.gms.maps.model.TextureStyle
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentUnknownTagBinding
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.ui.activities.UnknownTagActivity
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.LockCollapsed
import com.kieronquinn.app.utag.ui.screens.unknowntag.tag.UnknownTagViewModel.State
import com.kieronquinn.app.utag.ui.screens.unknowntag.tag.UnknownTagViewModel.State.Loaded.Location
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.dip
import com.kieronquinn.app.utag.utils.extensions.formatDateTime
import com.kieronquinn.app.utag.utils.extensions.getMap
import com.kieronquinn.app.utag.utils.extensions.isDarkMode
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import kotlinx.coroutines.flow.first
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate

class UnknownTagFragment: BoundFragment<FragmentUnknownTagBinding>(FragmentUnknownTagBinding::inflate), BackAvailable, LockCollapsed {

    private val args by navArgs<UnknownTagFragmentArgs>()
    private var appliedMapTheme: MapTheme? = null
    private var appliedMapStyle: MapStyle? = null

    private val dateFormat by lazy {
        DateFormat.getDateFormat(requireContext())
    }

    private val timeFormat by lazy {
        DateFormat.getTimeFormat(requireContext())
    }

    private val darkMapTheme by lazy {
        MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.mapstyle_dark)
    }

    private val viewModel by viewModel<UnknownTagViewModel> {
        parametersOf(args.privacyId, UnknownTagActivity.isStandalone(this))
    }

    private val margin by lazy {
        resources.getDimensionPixelSize(R.dimen.margin_16)
    }

    private val lineSpan by lazy {
        val bitmap = BitmapDescriptorFactory.fromBitmap(
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.map_line_arrow_uts),
                resources.dip(4),
                resources.dip(4),
                true
            ).copy(Bitmap.Config.ARGB_8888, true)
        )
        val strokeStyle = StrokeStyle.colorBuilder(Color.TRANSPARENT)
        val textureStyle = TextureStyle.newBuilder(bitmap).build()
        StyleSpan(strokeStyle.stamp(textureStyle).build())
    }

    private val markerStart by lazy {
        BitmapDescriptorFactory.fromBitmap(
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.ic_marker_uts_start),
                resources.dip(12),
                resources.dip(12),
                true
            ).copy(Bitmap.Config.ARGB_8888, true)
        )
    }

    private val markerPoint by lazy {
        BitmapDescriptorFactory.fromBitmap(
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.ic_marker_uts),
                resources.dip(18),
                resources.dip(18),
                true
            ).copy(Bitmap.Config.ARGB_8888, true)
        )
    }

    private val markerEnd by lazy {
        BitmapDescriptorFactory.fromBitmap(
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.ic_marker_uts_end),
                resources.dip(24),
                resources.dip(24),
                true
            ).copy(Bitmap.Config.ARGB_8888, true)
        )
    }

    private val mapFragment
        get() = childFragmentManager.findFragmentById(R.id.unknown_tag_map) as SupportMapFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupInsets()
        setupButtons()
        setupMap()
        binding.unknownTagScrollable.isNestedScrollingEnabled = false
    }

    private fun setupInsets() = with(binding.unknownTagContent) {
        val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
        onApplyInsets { view, insets ->
            val inset = insets.getInsets(SYSTEM_INSETS)
            view.updatePadding(bottom = inset.bottom + padding)
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun setupMap() = whenResumed {
        with(mapFragment.getMap().first()) {
            uiSettings.setAllGesturesEnabled(false)
            setOnMarkerClickListener { true }
        }
    }

    private fun setupButtons() {
        whenResumed {
            binding.unknownTagSafe.onClicked {
                showSafeDialog()
            }
        }
        whenResumed {
            binding.unknownTagOpen.onClicked {
                val intent = requireContext().packageManager
                    .getLaunchIntentForPackage(PACKAGE_NAME_ONECONNECT) ?: return@onClicked
                startActivity(intent)
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

    private fun handleState(state: State) {
        when(state) {
            is State.Loading -> binding.root.setDisplayedChildIfNeeded(1)
            is State.Loaded -> handleLoaded(state)
            is State.Error -> showErrorDialog()
        }
    }

    private fun handleLoaded(state: State.Loaded) {
        binding.root.setDisplayedChildIfNeeded(0)
        handleMap(state)
        val now = LocalDate.now()
        val firstSeenTime = state.locations.first().timestamp
        binding.unknownTagFirstSeenTime.text = if(firstSeenTime.toLocalDate() == now) {
            getString(
                R.string.settings_uts_tag_last_at,
                timeFormat.formatDateTime(firstSeenTime)
            )
        }else{
            getString(
                R.string.settings_uts_tag_last,
                dateFormat.formatDateTime(firstSeenTime)
            )
        }
        val lastSeenTime = state.locations.last().timestamp
        binding.unknownTagLastSeenTime.text = if(lastSeenTime.toLocalDate() == now) {
            getString(
                R.string.settings_uts_tag_last_at,
                timeFormat.formatDateTime(lastSeenTime)
            )
        }else{
            getString(
                R.string.settings_uts_tag_last,
                dateFormat.formatDateTime(lastSeenTime)
            )
        }
        binding.unknownTagFirstSeenAddress.text = state.locations.first().address
            ?: getString(R.string.settings_uts_tag_unknown_address)
        binding.unknownTagLastSeenAddress.text = state.locations.last().address
            ?: getString(R.string.settings_uts_tag_unknown_address)
    }

    private fun handleMap(state: State.Loaded) = whenResumed {
        with(mapFragment.getMap().first()) {
            clear()
            applyTheme(state.mapTheme, state.mapStyle)
            applyStyle(state.mapStyle)
            val bounds = addPoints(state.locations)
            val update = CameraUpdateFactory.newLatLngBounds(bounds, margin * 4)
            moveCamera(update)
        }
    }

    private fun GoogleMap.addPoints(points: List<Location>): LatLngBounds {
        val bounds = LatLngBounds.Builder()
        points.forEachIndexed { index, point ->
            val nextPoint = points.getOrNull(index + 1)
            if(nextPoint != null) {
                addPolyline(
                    PolylineOptions().add(point.location, nextPoint.location).addSpan(lineSpan)
                )
            }
            val marker = when {
                index == 0 -> {
                    MarkerOptions()
                        .position(point.location)
                        .icon(markerStart)
                        .anchor(0.5f, 0.5f)
                }
                nextPoint == null -> {
                    MarkerOptions()
                        .position(point.location)
                        .icon(markerEnd)
                        .anchor(0.5f, 0.9f)
                }
                else -> {
                    MarkerOptions()
                        .position(point.location)
                        .icon(markerPoint)
                        .anchor(0.5f, 0.5f)
                }
            }
            addMarker(marker)?.apply {
                tag = point
                zIndex = 100f + index
            }
            bounds.include(point.location)
        }
        return bounds.build()
    }

    @Synchronized
    private fun GoogleMap.applyTheme(theme: MapTheme, style: MapStyle): Boolean? {
        return when(style) {
            MapStyle.TERRAIN -> false //Light theme
            MapStyle.SATELLITE -> true //Dark theme
            MapStyle.NORMAL -> {
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
                if(themeToApply == appliedMapTheme) return null
                setMapStyle(if(themeToApply == MapTheme.DARK) darkMapTheme else null)
                appliedMapTheme = themeToApply
                themeToApply == MapTheme.DARK
            }
        }
    }

    @Synchronized
    private fun GoogleMap.applyStyle(style: MapStyle) {
        if(style == appliedMapStyle) return
        mapType = style.style
        appliedMapStyle = style
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.settings_uts_tag_error_dialog_title)
            setMessage(R.string.settings_uts_tag_error_dialog_content)
            setCancelable(false)
            setPositiveButton(R.string.settings_uts_tag_error_dialog_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.close()
            }
        }.show()
    }

    private fun showSafeDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.settings_uts_tag_safe_dialog_title)
            setMessage(getText(R.string.settings_uts_tag_safe_dialog_content))
            setPositiveButton(R.string.settings_uts_tag_safe_dialog_positive) { dialog, _ ->
                dialog.dismiss()
                viewModel.onSafeClicked()
            }
            setNegativeButton(R.string.settings_uts_tag_safe_dialog_negative) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

}