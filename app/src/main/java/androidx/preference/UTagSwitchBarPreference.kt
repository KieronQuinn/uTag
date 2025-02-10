package androidx.preference

import android.content.Context
import androidx.appcompat.widget.SeslSwitchBar
import androidx.appcompat.widget.SwitchCompat
import dev.oneuiproject.oneui.design.R

class UTagSwitchBarPreference(context: Context): TwoStatePreference(context) {

    init {
        layoutResource = R.layout.oui_preference_switch_bar_layout
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val switchBar = holder.itemView as SeslSwitchBar
        switchBar.isChecked = mChecked
        switchBar.addOnSwitchChangeListener { switchView: SwitchCompat?, isChecked: Boolean ->
            if (isChecked == mChecked) return@addOnSwitchChangeListener
            if (!callChangeListener(isChecked)) {
                switchBar.isChecked = !isChecked
                return@addOnSwitchChangeListener
            }
            setChecked(isChecked)
        }
        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
    }

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

}