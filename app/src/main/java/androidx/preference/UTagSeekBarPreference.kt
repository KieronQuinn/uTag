package androidx.preference

import android.content.Context
import androidx.appcompat.widget.SeslSeekBar

class UTagSeekBarPreference(context: Context): SeekBarPreference(context), SeekBarPreference.OnSeekBarPreferenceChangeListener {

    var onFocusChanged: (Boolean) -> Unit = {}

    override fun getId(): Long {
        return key.hashCode().toLong()
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