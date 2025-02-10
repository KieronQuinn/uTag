package com.kieronquinn.app.utag.ui.screens.tag.ring

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.airbnb.lottie.LottieProperty
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentTagRingDialogBinding
import com.kieronquinn.app.utag.ui.base.BaseDialogFragment
import com.kieronquinn.app.utag.ui.screens.tag.ring.TagRingDialogViewModel.State
import com.kieronquinn.app.utag.ui.screens.tag.ring.TagRingDialogViewModel.VolumeDirection
import com.kieronquinn.app.utag.utils.extensions.next
import com.kieronquinn.app.utag.utils.extensions.onClicked
import com.kieronquinn.app.utag.utils.extensions.previous
import com.kieronquinn.app.utag.utils.extensions.replaceColour
import com.kieronquinn.app.utag.utils.extensions.setDisplayedChildIfNeeded
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TagRingDialogFragment: BaseDialogFragment<FragmentTagRingDialogBinding>(FragmentTagRingDialogBinding::inflate) {

    private val args by navArgs<TagRingDialogFragmentArgs>()

    private val viewModel by viewModel<TagRingDialogViewModel> {
        parametersOf(args.deviceId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupButtons()
        setupLottie()
        binding.tagRingContent.text = getString(R.string.tag_ring_content_start, args.deviceLabel)
    }

    private fun setupButtons() = with(binding) {
        tagRingVolumeUp.setIconResource(dev.oneuiproject.oneui.R.drawable.ic_oui_volume_up)
        tagRingVolumeDown.setIconResource(dev.oneuiproject.oneui.R.drawable.ic_oui_volume_down)
    }

    private fun setupLottie() = with(binding) {
        val accent = ContextCompat.getColor(requireContext(), R.color.oui_accent_color)
        tagRingLottieBluetooth.replaceColour("**", replaceWith = accent)
        tagRingLottieBluetooth.replaceColour(
            "**", replaceWith = accent, property = LottieProperty.STROKE_COLOR
        )
        tagRingLottieNetwork.replaceColour("**", replaceWith = accent)
        tagRingLottieNetwork.replaceColour(
            "**", replaceWith = accent, property = LottieProperty.STROKE_COLOR
        )
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
            is State.Stopped -> {
                tagRingFlipper.setDisplayedChildIfNeeded(0)
                tagRingStart.text = getString(R.string.tag_ring_start)
                tagRingStart.showProgress = false
                whenResumed {
                    tagRingStart.onClicked {
                        viewModel.onStartClicked()
                    }
                }
            }
            is State.Loading -> {
                tagRingFlipper.setDisplayedChildIfNeeded(0)
                tagRingStart.text = getString(R.string.tag_ring_start)
                tagRingStart.showProgress = true
                tagRingStart.setOnClickListener(null)
            }
            is State.RingingNetwork -> {
                tagRingFlipper.setDisplayedChildIfNeeded(1)
                tagRingLottieNetwork.progress = 0f
                tagRingLottieNetwork.playAnimation()
                whenResumed {
                    tagRingStopNetwork.onClicked {
                        viewModel.onStopClicked()
                    }
                }
            }
            is State.RingingBluetooth -> {
                tagRingFlipper.setDisplayedChildIfNeeded(2)
                tagRingLottieBluetooth.progress = 0f
                tagRingLottieBluetooth.playAnimation()
                tagRingVolumeUp.showProgress = state.sendingVolumeLevel == VolumeDirection.UP
                tagRingVolumeDown.showProgress = state.sendingVolumeLevel == VolumeDirection.DOWN
                val buttonsDisabled = state.sendingVolumeLevel != null
                tagRingVolumeUp.buttonEnabled = state.volumeLevel.next() != null && !buttonsDisabled
                tagRingVolumeDown.buttonEnabled =
                    state.volumeLevel.previous() != null && !buttonsDisabled
                whenResumed {
                    tagRingVolumeUp.onClicked {
                        if(!isEnabled) return@onClicked
                        viewModel.onVolumeUpClicked()
                    }
                }
                whenResumed {
                    tagRingVolumeDown.onClicked {
                        if(!isEnabled) return@onClicked
                        viewModel.onVolumeDownClicked()
                    }
                }
                whenResumed {
                    tagRingStopBluetooth.onClicked {
                        viewModel.onStopClicked()
                    }
                }
            }
        }
    }

}