package androidx.preference

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets

class UTagBottomPaddingPreferenceCategory(
    context: Context,
    private val rootView: View,
    private val additionalPadding: Int = 0
): PreferenceCategory(context) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val extraPadding = context.resources.getDimensionPixelSize(R.dimen.margin_8)
        val currentInsets = ViewCompat.getRootWindowInsets(rootView)
            ?.getInsets(SYSTEM_INSETS)?.bottom ?: 0
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            height = currentInsets + currentInsets + additionalPadding
        }
        holder.itemView.layoutParams = layoutParams
        holder.itemView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        rootView.onApplyInsets { _, insets ->
            val bottomPadding = insets.getInsets(SYSTEM_INSETS).bottom
            holder.itemView.updateLayoutParams<LayoutParams> {
                height = bottomPadding + extraPadding + additionalPadding
            }
        }
    }

    override fun getId(): Long {
        return "bottom_padding".hashCode().toLong()
    }

}