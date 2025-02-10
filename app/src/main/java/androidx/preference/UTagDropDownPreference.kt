package androidx.preference

import android.content.Context

class UTagDropDownPreference(context: Context): DropDownPreference(context) {

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}