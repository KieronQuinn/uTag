package androidx.preference

import android.content.Context

class UTagPreferenceCategory(context: Context): PreferenceCategory(context) {

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}