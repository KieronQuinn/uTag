package androidx.preference

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import dev.oneuiproject.oneui.design.R

@SuppressLint("RestrictedApi")
class UTagLayoutPreference(
    context: Context,
    view: View,
    private val key: String
) : UTagPreference(context) {

    private var mRootView: View? = null

    private var mAllowDividerAbove = false
    private var mAllowDividerBelow = false

    private val mClickListener =
        View.OnClickListener { view: View? -> this.performClick(view) }

    init {
        setView(view)
    }

    private fun setView(view: View) {
        layoutResource = R.layout.oui_preference_layout_frame
        mRootView = view
        shouldDisableView = false
    }

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        view.itemView.setOnClickListener(mClickListener)

        val isSelectable = isSelectable
        view.itemView.isFocusable = isSelectable
        view.itemView.isClickable = isSelectable

        view.isDividerAllowedAbove = mAllowDividerAbove
        view.isDividerAllowedBelow = mAllowDividerBelow

        val layout = view.itemView as FrameLayout
        layout.removeAllViews()
        val parent = mRootView?.parent as? ViewGroup
        parent?.removeView(mRootView)
        layout.addView(mRootView)
    }

    fun <T : View?> findViewById(id: Int): T {
        return mRootView!!.findViewById(id)
    }

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}
