package androidx.preference

import android.content.Context

class UTagListPreference(context: Context): ListPreference(context) {

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}