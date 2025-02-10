package androidx.preference

import android.content.Context
import android.widget.RadioButton
import com.kieronquinn.app.utag.R

class UTagRadioButtonPreference(context: Context): Preference(context) {

    init {
        widgetLayoutResource = R.layout.preference_widget_radiobutton
    }

    private var holder: PreferenceViewHolder? = null
    private var checked: Boolean? = null

    var isChecked: Boolean
        get() = checked ?: false
        set(value) {
            checked = value
            syncView()
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        this.holder = holder
        checked?.let {
            isChecked = it
        }
    }

    private fun syncView() {
        holder?.findViewById(android.R.id.checkbox)?.let {
            it as RadioButton
            it.isChecked = isChecked
        }
    }

    override fun onClick() {
        isChecked = true
    }

}