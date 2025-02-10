package androidx.preference

import android.content.Context

class UTagEditTextPreference(context: Context): EditTextPreference(context) {

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}