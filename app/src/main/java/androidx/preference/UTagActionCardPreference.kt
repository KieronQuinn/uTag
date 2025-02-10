package androidx.preference

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import dev.oneuiproject.oneui.design.R
import com.kieronquinn.app.utag.R as UtagR

class UTagActionCardPreference(context: Context): UTagTipsCardPreference(context) {

    @ColorInt
    var actionTextColour: Int = ContextCompat.getColor(context, UtagR.color.oui_accent_color)

    override fun onBindViewHolder(preferenceViewHolder: PreferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder)
        val bottomBar = preferenceViewHolder.itemView
            .findViewById<LinearLayout>(R.id.tips_bottom_bar)
        bottomBar.updateLayoutParams<LinearLayout.LayoutParams> {
            gravity = Gravity.START
        }
        bottomBar.children.filterIsInstance<TextView>().forEach {
            it.setTextColor(actionTextColour)
        }
    }

}