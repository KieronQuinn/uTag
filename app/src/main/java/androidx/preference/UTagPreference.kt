package androidx.preference

import android.content.Context
import android.view.View.OnLongClickListener

open class UTagPreference(context: Context): Preference(context) {

    var longClickListener: OnLongClickListener? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        if(longClickListener != null) {
            holder.itemView.setOnLongClickListener(longClickListener)
        }
    }

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}