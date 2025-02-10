package com.kieronquinn.app.utag.ui.activities

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.workers.UpdateWorker

class MainActivity : BaseActivity() {

    companion object {
        const val EXTRA_SKIP_SPLASH = "skip_splash"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        UpdateWorker.queueCheckWorker(this)
    }

}