package com.kieronquinn.app.utag.ui.activities

import android.app.Activity
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.xposed.extensions.EXTRA_TARGET_ID

class TagActivity: BaseActivity() {

    companion object {
        fun getTargetId(activity: Activity): String? {
            return activity.intent.getStringExtra(EXTRA_TARGET_ID)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_tag)
    }

}