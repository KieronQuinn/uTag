package com.kieronquinn.app.utag.ui.base

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.utag.utils.extensions.isDarkMode
import com.kieronquinn.app.utag.utils.extensions.whenResumed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

abstract class BoundFragment<V: ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): Fragment() {

    private var _binding: V? = null

    protected val binding: V
        get() = _binding ?: throw NullPointerException("Unable to access binding before onCreateView or after onDestroyView")

    protected val statusNavDarkOverride = MutableStateFlow<Boolean?>(null)

    private val insetsController by lazy {
        val window = requireActivity().window
        WindowCompat.getInsetsController(window, window.decorView)
    }

    open val applyTransitions = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        whenResumed {
            statusNavDarkOverride.filterNotNull().collect {
                setStatusNavColour(it)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //Reset the colour for fragments which are not themed
        setStatusNavColour()
    }

    private fun setStatusNavColour(dark: Boolean = requireContext().isDarkMode) {
        insetsController?.isAppearanceLightStatusBars = !dark
        insetsController?.isAppearanceLightNavigationBars = !dark
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, v1: Int): Animation? {
        return null
    }

    override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
        return null
    }

}