package com.kieronquinn.app.utag.ui.activities

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.kieronquinn.app.utag.R

class UnknownTagActivity: BaseActivity() {

    companion object {
        fun isStandalone(fragment: Fragment): Boolean {
            return fragment.activity is UnknownTagActivity
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_unknown_tag)
    }

}