package androidx.preference

import android.content.Context
import android.view.View
import android.widget.LinearLayout

class UTagSpacerPreferenceCategory(
    context: Context,
    private val height: Int = 0,
    private val key: String
): PreferenceCategory(context) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            height = this@UTagSpacerPreferenceCategory.height
        }
        holder.itemView.layoutParams = layoutParams
        holder.itemView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}