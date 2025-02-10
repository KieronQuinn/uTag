package androidx.preference

import android.content.Context

open class UTagCheckBoxPreference(context: Context): CheckBoxPreference(context) {

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}