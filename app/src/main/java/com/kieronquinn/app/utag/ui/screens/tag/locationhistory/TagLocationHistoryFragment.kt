package com.kieronquinn.app.utag.ui.screens.tag.locationhistory

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import android.widget.Toast
import android.window.OnBackInvokedCallback
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SeslProgressBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.google.android.gms.maps.model.TextureStyle
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentTagLocationHistoryBinding
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.LocationHistoryPoint
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.TagLocationHistoryAdapter.ListItem
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.TagLocationHistoryViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.TagLocationHistoryViewModel.State
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.datepicker.TagLocationHistoryDatePickerDialogFragment.Companion.setupDatePickerResultListener
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.Companion.setupPinEntryResultListener
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.PinEntryResult
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.animateTo
import com.kieronquinn.app.utag.utils.extensions.animateToProgress
import com.kieronquinn.app.utag.utils.extensions.dip
import com.kieronquinn.app.utag.utils.extensions.getMap
import com.kieronquinn.app.utag.utils.extensions.isDarkMode
import com.kieronquinn.app.utag.utils.extensions.isLandscape
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.scaleAndRecycle
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.setVisible
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.extensions.widthWithMargins
import com.kieronquinn.app.utag.utils.recyclerview.CenterLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

class TagLocationHistoryFragment: BoundFragment<FragmentTagLocationHistoryBinding>(FragmentTagLocationHistoryBinding::inflate) {

    private val exportFilePicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { uri ->
        if(uri != null) {
            viewModel.onExportUriResult(uri)
        }
    }

    private val args by navArgs<TagLocationHistoryFragmentArgs>()
    private var isShowingError = false
    private var mapContentHash: Int? = null
    private var myLocationMarker: Marker? = null
    private var mapMarker: Marker? = null
    private var mapMarkerHash: Int? = null
    private var listContentHash: Int? = null
    private var hasCameraInitiallyMoved = false
    private var backDispatcherCallbackNative: Any? = null
    private var appliedMapTheme: MapTheme? = null
    private var appliedMapStyle: MapStyle? = null
    private var menu: Menu? = null
    private var map: WeakReference<GoogleMap>? = null

    private val darkMapTheme by lazy {
        loadRawResourceStyle(requireContext(), R.raw.mapstyle_dark)
    }

    private val viewModel by viewModel<TagLocationHistoryViewModel> {
        parametersOf(args.deviceId, args.deviceLabel)
    }

    private val mapFragment
        get() = childFragmentManager.findFragmentById(R.id.tag_location_history_map) as SupportMapFragment

    private val margin by lazy {
        resources.getDimensionPixelSize(R.dimen.margin_16)
    }

    private val dateFormat by lazy {
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE, MMM d yyyy")
        )
    }

    private val lineSpan by lazy {
        val bitmap = BitmapDescriptorFactory.fromBitmap(
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.map_line_arrow),
                resources.dip(4),
                resources.dip(4),
                true
            ).copy(Bitmap.Config.ARGB_8888, true)
        )
        val strokeStyle = StrokeStyle.colorBuilder(Color.TRANSPARENT)
        val textureStyle = TextureStyle.newBuilder(bitmap).build()
        StyleSpan(strokeStyle.stamp(textureStyle).build())
    }

    private val markerPoint by lazy {
        BitmapDescriptorFactory.fromBitmap(
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.ic_marker_point),
                resources.dip(18),
                resources.dip(18),
                true
            ).copy(Bitmap.Config.ARGB_8888, true)
        )
    }

    private val selectedMarker by lazy {
        val pin = BitmapFactory.decodeResource(resources, R.drawable.ic_marker)
        BitmapDescriptorFactory.fromBitmap(
            pin.scaleAndRecycle(resources.dip(40), resources.dip(44))
        )
    }

    private val adapter by lazy {
        TagLocationHistoryAdapter(
            requireContext(),
            emptyList(),
            null,
            false,
            viewModel::onPointClicked,
            ::showLocationInfoDialog
        )
    }

    private val layoutManager by lazy {
        CenterLayoutManager(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupHeader()
        setupInsets()
        setupMap()
        setupLoading()
        setupBack()
        setupEvent()
        setupState()
        setupPinResult()
        setupCard()
        setupCardHeader()
        setupDateListener()
        setupIntro()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    /**
     *  The OneUI library seems to not support predictive back using the compat library. Use the
     *  native dispatcher where possible, and where predictive back is not used, we can use compat.
     */
    @SuppressLint("RestrictedApi")
    private fun setupBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupBackNative()
        }else{
            setupBackCompat()
        }
    }

    private fun setupBackCompat() = whenResumed {
        requireActivity().onBackPressedDispatcher.addCallback(
            object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.onBackPressed()
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupBackNative() = whenResumed {
        val callback = OnBackInvokedCallback {
            viewModel.onBackPressed()
        }
        backDispatcherCallbackNative = callback
        requireActivity().onBackInvokedDispatcher
            .registerOnBackInvokedCallback(100, callback)
    }

    override fun onDestroyView() {
        mapMarker = null
        myLocationMarker = null
        map?.get()?.clear()
        super.onDestroyView()
    }

    override fun onDestroy() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Unregister the back callback if it exists
            (backDispatcherCallbackNative as? OnBackInvokedCallback)?.let {
                requireActivity().onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
                backDispatcherCallbackNative = null
            }
        }
        mapMarker = null
        myLocationMarker = null
        super.onDestroy()
    }

    private fun setupHeader() = with(binding.root) {
        setCollapsedSubtitle(args.deviceLabel)
        setNavigationButtonOnClickListener {
            viewModel.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_location_history, menu)
        menu.findItem(R.id.menu_location_history_delete).isVisible = args.isOwner
        this.menu = menu
        updateMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_location_history_delete -> showDeleteDialog()
            R.id.menu_location_history_export -> showExportDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateMenu() {
        menu?.setVisible(viewModel.state.value.showMenu())
    }

    private fun setupInsets() = with(binding) {
        root.onApplyInsets { view, insets ->
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            val leftInsets = systemInsets.left
            val rightInsets = systemInsets.right
            view.updateLayoutParams<MarginLayoutParams> {
                updateMargins(left = leftInsets, right = rightInsets)
            }
            binding.tagLocationHistoryIntro.bottomNavigation.updateLayoutParams<MarginLayoutParams> {
                updateMargins(bottom = systemInsets.bottom + margin)
            }
            headerView.updatePadding(top = systemInsets.top)
            if(requireContext().isLandscape()) {
                tagLocationHistoryCard.root.updateLayoutParams<MarginLayoutParams> {
                    updateMargins(bottom = systemInsets.bottom)
                }
            }else{
                headerView.toolbar.updatePadding(left = leftInsets, right = rightInsets)
                tagLocationHistoryCard.tagLocationHistoryRecyclerView.updatePadding(
                    bottom = systemInsets.bottom + margin
                )
                tagLocationHistoryCard.tagLocationHistoryEmpty.updatePadding(
                    bottom = systemInsets.bottom
                )
            }
            viewModel.onInsetsChanged(systemInsets)
        }
    }

    private fun setupMap() = whenResumed {
        with(mapFragment.getMap().first()) {
            map = WeakReference(this)
            isBuildingsEnabled = false
            uiSettings.isMapToolbarEnabled = false
            setOnMarkerClickListener { marker ->
                (marker.tag as? LocationHistoryPoint)?.let {
                    viewModel.onPointClicked(it)
                    adapter.setSelectedItem(it)?.let { index ->
                        binding.tagLocationHistoryCard.tagLocationHistoryRecyclerView
                            .smoothScrollToPosition(index)
                    }
                }
                false
            }
        }
    }

    private fun setupLoading() = with(binding) {
        tagLocationHistoryProgress.setMode(SeslProgressBar.MODE_CIRCLE)
    }

    private fun handleMap(state: State.Loaded) = whenResumed {
        with(mapFragment.getMap().first()) {
            applyMapPadding(state.insets ?: Insets.NONE)
            val contentHash = state.selectedDate.hashCode()
            if(mapContentHash != contentHash) {
                mapContentHash = contentHash
                clear()
                //The selected marker has also been removed here, so remove the cache
                mapMarkerHash = null
                mapMarker = null
                myLocationMarker = null
                val bounds = addPoints(state.points)
                val update = CameraUpdateFactory.newLatLngBounds(bounds, margin * 4)
                if(!hasCameraInitiallyMoved) {
                    hasCameraInitiallyMoved = true
                    moveCamera(update)
                }else {
                    animateCamera(update)
                }
            }
            val markerHash = state.selectedPoint.hashCode()
            if(mapMarkerHash != markerHash) {
                mapMarkerHash = markerHash
                mapMarker = mapMarker?.also {
                    it.position = state.selectedPoint.location
                } ?: addMarker(
                    MarkerOptions()
                        .position(state.selectedPoint.location)
                        .anchor(0.5f, 0.93f)
                        .zIndex(Float.MAX_VALUE)
                        .icon(selectedMarker)
                )
            }
            handleMyLocation(state)
            applyStyle(state.mapOptions.style)
            applyTheme(state.mapOptions.theme, state.mapOptions.style)
            isBuildingsEnabled = state.mapOptions.showBuildings
        }
    }

    private fun GoogleMap.addPoints(points: List<LocationHistoryPoint>): LatLngBounds {
        val bounds = LatLngBounds.Builder()
        points.forEachIndexed { index, point ->
            val nextPoint = points.getOrNull(index + 1)
            if(nextPoint != null) {
                addPolyline(
                    PolylineOptions().add(point.location, nextPoint.location).addSpan(lineSpan)
                )
            }
            val pointMarker = MarkerOptions()
                .position(point.location)
                .icon(markerPoint)
                .anchor(0.5f, 0.5f)
            addMarker(pointMarker)?.apply {
                tag = point
                zIndex = 100f
            }
            bounds.include(point.location)
        }
        return bounds.build()
    }

    @Synchronized
    private fun GoogleMap.handleMyLocation(state: State.Loaded) {
        val location = state.mapOptions.location
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
            val leftInset = tagLocationHistoryCard.root.widthWithMargins
            setPadding(leftInset, 0, 0, insets.bottom)
        }else{
            //Map is above the card
            val bottomInset = tagLocationHistoryCard.root.height
            setPadding(0, 0, 0, bottomInset)
        }
    }

    private fun setupEvent() {
        whenResumed {
            viewModel.event.collect {
                handleEvent(it)
            }
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.DELETE_FAILED -> {
                Toast.makeText(
                    requireContext(), R.string.tag_location_history_delete_error, Toast.LENGTH_LONG
                ).show()
            }
            Event.DELETE_DISABLED -> {
                Toast.makeText(
                    requireContext(), R.string.tag_location_history_delete_disabled, Toast.LENGTH_LONG
                ).show()
            }
            Event.EXPORT_DISABLED -> {
                Toast.makeText(
                    requireContext(), R.string.tag_location_history_export_disabled, Toast.LENGTH_LONG
                ).show()
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
        handleCardHeader(state)
        updateMenu()
        when(state) {
            is State.Intro -> {
                tagLocationHistoryFlipper.setDisplayedChildIfNeeded(2)
                tagLocationHistoryMapPlaceholder.isVisible = false
            }
            is State.Loading -> {
                tagLocationHistoryFlipper.setDisplayedChildIfNeeded(1)
                tagLocationHistoryMapPlaceholder.isVisible = false
                if(state.progress != null) {
                    tagLocationHistoryProgress.animateToProgress(state.progress)
                    tagLocationHistoryProgressFlipper.setDisplayedChildIfNeeded(0)
                }else{
                    tagLocationHistoryProgressFlipper.setDisplayedChildIfNeeded(1)
                }
            }
            is State.Loaded -> {
                tagLocationHistoryFlipper.setDisplayedChildIfNeeded(0)
                tagLocationHistoryCard.tagLocationHistoryCardFlipper.setDisplayedChildIfNeeded(0)
                tagLocationHistoryMapFlipper.setDisplayedChildIfNeeded(0)
                tagLocationHistoryMapPlaceholder.isVisible = false
                handleMap(state)
                handleContent(state)
            }
            is State.Empty -> {
                tagLocationHistoryFlipper.setDisplayedChildIfNeeded(0)
                tagLocationHistoryCard.tagLocationHistoryCardFlipper.setDisplayedChildIfNeeded(1)
                tagLocationHistoryMapFlipper.setDisplayedChildIfNeeded(1)
                tagLocationHistoryMapPlaceholder.isVisible = true
            }
            is State.Error -> {
                showErrorDialog()
            }
            is State.PINRequired -> {
                tagLocationHistoryFlipper.setDisplayedChildIfNeeded(1)
                tagLocationHistoryMapPlaceholder.isVisible = false
                viewModel.showPinEntry()
            }
        }
    }

    private fun setupPinResult() {
        setupPinEntryResultListener {
            when(it) {
                is PinEntryResult.Success -> {
                    viewModel.onPinEntered(it)
                }
                is PinEntryResult.Failed -> {
                    viewModel.onPinCancelled()
                }
            }
        }
    }

    private fun setupCard() = with(binding.tagLocationHistoryCard) {
        root.clipToOutline = true
        tagLocationHistoryRecyclerView.layoutManager = layoutManager
        tagLocationHistoryRecyclerView.adapter = adapter
    }

    private fun setupCardHeader() = with(binding.tagLocationHistoryCard) {
        whenResumed {
            tagLocationHistoryPrevious.onClicked {
                viewModel.onPreviousClicked()
            }
        }
        whenResumed {
            tagLocationHistoryNext.onClicked {
                viewModel.onNextClicked()
            }
        }
        whenResumed {
            tagLocationHistoryCalendar.onClicked {
                viewModel.onDateClicked()
            }
        }
    }

    private fun setupDateListener() {
        setupDatePickerResultListener {
            viewModel.onDateSelected(it)
        }
    }

    private fun setupIntro() = with(binding.tagLocationHistoryIntro) {
        whenResumed {
            locationHistoryIntroNext.onClicked {
                viewModel.onIntroGotItClicked()
            }
        }
    }

    private fun handleCardHeader(state: State) = with(binding.tagLocationHistoryCard) {
        tagLocationHistoryPrevious.setButtonEnabled(state.previousEnabled)
        tagLocationHistoryNext.setButtonEnabled(state.nextEnabled)
        tagLocationHistoryCalendar.setButtonEnabled(state !is State.Loading)
        tagLocationHistoryDate.text = dateFormat.format(state.selectedDate)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleContent(state: State.Loaded) {
        val hash = state.points.hashCode()
        if(listContentHash != hash) {
            listContentHash = hash
            val header = if(state.decryptFailed) {
                listOf(ListItem.Header)
            }else emptyList()
            adapter.items = header + state.points.map { ListItem.Item(it) }.reversed()
            adapter.selectedDay = state.selectedDate.toLocalDate()
            adapter.debugModeEnabled = state.debugEnabled
            adapter.notifyDataSetChanged()
            if(adapter.itemCount > 0) {
                binding.tagLocationHistoryCard.tagLocationHistoryRecyclerView
                    .scrollToPosition(0)
            }
        }
        setSelectedItem(state.selectedPoint)
    }

    private fun setSelectedItem(item: LocationHistoryPoint?) = with(binding.tagLocationHistoryCard) {
        val newIndex = adapter.setSelectedItem(item)
        if(item == null || newIndex == null) return
        tagLocationHistoryRecyclerView.smoothScrollToPosition(newIndex)
    }

    private fun ImageButton.setButtonEnabled(enabled: Boolean) {
        isEnabled = enabled
        alpha = if(enabled) 1f else 0.5f
    }

    private fun showErrorDialog() {
        if(isShowingError) return
        isShowingError = true
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tag_location_history_error_title)
            setMessage(R.string.tag_location_history_error_content)
            setCancelable(false)
            setPositiveButton(R.string.tag_location_history_error_close) { _, _ ->
                viewModel.onBackPressed()
            }
        }.show()
    }

    private fun showDeleteDialog() {
        val dialog = AlertDialog.Builder(requireContext()).apply {
            setMessage(
                getString(R.string.tag_location_history_delete_dialog_content, args.deviceLabel)
            )
            setPositiveButton(R.string.tag_location_history_delete_dialog_delete) { dialog, _ ->
                dialog.dismiss()
                viewModel.onDeleteClicked()
            }
            setNegativeButton(R.string.tag_location_history_delete_dialog_cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
        dialog.getButton(Dialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.negative_red))
    }

    private fun showLocationInfoDialog(location: LocationHistoryPoint) = whenResumed {
        val message = withContext(Dispatchers.IO) {
            StringBuilder().apply {
                appendLine(
                    getString(R.string.tag_location_history_info_content_header, location.locations.size)
                )
                appendLine()
                location.locations.forEachIndexed { index, item ->
                    appendLine(
                        getString(
                            R.string.tag_location_history_info_content_item,
                            index + 1,
                            item.latitude.toString(),
                            item.longitude.toString(),
                            Instant.ofEpochMilli(item.time).toString(),
                            item.accuracy.toString(),
                            item.speed?.toString(),
                            item.rssi?.toString(),
                            item.battery?.let { getString(it.labelRaw) },
                            item.method,
                            item.findHost?.let { getString(it.label) },
                            item.nearby?.toString(),
                            item.onDemand?.toString(),
                            item.connectedUserId,
                            item.connectedDeviceId,
                            item.d2dStatus?.let { getString(it.label) },
                            item.wasEncrypted.toString()
                        )
                    )
                    appendLine()
                }
            }
        }
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.tag_location_history_info_title)
            setMessage(message)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showExportDialog() {
        val state = viewModel.state.value as? State.Loaded ?: return
        val filename = state.getFilename() ?: run {
            Toast.makeText(
                requireContext(),
                R.string.tag_location_history_export_error_toast,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        AlertDialog.Builder(requireContext()).apply {
            val message = SpannableStringBuilder().apply {
                appendLine(
                    getString(R.string.tag_location_history_export_dialog_content, args.deviceLabel)
                )
                if(state.decryptFailed) {
                    appendLine()
                    append(getText(R.string.tag_location_history_export_dialog_warning))
                }
            }
            setTitle(R.string.tag_location_history_export)
            setMessage(message)
            setPositiveButton(R.string.tag_location_history_export_dialog_export) { dialog, _ ->
                dialog.dismiss()
                exportFilePicker.launch(filename)
            }
            setNegativeButton(R.string.tag_location_history_export_dialog_cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun State.Loaded.getFilename(): String? {
        val tagName = args.deviceLabel.lowercase().replace(" ", "_")
        val startTimestamp = exportLocations.firstOrNull()?.time?.toString() ?: return null
        val endTimestamp = exportLocations.lastOrNull()?.time?.toString() ?: return null
        return getString(
            R.string.tag_location_history_export_filename_template,
            tagName,
            startTimestamp,
            endTimestamp
        )
    }

}