package com.kieronquinn.app.utag.ui.screens.tag.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.util.Linkify
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentTagMapBinding
import com.kieronquinn.app.utag.repositories.ApiRepository.Companion.FIND_PRIVACY_URL
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme
import com.kieronquinn.app.utag.repositories.SmartTagRepository.TagState.Loaded.LocationState
import com.kieronquinn.app.utag.service.UTagForegroundService
import com.kieronquinn.app.utag.ui.activities.MainActivity
import com.kieronquinn.app.utag.ui.activities.TagActivity
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.screens.tag.map.TagMapViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.map.TagMapViewModel.State
import com.kieronquinn.app.utag.ui.screens.tag.map.TagMapViewModel.State.Error.ErrorType
import com.kieronquinn.app.utag.ui.screens.tag.picker.TagDevicePickerFragment.Companion.setupTagPickerListener
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.Companion.setupPinEntryResultListener
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogFragment.PinEntryResult
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.animateTo
import com.kieronquinn.app.utag.utils.extensions.fade
import com.kieronquinn.app.utag.utils.extensions.formatTime
import com.kieronquinn.app.utag.utils.extensions.getMap
import com.kieronquinn.app.utag.utils.extensions.isDarkMode
import com.kieronquinn.app.utag.utils.extensions.isLandscape
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.onMapStartMove
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.tagMap
import com.kieronquinn.app.utag.utils.extensions.url
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.extensions.widthWithMargins
import dev.oneuiproject.oneui.widget.Toast
import kotlinx.coroutines.flow.first
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.lang.ref.WeakReference

class TagMapFragment: BoundFragment<FragmentTagMapBinding>(FragmentTagMapBinding::inflate) {

    companion object {
        private const val TAG_KEY_DEVICE_ID = "device_id"
    }

    private val viewModel by viewModel<TagMapViewModel> {
        parametersOf(TagActivity.getTargetId(requireActivity()) ?: "")
    }

    private val mapFragment
        get() = childFragmentManager.findFragmentById(R.id.tag_map) as SupportMapFragment

    private val shortcutManager by lazy {
        requireContext().getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
    }

    private val margin by lazy {
        resources.getDimensionPixelSize(R.dimen.margin_16)
    }

    private val glide by lazy {
        Glide.with(requireContext())
    }

    private var hasCameraInitiallyMoved = false
    private var myLocationMarker: Marker? = null
    private val markers = HashMap<String, Marker>()
    private var appliedMapTheme: MapTheme? = null
    private var appliedMapStyle: MapStyle? = null
    private var map: WeakReference<GoogleMap>? = null

    private val darkMapTheme by lazy {
        MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.mapstyle_dark)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupState()
        setupEvents()
        setupBack()
        setupMenu()
        setupMap()
        setupPinResult()
        setupPickerResult()
        setupCard()
        UTagForegroundService.startIfNeeded(requireContext())
    }

    override fun onDestroyView() {
        myLocationMarker = null
        markers.clear()
        map?.get()?.clear()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun setupInsets() = with(binding) {
        root.onApplyInsets { _, insets ->
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            tagMapFloatingToolbar.updatePadding(
                top = systemInsets.top,
                left = systemInsets.left,
                right = systemInsets.right
            )
            tagMapLoadingFloatingToolbar.updatePadding(
                top = systemInsets.top,
                left = systemInsets.left,
                right = systemInsets.right
            )
            tagMapCard.root.updateLayoutParams<MarginLayoutParams> {
                updateMargins(
                    bottom = systemInsets.bottom + margin,
                    left = systemInsets.left + margin,
                    right = systemInsets.right + margin
                )
            }
            viewModel.setInsets(systemInsets)
        }
    }

    private fun setupBack() = whenResumed {
        binding.tagMapBack.onClicked {
            requireActivity().finish()
        }
        binding.tagMapLoadingBack.onClicked {
            requireActivity().finish()
        }
    }

    private fun setupMenu() = whenResumed {
        binding.tagMapMenu.onClicked {
            binding.tagMapFloatingToolbar.showMenu()
        }
    }

    private fun View.showMenu() {
        val showAddToHome = (viewModel.state.value as? State.Loaded)?.showAddToHome ?: false
        PopupMenu(context, this, Gravity.CENTER or Gravity.END).apply {
            inflate(R.menu.menu_map)
            menu.findItem(R.id.menu_map_add_to_home).isVisible = showAddToHome
            setOnMenuItemClickListener {
                dismiss()
                when(it.itemId) {
                    R.id.menu_map_add_to_home -> showAddToHomeDialog()
                    R.id.menu_map_settings -> {
                        startActivity(Intent(requireContext(), MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        })
                    }
                }
                true
            }
        }.show()
    }

    private fun setupCard() = with(binding.tagMapCard) {
        tagMapCardRing.text = getString(R.string.map_ring)
        tagMapCardRing.setIconResource(R.drawable.ic_ring)
        tagMapCardLocationHistory.text = getString(R.string.map_location_history)
        tagMapCardLocationHistory.setIconResource(R.drawable.ic_location_history)
        tagMapCardRefresh.text = getString(R.string.map_refresh)
        tagMapCardRefresh.setIconResource(dev.oneuiproject.oneui.R.drawable.ic_oui_refresh)
        tagMapCardMore.text = getString(R.string.map_more)
        tagMapCardMore.setIconResource(R.drawable.ic_more_horizontal)
        whenResumed {
            tagMapCardRing.onClicked {
                onRingClicked()
            }
        }
        whenResumed {
            tagMapCardLocationHistory.onClicked {
                viewModel.onLocationHistoryClicked()
            }
        }
        whenResumed {
            tagMapCardRefresh.onClicked {
                if(viewModel.state.value.isBusy()) return@onClicked
                viewModel.onRefreshClicked()
            }
        }
        whenResumed {
            binding.tagMapCard.tagMapCardMore.onClicked {
                viewModel.onMoreClicked()
            }
        }
        whenResumed {
            binding.tagMapCenter.onClicked {
                viewModel.onCenterClicked()
            }
        }
        whenResumed {
            binding.tagMapDevices.onClicked {
                viewModel.onPickerClicked()
            }
        }
        whenResumed {
            binding.tagMapBtDisabled.onClicked {
                viewModel.onBluetoothEnable()
            }
        }
        whenResumed {
            binding.tagMapOffline.onClicked {
                showOfflineDialog()
            }
        }
        whenResumed {
            binding.tagMapCard.tagMapCardAllowLocation.onClicked {
                showShareLocationDialog()
            }
        }
    }

    @SuppressLint("MissingPermission", "PotentialBehaviorOverride")
    private fun setupMap() = whenResumed {
        with(mapFragment.getMap().first()) {
            map = WeakReference(this)
            uiSettings.isMapToolbarEnabled = false
            setOnMarkerClickListener {
                val deviceId = it.tagMap[TAG_KEY_DEVICE_ID] as? String
                    ?: return@setOnMarkerClickListener false
                viewModel.setSelectedDeviceId(deviceId)
                true
            }
            onMapStartMove().collect {
                viewModel.onMapMoved()
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

    private fun setupPickerResult() {
        setupTagPickerListener {
            viewModel.setSelectedDeviceId(it)
        }
    }

    private fun setupEvents() {
        whenResumed {
            viewModel.events.collect {
                handleEvent(it)
            }
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            is Event.FailedToRefresh -> showSearchingDialog(event.deviceLabel)
            Event.NetworkError -> {
                Toast.makeText(requireContext(), R.string.map_network_error, Toast.LENGTH_LONG).show()
            }
            Event.LocationError -> {
                Toast.makeText(requireContext(), R.string.map_location_error, Toast.LENGTH_LONG).show()
            }
            is Event.FailedToConnect -> {
                if(event.reason.isLimitReached()) {
                    showMaxConnectionsDialog(event.deviceLabel)
                }
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

    private fun handleState(state: State) = with(binding.tagMapFlipper) {
        when(state) {
            is State.Loading -> {
                binding.tagMapPlaceholder.isVisible = false
                setDisplayedChildIfNeeded(1)
            }
            is State.Loaded -> {
                if(state.tagState.isPinRequired()) {
                    //Don't show content if PIN is required
                    binding.tagMapPlaceholder.isVisible = false
                    setDisplayedChildIfNeeded(1)
                    viewModel.showPinEntry()
                }else{
                    setDisplayedChildIfNeeded(0)
                    handleMap(state)
                    handleCard(state)
                }
            }
            is State.Error -> when(state.type) {
                is ErrorType.Generic -> showGenericErrorDialog(state.type.code)
                ErrorType.NoTags -> showNoTagsErrorDialog()
                ErrorType.Permissions -> showPermissionErrorDialog()
                else -> showModOrWarningErrorDialog(state.type)
            }
        }
    }

    @Synchronized
    private fun handleMap(state: State.Loaded) = with(binding) {
        val location = state.tagState.getLocation()
        if(location != null) {
            tagMapPlaceholder.isVisible = false
            tagMapInnerFlipper.setDisplayedChildIfNeeded(0)
            tagMapCenter.isVisible = true
            whenResumed {
                with(mapFragment.getMap().first()) {
                    applyMapPadding(state.insets ?: Insets.NONE)
                    handleSelectedLocation(state)
                    handleUnselectedLocations(state)
                    handleMyLocation(state)
                    applyTheme(state.mapOptions.theme, state.mapOptions.style)?.let {
                        statusNavDarkOverride.emit(it)
                    }
                    applyStyle(state.mapOptions.style)
                    isBuildingsEnabled = state.mapOptions.showBuildings
                }
            }
        }else{
            tagMapPlaceholder.isVisible = true
            tagMapCenter.isVisible = false
            tagMapInnerFlipper.setDisplayedChildIfNeeded(1)
        }
        tagMapDevices.isVisible = state.showPicker
        tagMapBtDisabled.isVisible = !state.mapOptions.bluetoothEnabled
        tagMapOffline.isVisible = state.tagState.locationState?.cached == true
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

    @Synchronized
    private fun GoogleMap.handleSelectedLocation(state: State.Loaded) {
        state.tagState.getLocation()?.let {
            val marker = markers[state.deviceId]
            val icon = BitmapDescriptorFactory
                .fromBitmap(state.tagState.device.markerIcons.first)
            marker?.let { currentMarker ->
                currentMarker.animateTo(it.latLng)
                currentMarker.tagMap[TAG_KEY_DEVICE_ID] = state.deviceId
                currentMarker.zIndex = 100f
                currentMarker.setIcon(icon)
            } ?: run {
                val newMarker = addMarker(
                    MarkerOptions()
                        .icon(icon)
                        .anchor(0.5f, 0.96f)
                        .position(it.latLng)
                )
                if(newMarker != null) {
                    newMarker.tagMap[TAG_KEY_DEVICE_ID] = state.deviceId
                    newMarker.zIndex = 100f
                    markers[state.deviceId] = newMarker
                }
            }
            if(state.mapOptions.shouldCenter) {
                if(hasCameraInitiallyMoved) {
                    animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, 19f))
                }else{
                    moveCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, 19f))
                }
                hasCameraInitiallyMoved = true
            }
        }
    }

    @Synchronized
    private fun GoogleMap.handleUnselectedLocations(state: State.Loaded) {
        val unselectedLocations = state.otherTags.filter { it.getLocation() != null }
        val allTagIds = setOf(
            state.deviceId,
            *unselectedLocations.map { it.device.deviceId }.toTypedArray()
        )
        val removedMarkers = markers.filterNot { allTagIds.contains(it.key) }
        removedMarkers.forEach { it.value.remove() }
        unselectedLocations.forEach {
            val location = it.getLocation() ?: return@forEach
            val deviceId = it.device.deviceId
            val marker = markers[deviceId]
            val icon = BitmapDescriptorFactory
                .fromBitmap(it.device.markerIcons.second)
            marker?.let { currentMarker ->
                currentMarker.animateTo(location.latLng)
                currentMarker.setIcon(icon)
                currentMarker.zIndex = 1f
                currentMarker.tagMap[TAG_KEY_DEVICE_ID] = deviceId
            } ?: run {
                val newMarker = addMarker(
                    MarkerOptions()
                        .icon(icon)
                        .anchor(0.5f, 0.96f)
                        .position(location.latLng)
                )
                if(newMarker != null) {
                    newMarker.tagMap[TAG_KEY_DEVICE_ID] = deviceId
                    newMarker.zIndex = 1f
                    markers[deviceId] = newMarker
                }
            }
        }
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

    private fun GoogleMap.applyMapPadding(insets: Insets) = with(binding) {
        //Toolbar height includes top inset
        val topInset = tagMapFloatingToolbar.height
        if(requireContext().isLandscape()) {
            //Map is to the right of the card, margins include the insets
            val leftInset = tagMapCard.root.widthWithMargins
            setPadding(leftInset, topInset, insets.right, insets.bottom)
        }else{
            //Map is above the card, bottom margin is the inset
            val bottomInset = tagMapCard.root.height + tagMapCard.root.marginBottom
            setPadding(insets.left, topInset, insets.right, bottomInset)
        }
    }

    private fun handleCard(state: State.Loaded) = with(binding.tagMapCard) {
        val active = if(state.tagState.isInPassiveMode) {
            state.isScannedOrConnected
        }else{
            state.isConnected
        }
        if(active) {
            glide.url(state.tagState.device.icon).fade().into(tagMapCardIcon)
        }else{
            glide.url(state.tagState.device.disconnectedIcon).fade().into(tagMapCardIcon)
        }
        val locationState = state.tagState.locationState
        val time = when(locationState) {
            is LocationState.Location -> locationState.time
            is LocationState.PINRequired -> locationState.time
            is LocationState.NoKeys -> locationState.time
            else -> null
        }
        val address = when {
            locationState is LocationState.Location -> locationState.address
            locationState is LocationState.NoKeys || locationState is LocationState.PINRequired -> {
                getString(R.string.map_address_encrypted)
            }
            state.requiresMutualAgreement -> getString(R.string.map_address_denied)
            else -> getString(R.string.map_address_unknown)
        }
        val statuses = ArrayList<String>()
        if(!state.tagState.device.isOwner) {
            val memberName = state.knownTagMembers?.get(state.tagState.device.ownerId)
            if(memberName != null) {
                getString(R.string.map_owner, memberName)
            }else{
                getString(R.string.map_owner_generic)
            }.let {
                statuses.add(it)
            }
        }
        if(state.tagState.isInPassiveMode && !state.requiresMutualAgreement) {
           statuses.add(getString(R.string.map_passive))
        }
        val status = statuses.joinToString(" • ")
        tagMapCardOwner.text = status
        tagMapCardOwner.isVisible = statuses.isNotEmpty()
        tagMapCardShared.isVisible = state.tagState.device.shareable && state.tagState.device.isOwner
        tagMapCardTitle.text = state.tagState.device.label
        tagMapCardAddress.text = address
        tagMapCardStatus.text = time?.let {
            requireContext().formatTime(it)
        } ?: getString(R.string.map_time_unknown)
        tagMapCardControls.isVisible = !state.requiresMutualAgreement
        tagMapCardAllowLocation.isVisible = state.requiresMutualAgreement
        tagMapCardStatus.isVisible = !state.requiresMutualAgreement
        tagMapCardRefresh.showProgress = state.isRefreshing
        whenResumed {
            if(locationState is LocationState.PINRequired || locationState is LocationState.NoKeys) {
                tagMapCardContent.isClickable = true
                tagMapCardContent.isFocusable = true
                tagMapCardContent.onClicked {
                    viewModel.showPinEntry()
                }
            }else{
                tagMapCardContent.isClickable = false
                tagMapCardContent.isFocusable = false
                tagMapCardContent.setOnClickListener(null)
            }
        }
    }

    private fun onRingClicked() {
        val tagState = (viewModel.state.value as? State.Loaded)?.tagState ?: return
        if(tagState.isInPassiveMode) {
             showPassiveModeDialog()
        }else {
            viewModel.onRingClicked()
        }
    }

    private fun showGenericErrorDialog(code: Int) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_error_title)
            setMessage(getString(R.string.map_error_content, code))
            setCancelable(false)
            setPositiveButton(R.string.map_error_close) { _, _ ->
                requireActivity().finish()
            }
        }.show()
    }

    private fun showNoTagsErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_error_title)
            setMessage(R.string.map_error_no_tags_content)
            setCancelable(false)
            setPositiveButton(R.string.map_error_close) { _, _ ->
                requireActivity().finish()
            }
        }.show()
    }

    private fun showPassiveModeDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_passive_mode_dialog_title)
            setMessage(R.string.map_passive_mode_dialog_content)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showMaxConnectionsDialog(label: String) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_connection_error_title)
            setMessage(getString(R.string.map_connection_error_content, label))
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showModOrWarningErrorDialog(type: ErrorType) {
        val title = when(type) {
            is ErrorType.SmartThingsNotInstalled,
            is ErrorType.ModuleNotActivated -> R.string.map_error_title
            is ErrorType.ModuleNewer,
            is ErrorType.ModuleOutdated -> R.string.map_warning_title
            else -> return //Not handled here
        }
        val content = when(type) {
            is ErrorType.SmartThingsNotInstalled -> R.string.map_error_no_smartthings_content
            is ErrorType.ModuleNotActivated -> {
                if(type.isUTagMod) {
                    R.string.map_error_mod_not_activated_patch_content
                }else{
                    R.string.map_error_mod_not_activated_xposed_content
                }
            }
            is ErrorType.ModuleNewer -> {
                if(type.isUTagMod) {
                    R.string.map_warning_mod_newer_patch_content
                }else{
                    R.string.map_warning_mod_newer_xposed_content
                }
            }
            is ErrorType.ModuleOutdated -> {
                if(type.isUTagMod) {
                    R.string.map_warning_mod_outdated_patch_content
                }else{
                    R.string.map_warning_mod_outdated_xposed_content
                }
            }
            else -> return //Not handled here
        }
        val cancelable = when(type) {
            is ErrorType.SmartThingsNotInstalled,
            is ErrorType.ModuleNotActivated -> false
            is ErrorType.ModuleNewer,
            is ErrorType.ModuleOutdated -> true
            else -> return //Not handled here
        }
        AlertDialog.Builder(requireContext()).apply {
            setTitle(title)
            setMessage(content)
            setCancelable(cancelable)
            setPositiveButton(R.string.map_error_close) { dialog, _ ->
                if(cancelable) {
                    dialog.dismiss()
                }else {
                    requireActivity().finish()
                }
            }
            setNegativeButton(R.string.map_error_open_settings) { _, _ ->
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }
        }.show()
    }

    private fun showPermissionErrorDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_error_title)
            setMessage(R.string.map_error_permissions_content)
            setCancelable(false)
            setPositiveButton(R.string.map_error_permissions_button) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    setData(Uri.parse("package:$PACKAGE_NAME_ONECONNECT"))
                }
                startActivity(intent)
                requireActivity().finish()
            }
            setNegativeButton(R.string.map_error_close) { _, _ ->
                requireActivity().finish()
            }
        }.show()
    }

    private fun showSearchingDialog(deviceLabel: String) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_searching_dialog_title)
            setMessage(getString(R.string.map_searching_dialog_content, deviceLabel))
            setPositiveButton(R.string.map_searching_dialog_enable_searching) { _, _ ->
                viewModel.setSearchingMode()
            }
            setNegativeButton(R.string.map_searching_dialog_cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showOfflineDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_offline_dialog_title)
            setMessage(getString(R.string.map_offline_dialog_content))
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showShareLocationDialog() {
        val region = (viewModel.state.value as? State.Loaded)?.region ?: return
        val privacyUrl = FIND_PRIVACY_URL.format(region.lowercase())
        val content = Html.fromHtml(
            getString(R.string.map_allow_access_dialog_content, privacyUrl),
            Html.FROM_HTML_MODE_COMPACT
        )
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_allow_access_dialog_title)
            setMessage(content)
            setPositiveButton(R.string.map_allow_access_dialog_positive) { _, _ ->
                viewModel.onAllowAccessClicked()
            }
            setNegativeButton(R.string.map_allow_access_dialog_negative) { dialog, _ ->
                dialog.dismiss()
            }
        }.create().apply {
            show()
            findViewById<TextView>(android.R.id.message)?.setupConsentText()
        }
    }

    private fun TextView.setupConsentText() {
        Linkify.addLinks(this, Linkify.WEB_URLS)
        setLinkTextColor(ContextCompat.getColor(requireContext(), R.color.oui_accent_color))
        highlightColor = ContextCompat.getColor(requireContext(), R.color.oui_accent_color_disabled)
        movementMethod = BetterLinkMovementMethod.newInstance().apply {
            setOnLinkClickListener { _, url ->
                val intent = CustomTabsIntent.Builder().apply {
                    setShowTitle(false) //Policy pages don't have a title, show the URL
                    setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                    setBookmarksButtonEnabled(false)
                    setDownloadButtonEnabled(false)
                }.build().intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    data = Uri.parse(url)
                }
                startActivity(intent)
                true
            }
        }
    }

    private fun showAddToHomeDialog() {
        val shortcutInfo = ShortcutInfo.Builder(requireContext(), "utag").build()
        shortcutManager.requestPinShortcut(shortcutInfo, null)
    }

}