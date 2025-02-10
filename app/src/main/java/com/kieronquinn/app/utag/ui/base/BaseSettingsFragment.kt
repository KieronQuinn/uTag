package com.kieronquinn.app.utag.ui.base

import android.animation.Animator
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.SeslProgressBar
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.utils.extensions.animateToProgress
import com.kieronquinn.app.utag.utils.preferences.bottomPaddingPreferenceCategory
import dev.oneuiproject.oneui.design.R as OneUIR


abstract class BaseSettingsFragment: PreferenceFragmentCompat() {

    companion object {
        private const val KEY_RECYCLERVIEW_POSITION = "recyclerview_position"
    }

    protected val recyclerView
        get() = view?.findViewById<RecyclerView>(OneUIR.id.recycler_view)

    private var savedPosition: Int? = null
    private var defaultItemAnimator: ItemAnimator? = DefaultItemAnimator()

    protected open val applyInsets = true
    protected open val clearPreferences = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        //No-op, dynamic
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Hack - clear stubborn preferences that insist on saving even when told not to
        if(clearPreferences) {
            preferenceManager.sharedPreferences?.edit()?.clear()?.apply()
        }
        val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
        view.findViewById<FrameLayout>(android.R.id.list_container).apply {
            setBackgroundColor(requireContext().getColor(R.color.oui_background_color))
            updateLayoutParams<FrameLayout.LayoutParams> {
                updateMargins(left = padding, right = padding)
            }
        }
        view.setBackgroundColor(requireContext().getColor(R.color.preferences_background))
        recyclerView?.isVerticalScrollBarEnabled = false
        view.findViewById<SeslProgressBar>(R.id.preference_list_loading_progress_bar)
            .setMode(SeslProgressBar.MODE_CIRCLE)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, v1: Int): Animation? {
        return null
    }

    override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        defaultItemAnimator = null
        preferenceScreen = null
    }

    protected fun setLoading(loading: Boolean, text: CharSequence? = null, progress: Int? = null) {
        val indeterminateLoading = loading && progress == null
        val progressLoading = loading && progress != null
        with(requireView()) {
            findViewById<View>(R.id.preference_list_loading).isVisible = indeterminateLoading
            findViewById<TextView>(R.id.preference_list_loading_text).run {
                isVisible = text != null
                this.text = text
            }
            findViewById<View>(R.id.preference_list_loading_progress).isVisible = progressLoading
            findViewById<TextView>(R.id.preference_list_loading_progress_text).run {
                isVisible = text != null
                this.text = text
            }
            if(progress != null) {
                findViewById<SeslProgressBar>(R.id.preference_list_loading_progress_bar)
                    .animateToProgress(progress)
            }
            findViewById<View>(android.R.id.list_container).isVisible = !loading
            findViewById<View>(android.R.id.empty).isVisible = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (recyclerView?.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()?.let {
            outState.putInt(KEY_RECYCLERVIEW_POSITION, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedPosition = savedInstanceState?.getInt(KEY_RECYCLERVIEW_POSITION, -1)
            ?.takeIf { it >= 0 }
    }

    protected fun setPreferences(additionalPadding: Int = 0, block: PreferenceScreen.() -> Unit) {
        val state = recyclerView?.layoutManager?.onSaveInstanceState()
        setOrUpdatePreferenceScreen(block, additionalPadding)
        setLoading(false)
        recyclerView?.post {
            savedPosition?.let {
                (recyclerView?.layoutManager as? LinearLayoutManager)?.scrollToPosition(it)
                savedPosition = null
            } ?: run {
                recyclerView?.layoutManager?.onRestoreInstanceState(state)
            }
        }
    }

    private fun setOrUpdatePreferenceScreen(
        block: PreferenceScreen.() -> Unit,
        additionalPadding: Int
    ) {
        //Re-use the existing screen if we can to keep Views
        val screen = preferenceScreen ?: preferenceManager.createPreferenceScreen(context).also {
            preferenceScreen = it
        }
        //If there's no item animator set, wait for first pass and enable it
        if(recyclerView?.itemAnimator == null) {
            recyclerView?.enableAnimatorAfterPass()
        }
        screen.removeAll()
        block(screen)
        if(applyInsets) {
            screen.bottomPaddingPreferenceCategory(requireView(), additionalPadding)
        }
    }

    /**
     *  Waits for layout pass calling [RecyclerView.AdapterDataObserver.onChanged] and then enables
     *  the item animator
     */
    private fun RecyclerView.enableAnimatorAfterPass() {
        val observer = object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                itemAnimator = defaultItemAnimator
                adapter?.unregisterAdapterDataObserver(this)
            }
        }
        adapter?.registerAdapterDataObserver(observer)
    }

}