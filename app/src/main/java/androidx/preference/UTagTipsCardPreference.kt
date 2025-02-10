package androidx.preference

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import dev.oneuiproject.oneui.design.R

open class UTagTipsCardPreference(private val mContext: Context) : UTagPreference(mContext) {

    @ColorInt
    private val mTextColor: Int
    private var mCancelBtnOCL: View.OnClickListener? = null
    private val mBottomBarBtns = ArrayList<TextView>()

    private var mItemView: View? = null
    private var mTitleContainer: RelativeLayout? = null
    private var mCancelButton: AppCompatImageView? = null
    private var mEmptyBottom: View? = null
    private var mBottomBar: LinearLayout? = null

    init {
        isSelectable = false
        layoutResource = R.layout.oui_preference_tips_layout
        mTextColor = ContextCompat.getColor(mContext, R.color.oui_primary_text_color)
    }

    override fun onBindViewHolder(preferenceViewHolder: PreferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder)
        mItemView = preferenceViewHolder.itemView

        // workaround since we can't use setSelectable here
        if (onPreferenceClickListener != null) {
            mItemView!!.setOnClickListener { v: View? ->
                onPreferenceClickListener
                    .onPreferenceClick(this)
            }
        }

        (mItemView!!.findViewById<View>(android.R.id.title) as TextView).setTextColor(mTextColor)
        (mItemView!!.findViewById<View>(android.R.id.summary) as TextView).setTextColor(mTextColor)

        mTitleContainer = mItemView!!.findViewById(R.id.tips_title_container)
        mCancelButton = mItemView!!.findViewById(R.id.tips_cancel_button)
        mEmptyBottom = mItemView!!.findViewById(R.id.tips_empty_bottom)
        mBottomBar = mItemView!!.findViewById(R.id.tips_bottom_bar)

        if (!TextUtils.isEmpty(title)) {
            mTitleContainer!!.visibility = View.VISIBLE
        }

        if (mCancelBtnOCL != null) {
            mCancelButton!!.setVisibility(View.VISIBLE)
            mCancelButton!!.setOnClickListener(mCancelBtnOCL)
        }

        if (mBottomBarBtns.size > 0) {
            mBottomBar!!.setVisibility(View.VISIBLE)
            mBottomBar!!.removeAllViews()
            (mItemView as ViewGroup).removeView(mEmptyBottom)
            mEmptyBottom = null

            for (txtView in mBottomBarBtns) {
                mBottomBar!!.addView(txtView)
            }
            mBottomBarBtns.clear()
        }
    }

    fun addButton(text: CharSequence?, listener: View.OnClickListener?): TextView {
        val txtView = TextView(
            mContext, null, 0,
            R.style.OneUI_TipsCardTextButtonStyle
        )
        txtView.text = text
        txtView.setOnClickListener(listener)

        if (mBottomBar != null) {
            mBottomBar!!.visibility = View.VISIBLE
            if (mEmptyBottom != null) {
                (mItemView as ViewGroup).removeView(mEmptyBottom)
                mEmptyBottom = null
            }
            mBottomBar!!.addView(txtView)
        } else {
            mBottomBarBtns.add(txtView)
        }

        return txtView
    }

    fun setOnCancelClickListener(listener: View.OnClickListener?) {
        mCancelBtnOCL = listener
        if (mCancelButton != null) {
            mCancelButton!!.visibility = if (mCancelBtnOCL == null)
                View.GONE
            else
                View.VISIBLE
            mCancelButton!!.setOnClickListener(mCancelBtnOCL)
        }
    }
}
