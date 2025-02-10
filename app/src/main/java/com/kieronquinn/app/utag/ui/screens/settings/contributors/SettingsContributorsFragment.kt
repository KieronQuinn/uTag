package com.kieronquinn.app.utag.ui.screens.settings.contributors

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preference

class SettingsContributorsFragment: BaseSettingsFragment(), BackAvailable {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPreferences {
            preference {
                title = getString(R.string.contributors_oneui_title)
                summary = getString(R.string.contributors_oneui_content)
                onClick {
                    openUrl("https://github.com/OneUIProject")
                }
            }
            preference {
                title = getString(R.string.contributors_icons_title)
                summary = getString(R.string.contributors_icons_content)
                onClick {
                    openUrl("https://github.com/Templarian/MaterialDesign")
                }
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        })
    }

}