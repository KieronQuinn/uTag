package com.kieronquinn.app.utag.ui.screens.safearea.location

import android.os.Bundle
import android.text.InputType
import android.view.View
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.ExitBuffer
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.ui.screens.safearea.location.SafeAreaLocationViewModel.State
import com.kieronquinn.app.utag.utils.extensions.isLandscape
import com.kieronquinn.app.utag.utils.extensions.setTextToEnd
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import com.kieronquinn.app.utag.utils.preferences.editTextPreference
import com.kieronquinn.app.utag.utils.preferences.listPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.seekbarPreference
import com.kieronquinn.app.utag.utils.preferences.spacerPreferenceCategory
import kotlin.math.roundToInt

class SafeAreaLocationBottomSheetFragment: BaseSettingsFragment() {

    companion object {
        private const val MIN_RADIUS = 10
        private const val MAX_RADIUS = 1000
        private const val DIFF_RADIUS = MAX_RADIUS - MIN_RADIUS
    }
    
    private val viewModel by lazy {
        SafeAreaLocationFragment.getParentViewModel(parentFragment!!)
    }

    private val margin by lazy {
        resources.getDimensionPixelSize(R.dimen.margin_16)
    }

    private var contentKey: Int? = null
    override val clearPreferences = true

    override val applyInsets by lazy {
        !requireContext().isLandscape()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        recyclerView?.isNestedScrollingEnabled = false
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
            is State.Loading -> setLoading(true)
            is State.Loaded -> handleContent(state)
            else -> {
                //No-op, handled by parent
            }
        }
    }
    
    private fun handleContent(state: State.Loaded) {
        //If the content has not changed, excluding the radius or map options, ignore
        val contentKey = state.copy(radius = 0f, mapOptions = null).hashCode()
        if(this.contentKey == contentKey) return
        this.contentKey = contentKey
        setPreferences {
            spacerPreferenceCategory(margin, "top_padding")
            editTextPreference {
                title = getString(R.string.safe_area_location_name_title)
                summary = state.name ?: getString(R.string.safe_area_location_name_content_empty)
                dialogTitle = getString(R.string.safe_area_location_name_title)
                dialogMessage = getString(R.string.safe_area_location_name_dialog_message)
                setOnBindEditTextListener {
                    it.setTextToEnd(state.name)
                    it.inputType = InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                            InputType.TYPE_TEXT_FLAG_CAP_WORDS
                }
                onChange<String> {
                    viewModel.onNameChanged(it.trim())
                }
            }
            seekbarPreference {
                title = getString(R.string.safe_area_location_radius_title)
                summary = getString(R.string.safe_area_location_radius_content)
                min = 0
                max = 100
                value = (((state.radius / DIFF_RADIUS.toFloat()) * 100).roundToInt())
                updatesContinuously = false
                onChange<Int> {
                    viewModel.onRadiusChanged(((it / 100f) * DIFF_RADIUS) + MIN_RADIUS)
                    value = it
                }
            }
            listPreference {
                title = getString(R.string.safe_area_location_exit_buffer_title)
                dialogTitle = getString(R.string.safe_area_location_exit_buffer_title)
                summary = getString(
                    R.string.safe_area_location_exit_buffer_content,
                    getString(state.exitBuffer.label)
                )
                entries = ExitBuffer.entries.map { getString(it.label) }.toTypedArray()
                entryValues = ExitBuffer.entries.map { it.name }.toTypedArray()
                value = state.exitBuffer.name
                onChange<String> {
                    viewModel.onExitBufferChanged(ExitBuffer.valueOf(it))
                }
            }
            if(!applyInsets) {
                spacerPreferenceCategory(margin, "bottom_padding")
            }
        }
    }

}