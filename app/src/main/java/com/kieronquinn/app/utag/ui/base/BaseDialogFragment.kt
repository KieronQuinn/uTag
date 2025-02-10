package com.kieronquinn.app.utag.ui.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets

abstract class BaseDialogFragment<T: ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T): DialogFragment() {

    internal val binding: T
        get() = _binding ?: throw NullPointerException("Cannot access binding before onCreate or after onDestroy")

    private var _binding: T? = null

    open val isDismissable = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflate.invoke(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setCancelable(isDismissable)
    }

    override fun onStart() {
        super.onStart()
        requireDialog().window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.attributes.gravity = Gravity.BOTTOM
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun setupInsets() = with(binding) {
        root.onApplyInsets { _, insets ->
            val systemInsets = insets.getInsets(SYSTEM_INSETS)
            root.updateLayoutParams<MarginLayoutParams> {
                updateMargins(
                    top = systemInsets.top,
                    bottom = systemInsets.bottom,
                    left = systemInsets.left,
                    right = systemInsets.right
                )
            }
        }
    }

}