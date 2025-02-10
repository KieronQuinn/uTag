package com.kieronquinn.app.utag.ui.screens.findmydevice

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.airbnb.lottie.LottieProperty
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentFindMyDeviceBinding
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository.Companion.ACTION_STOP
import com.kieronquinn.app.utag.ui.activities.FindMyDeviceActivity
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.ui.screens.findmydevice.FindMyDeviceViewModel.State
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.replaceColour
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class FindMyDeviceFragment: BoundFragment<FragmentFindMyDeviceBinding>(FragmentFindMyDeviceBinding::inflate) {

    private var countdownAnimation: Animator? = null

    private val config by lazy {
        FindMyDeviceActivity.getConfig(requireActivity())
    }

    private val viewModel by viewModel<FindMyDeviceViewModel> {
        parametersOf(config)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLottieRing()
        setupLottieDelay()
        setupText()
        setupInsets()
        setupClose()
        setupCloseListener()
        setupState()
        view.keepScreenOn = true
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
            is State.Loading -> findMyDeviceFlipper.setDisplayedChildIfNeeded(0)
            is State.Delay -> {
                findMyDeviceFlipper.setDisplayedChildIfNeeded(1)
                findMyDeviceAboutToRingProgress.animateToProgress(state.progress)
            }
            is State.Ringing -> {
                findMyDeviceFlipper.setDisplayedChildIfNeeded(2)
                findMyDeviceProgress.animateToProgress(state.progress)
            }
            is State.Close -> {
                requireActivity().finishAndRemoveTask()
            }
        }
    }

    private fun setupLottieRing() = with(binding.findMyDeviceRing) {
        val accent = ContextCompat.getColor(context, R.color.oui_accent_color)
        replaceColour("**", replaceWith = accent)
        replaceColour("**", replaceWith = accent, property = LottieProperty.STROKE_COLOR)
    }

    private fun setupLottieDelay() = with(binding.findMyDeviceAboutToRing) {
        val accent = ContextCompat.getColor(context, R.color.oui_accent_color)
        replaceColour("**", replaceWith = accent)
        replaceColour("**", replaceWith = accent, property = LottieProperty.STROKE_COLOR)
    }

    private fun setupText() = with(binding) {
        if(config.deviceLabel != null) {
            findMyDeviceTitle.text =
                getString(R.string.notification_content_find_my_device, config.deviceLabel)
            findMyDeviceAboutToRingTitle.text =
                getString(R.string.notification_content_find_my_device_delay, config.deviceLabel)
        }
    }

    private fun setupInsets() = with(binding.root) {
        val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
        onApplyInsets { view, insets ->
            val inset = insets.getInsets(SYSTEM_INSETS)
            view.updatePadding(
                left = inset.left + padding,
                right = inset.right + padding,
                top = inset.top + padding,
                bottom = inset.bottom + padding,
            )
        }
    }

    private fun setupClose() = with(binding.findMyDeviceStop) {
        whenResumed {
            onClicked {
                requireActivity().finishAndRemoveTask()
            }
        }
    }

    private fun setupCloseListener() = whenCreated {
        requireContext().broadcastReceiverAsFlow(IntentFilter(ACTION_STOP)).collect {
            requireActivity().finishAndRemoveTask()
        }
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

}