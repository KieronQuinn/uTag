package androidx.preference

import android.content.Context

class UTagMultiSelectListPreference(context: Context) : MultiSelectListPreference(context) {

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}