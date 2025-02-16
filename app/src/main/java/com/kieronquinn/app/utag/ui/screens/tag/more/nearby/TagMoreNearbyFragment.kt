package com.kieronquinn.app.utag.ui.screens.tag.more.nearby

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.icu.util.LocaleData
import android.icu.util.LocaleData.MeasurementSystem
import android.icu.util.ULocale
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.window.OnBackInvokedCallback
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentTagMoreNearbyBinding
import com.kieronquinn.app.utag.databinding.IncludeNearbyRingBinding
import com.kieronquinn.app.utag.model.VolumeLevel
import com.kieronquinn.app.utag.repositories.SettingsRepository.Units
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.base.ProvidesTitle
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel.Event
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel.RingState
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel.State
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.await
import com.kieronquinn.app.utag.utils.extensions.fade
import com.kieronquinn.app.utag.utils.extensions.getActionBarSize
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.removeSuffixRecursively
import com.kieronquinn.app.utag.utils.extensions.replaceColour
import com.kieronquinn.app.utag.utils.extensions.round
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.url
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import dev.oneuiproject.oneui.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

class TagMoreNearbyFragment: BoundFragment<FragmentTagMoreNearbyBinding>(FragmentTagMoreNearbyBinding::inflate), ProvidesTitle {

    private val args by navArgs<TagMoreNearbyFragmentArgs>()

    private val lottieArrowLock = Mutex()
    private var lottieArrowAnimation: Animator? = null
    private var countdownAnimation: Animator? = null
    private var arrowState: Boolean = false
    private var rotationLocked = false
    private var bluetoothDialog: AlertDialog? = null
    private var uwbDialog: AlertDialog? = null
    private var backDispatcherCallbackNative: Any? = null

    private val uwbPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if(!granted) {
            viewModel.onUWBPermissionDenied()
        }
        viewModel.onResume()
    }

    private val viewModel by viewModel<TagMoreNearbyViewModel> {
        parametersOf(args.deviceId, args.supportsUwb, args.isRoot)
    }

    private val topPadding by lazy {
        if(args.isRoot) {
            requireContext().getActionBarSize()
        }else 0
    }

    private val glide by lazy {
        Glide.with(requireContext())
    }

    private val isSystemMeasurementImperial by lazy {
        LocaleData.getMeasurementSystem(ULocale.getDefault()) != MeasurementSystem.SI
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupLoading()
        setupRssi()
        setupUwb()
        setupBack()
        setupSearching()
        setupState()
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        requireView().keepScreenOn = true
        lockRotationIfNeeded(viewModel.state.value is State.UWB)
    }

    override fun onPause() {
        requireView().keepScreenOn = false
        lockRotationIfNeeded(false)
        super.onPause()
    }

    override fun onDestroy() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Unregister the back callback if it exists
            (backDispatcherCallbackNative as? OnBackInvokedCallback)?.let {
                requireActivity().onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
                backDispatcherCallbackNative = null
            }
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        lottieArrowAnimation?.cancel()
        lottieArrowAnimation = null
        countdownAnimation?.cancel()
        countdownAnimation = null
        super.onDestroyView()
    }

    override fun getTitle(): CharSequence {
        return ""
    }

    /**
     *  The OneUI library seems to not support predictive back using the compat library. Use the
     *  native dispatcher where possible, and where predictive back is not used, we can use compat.
     */
    @SuppressLint("RestrictedApi")
    private fun setupBack() {
        if(!args.isRoot) return //Handled by navigation system
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

    private fun setupInsets() = with(binding.root) {
        updatePadding(top = topPadding)
        onApplyInsets { view, insets ->
            val inset = insets.getInsets(SYSTEM_INSETS)
            val topInset = if(topPadding > 0) {
                topPadding + inset.top
            }else 0
            view.updatePadding(bottom = inset.bottom, top = topInset)
        }
    }

    private fun setupLoading() = with(binding.nearbyLoading) {
        nearbyLoadingText.text = getString(R.string.nearby_loading, args.deviceLabel)
        whenResumed {
            nearbyLoadingClose.onClicked {
                viewModel.close()
            }
        }
    }

    private fun setupRssi() = with(binding.nearbyRssi) {
        nearbyRssiName.text = args.deviceLabel
        nearbyRssiRing.setup()
        glide.url(args.iconUrl).into(nearbyRssiIcon)
        whenResumed {
            nearbyRssiClose.onClicked {
                viewModel.close()
            }
        }
    }

    private fun setupUwb() = with(binding.nearbyUwb) {
        nearbyUwbName.text = args.deviceLabel
        nearbyUwbRing.setup()
        glide.load(args.iconUrl).fade().into(nearbyUwbIcon)
        val accent = ContextCompat.getColor(requireContext(), R.color.oui_accent_color)
        //Override the colours, firstly all with the accent then just the arrow with white
        nearbyUwbLottieArrow.replaceColour("**", replaceWith = accent)
        nearbyUwbLottieArrow.replaceColour("arrow", "**", replaceWith = Color.WHITE)
        nearbyUwbLottieDetect.replaceColour("**", replaceWith = accent)
        nearbyUwbLottieDetect.replaceColour("arrow", "**", replaceWith = Color.WHITE)
        nearbyUwbUprightAnim.replaceColour("**", replaceWith = accent)
        nearbyUwbUprightAnim.replaceColour(
            "**",
            replaceWith = accent,
            property = LottieProperty.STROKE_COLOR
        )
        whenResumed {
            nearbyUwbClose.onClicked {
                viewModel.close()
            }
        }
        whenResumed {
            animateArrow()
        }
    }

    private fun setupSearching() = with(binding.nearbySearching) {
        nearbySearchingInstruction.text =
            getString(R.string.search_nearby_searching_content, args.deviceLabel)
        val accent = ContextCompat.getColor(requireContext(), R.color.oui_accent_color)
        nearbySearchingAnim.replaceColour(
            "**",
            replaceWith = accent,
            property = LottieProperty.STROKE_COLOR
        )
        whenResumed {
            nearbySearchingClose.onClicked {
                viewModel.close()
            }
        }
    }

    private fun IncludeNearbyRingBinding.setup() {
        nearbyRingVolUp.setIconResource(dev.oneuiproject.oneui.R.drawable.ic_oui_volume_up)
        nearbyRingVolDown.setIconResource(dev.oneuiproject.oneui.R.drawable.ic_oui_volume_down)
        nearbyRingRing.buttonEnabled = true
        whenResumed {
            nearbyRingRing.onClicked {
                viewModel.onRingClicked()
            }
        }
        whenResumed {
            nearbyRingVolUp.onClicked {
                viewModel.onRingVolumeUpClicked()
            }
        }
        whenResumed {
            nearbyRingVolDown.onClicked {
                viewModel.onRingVolumeDownClicked()
            }
        }
    }

    private fun setupEvents() = whenResumed {
        viewModel.events.collect {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: Event) {
        when(event) {
            Event.RingError -> {
                Toast.makeText(requireContext(), R.string.tag_ring_error, Toast.LENGTH_LONG).show()
            }
            is Event.FailedToConnect -> showMaxConnectionsDialog(event.deviceLabel)
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
        bluetoothDialog?.dismiss()
        uwbDialog?.dismiss()
        when(state) {
            is State.Loading -> {
                root.setDisplayedChildIfNeeded(3)
            }
            is State.RSSI -> {
                handleRssi(state)
                root.setDisplayedChildIfNeeded(0)
            }
            is State.UWB -> {
                handleUwb(state)
                root.setDisplayedChildIfNeeded(1)
            }
            is State.UWBPermissionRequired -> {
                uwbPermissionLauncher.launch(state.permission)
            }
            is State.Searching -> {
                handleSearching(state)
                root.setDisplayedChildIfNeeded(2)
            }
            is State.Timeout -> showErrorDialog(state.wasConnected)
            is State.UWBDisabled -> showUWBDisabledDialog()
            is State.EnableBluetooth -> showBluetoothDisabledDialog()
        }
        lockRotationIfNeeded(state is State.UWB)
    }

    private fun handleRssi(state: State.RSSI) = with(binding.nearbyRssi) {
        nearbyRssiProgress.setProgress(state.distance.progress)
        nearbyRssiInstruction.setText(state.moveAroundText.label)
        nearbyRssiDistance.setText(state.distance.label)
        nearbyRssiRing.handleRing(state.ringState)
    }

    private fun handleUwb(state: State.UWB) = with(binding.nearbyUwb) {
        state.azimuth?.let { angle ->
            nearbyUwbDirection.animate().rotation(angle).start()
        }
        nearbyUwbArrowFlipper.setDisplayedChildIfNeeded(if(state.isVeryCloseBy) 1 else 0)
        nearbyUwbAnimFlipper.setDisplayedChildIfNeeded(if(state.isVeryCloseBy) 1 else 0)
        val imperial = when(state.units) {
            Units.SYSTEM -> isSystemMeasurementImperial
            Units.IMPERIAL -> true
            Units.METRIC -> false
        }
        val distanceText = if(state.isVeryCloseBy) {
            getString(R.string.nearby_suggest_ring)
        }else if(imperial){
            getString(R.string.nearby_distance_feet, state.distance.formatDistanceFeet())
        }else{
            getString(R.string.nearby_distance_metres, state.distance.formatDistanceMetres())
        }
        nearbyUwbDistance.text = distanceText
        val instructionText = when {
            state.isVeryCloseBy -> {
                getString(R.string.nearby_its_here)
            }
            state.direction != null -> {
                getString(state.direction.label)
            }
            else -> {
                //User needs to reorient their device
                getString(R.string.nearby_hold_upright)
            }
        }
        nearbyUwbInstruction.text = instructionText
        nearbyUwbRing.handleRing(state.ringState)
        nearbyUwbUprightAnimFlipper.setDisplayedChildIfNeeded(if(state.requiresInclination) 1 else 0)
        lifecycleScope.launch {
            animateArrowToState(state.azimuth != null)
        }
    }

    private fun handleSearching(state: State.Searching) = with(binding.nearbySearching) {
        nearbySearchingProgress.animateToProgress((state.progress * 100).roundToInt())
        nearbySearchingTime.text = DateUtils.formatElapsedTime(state.remainingSeconds)
    }

    private fun IncludeNearbyRingBinding.handleRing(ring: RingState) {
        nearbyRingVolUp.isVisible = ring is RingState.Ringing
        nearbyRingVolDown.isVisible = ring is RingState.Ringing
        nearbyRingVolUp.showProgress = (ring as? RingState.Ringing)?.sending ?: false
        nearbyRingVolDown.showProgress = (ring as? RingState.Ringing)?.sending ?: false
        nearbyRingRing.showProgress = ring is RingState.Sending
        nearbyRingVolUp.buttonEnabled = (ring as? RingState.Ringing)?.volume == VolumeLevel.LOW
        nearbyRingVolDown.buttonEnabled = (ring as? RingState.Ringing)?.volume == VolumeLevel.HIGH
        val icon = if(ring is RingState.Ringing) {
            R.drawable.ic_ring_stop
        }else{
            R.drawable.ic_ring
        }
        nearbyRingRing.setIconResource(icon)
    }

    private fun Double.formatDistanceMetres(): String {
        return round(1).toString()
            .removeSuffixRecursively("0")
            .removeSuffix(".")
    }

    private fun Double.formatDistanceFeet(): String {
        val distance = this * 3.281
        return distance.toInt().toString()
    }

    @Synchronized
    private fun lockRotationIfNeeded(locked: Boolean) {
        if(rotationLocked == locked) return
        requireActivity().requestedOrientation = if(locked){
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER
        }
        rotationLocked = locked
    }

    private suspend fun animateArrowToState(
        expanded: Boolean
    ) = with(binding.nearbyUwb.nearbyUwbLottieArrow) {
        val animateToProgress = if(expanded) 1f else 0f
        val animateFromProgress = if(expanded) 0f else 1f
        if(arrowState == expanded) return@with
        arrowState = expanded
        progress = animateFromProgress
        //Wait for any ongoing arrow animation
        lottieArrowLock.await()
        //Hide the detect arrow if we're animating to not expanded
        if(!expanded) {
            binding.nearbyUwb.nearbyUwbLottieDetect.visibility = View.INVISIBLE
        }
        visibility = View.VISIBLE
        //Cancel any existing animation
        lottieArrowAnimation?.cancel()
        //Start a new animation to the new progress
        lottieArrowAnimation = ValueAnimator.ofFloat(progress, animateToProgress).apply {
            addUpdateListener {
                progress = it.animatedValue as Float
                if(it.animatedFraction == 1f) {
                    //Show the detect arrow if we're animating to expanded
                    if(expanded) {
                        binding.nearbyUwb.nearbyUwbLottieDetect.visibility = View.VISIBLE
                    }
                    //And hide ourself now the animation has finished
                    visibility = View.INVISIBLE
                }
            }
            start()
        }
    }

    private suspend fun animateArrow() = with(binding.nearbyUwb.nearbyUwbLottieDetect) {
        while(coroutineContext.isActive) {
            delay(1000L)
            //Skip animation and wait for next loop if the arrow isn't currently visible
            if(visibility == View.VISIBLE) {
                lottieArrowLock.withLock {
                    playAndWait()
                }
            }
            delay(1000L)
        }
    }

    private suspend fun LottieAnimationView.playAndWait() = suspendCoroutine {
        val listener = object: AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                removeAnimatorListener(this)
                it.resume(Unit)
            }

            override fun onAnimationCancel(animation: Animator) {
                removeAnimatorListener(this)
            }

            override fun onAnimationRepeat(animation: Animator) {
                //No-op
            }

            override fun onAnimationStart(animation: Animator) {
                //No-op
            }
        }
        addAnimatorListener(listener)
        playAnimation()
    }

    private fun CircularProgressIndicator.animateToProgress(progress: Int) {
        countdownAnimation?.cancel()
        countdownAnimation = ValueAnimator.ofInt(this.progress, progress).apply {
            addUpdateListener {
                this@animateToProgress.progress = it.animatedValue as Int
            }
            duration = 1000L
            start()
        }
    }

    private fun showErrorDialog(wasConnected: Boolean) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.search_nearby_timeout_title)
            setMessage(if(wasConnected) {
                R.string.search_nearby_timeout_content_alt
            }else{
                R.string.search_nearby_timeout_content
            })
            setCancelable(false)
            setPositiveButton(R.string.search_nearby_timeout_close) { dialog, _ ->
                dialog.dismiss()
                viewModel.close()
            }
        }.show()
    }

    private fun showUWBDisabledDialog() {
        uwbDialog?.dismiss()
        uwbDialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.search_nearby_uwb_disabled_title)
            setMessage(R.string.search_nearby_uwb_disabled_content)
            setCancelable(false)
            setPositiveButton(R.string.search_nearby_uwb_disabled_positive) { dialog, _ ->
                dialog.dismiss()
                viewModel.onEnableUWBClicked()
            }
            setNegativeButton(R.string.search_nearby_uwb_disabled_negative) { dialog, _ ->
                dialog.dismiss()
                viewModel.onIgnoreUWBClicked()
            }
        }.show()
    }

    private fun showBluetoothDisabledDialog() {
        bluetoothDialog?.dismiss()
        bluetoothDialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.search_nearby_bluetooth_disabled_title)
            setMessage(R.string.search_nearby_bluetooth_disabled_content)
            setCancelable(false)
            setPositiveButton(R.string.search_nearby_bluetooth_disabled_positive) { dialog, _ ->
                viewModel.onEnableBluetoothClicked()
            }
            setNegativeButton(R.string.search_nearby_bluetooth_disabled_negative) { dialog, _ ->
                dialog.dismiss()
                viewModel.close()
            }
        }.show()
    }

    private fun showMaxConnectionsDialog(label: String) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.map_connection_error_title)
            setMessage(getString(R.string.map_connection_error_content, label))
            setCancelable(false)
            setPositiveButton(R.string.map_connection_error_close) { _, _ ->
                viewModel.close()
            }
        }.show()
    }

}