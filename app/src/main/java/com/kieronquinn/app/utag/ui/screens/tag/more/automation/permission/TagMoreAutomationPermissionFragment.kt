package com.kieronquinn.app.utag.ui.screens.tag.more.automation.permission

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BaseSettingsFragment
import com.kieronquinn.app.utag.utils.extensions.showAppInfo
import com.kieronquinn.app.utag.utils.preferences.actionCardPreference
import com.kieronquinn.app.utag.utils.preferences.onChange
import com.kieronquinn.app.utag.utils.preferences.onClick
import com.kieronquinn.app.utag.utils.preferences.preferenceCategory
import com.kieronquinn.app.utag.utils.preferences.switchPreference
import org.koin.androidx.viewmodel.ext.android.viewModel

class TagMoreAutomationPermissionFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<TagMoreAutomationPermissionViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPreferences {
            switchPreference {
                title = getString(R.string.tag_more_automation_permission_allow_title)
                summary = getString(R.string.tag_more_automation_permission_allow_content)
                onChange<Boolean> {
                    //No-op
                }
                onClick {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                }
            }
            preferenceCategory("automation_permission_app_footer") {
                actionCardPreference {
                    title = getString(R.string.tag_more_automation_permission_restricted_title)
                    summary = getText(R.string.tag_more_automation_permission_restricted_content)
                    addButton(getString(R.string.tag_more_automation_permission_restricted_action)) {
                        requireContext().showAppInfo()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

}