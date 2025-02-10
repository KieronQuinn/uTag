package androidx.preference

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.kieronquinn.app.utag.utils.extensions.fade

class UTagGlideCheckBoxPreference(context: Context): UTagCheckBoxPreference(context) {

    private val glide = Glide.with(context)

    var url: String? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val icon = holder.findViewById(android.R.id.icon) as ImageView
        val imageFrame = holder.findViewById(R.id.icon_frame) as View
        if(url != null) {
            icon.visibility = View.VISIBLE
            imageFrame.visibility = View.VISIBLE
            glide.load(url).fade().into(icon)
        }else{
            icon.visibility = View.GONE
            imageFrame.visibility = View.GONE
        }
    }

}