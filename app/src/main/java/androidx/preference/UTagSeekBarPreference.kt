package androidx.preference

import android.content.Context
import androidx.appcompat.widget.SeslProgressBar.SeekBarMode
import androidx.appcompat.widget.SeslSeekBar
import dev.oneuiproject.oneui.utils.SeekBarUtils
import java.lang.ref.WeakReference

class UTagSeekBarPreference(context: Context): SeekBarPreference(context), SeekBarPreference.OnSeekBarPreferenceChangeListener {

    var onFocusChanged: (Boolean) -> Unit = {}

    var isStepped: Boolean = false
        set(value) {
            field = value
            updateStepped()
        }

    var description: CharSequence? = null
        set(value) {
            field = value
            updateDescription()
        }

    @SeekBarMode
    var seekBarMode: Int = SeslSeekBar.MODE_EXPAND
        set(value) {
            field = value
            updateMode()
        }

    private var seekBar: WeakReference<SeslSeekBar>? = null

    override fun getId(): Long {
        return key.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        seekBar = WeakReference(holder.findViewById(R.id.seekbar) as SeslSeekBar)
        updateStepped()
        updateMode()
    }

    private fun updateStepped() {
        seekBar?.get()?.let {
            val enabled = isStepped
            SeekBarUtils.showTickMark(it, enabled)
        }
    }

    private fun updateMode() {
        seekBar?.get()?.setMode(seekBarMode)
    }

    private fun updateDescription() {
        seekBar?.get()?.contentDescription = description
    }

    override fun onProgressChanged(seekBar: SeslSeekBar, progress: Int, fromUser: Boolean) {
        //No-op
    }

    override fun onStartTrackingTouch(seekBar: SeslSeekBar) {
        onFocusChanged(true)
    }

    override fun onStopTrackingTouch(seekBar: SeslSeekBar) {
        onFocusChanged(false)
    }

    init {
        setOnSeekBarPreferenceChangeListener(this)
    }

}