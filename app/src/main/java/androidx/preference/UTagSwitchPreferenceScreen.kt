package androidx.preference

import android.content.Context

class UTagSwitchPreferenceScreen(context: Context): SeslSwitchPreferenceScreen(context) {

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}