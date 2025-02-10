package com.kieronquinn.app.utag.ui.activities

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.view.WindowCompat
import com.kieronquinn.app.utag.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthResponseActivity: BaseActivity() {

    companion object: KoinComponent {
        private val context by inject<Context>()

        fun setEnabled(enabled: Boolean) {
            val component = ComponentName(context, AuthResponseActivity::class.java)
            val state = if(enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }else{
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                component, state, PackageManager.DONT_KILL_APP
            )
        }
    }

    override val disableAuth = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_auth_response)
    }

}