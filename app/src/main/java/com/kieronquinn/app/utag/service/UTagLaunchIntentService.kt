package com.kieronquinn.app.utag.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.LifecycleService
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.xposed.extensions.applySecurity
import com.kieronquinn.app.utag.utils.extensions.dp
import com.kieronquinn.app.utag.utils.extensions.getCameraLaunchIntent
import com.kieronquinn.app.utag.xposed.extensions.getParcelableExtraCompat
import com.kieronquinn.app.utag.xposed.extensions.verifySecurity
import com.kieronquinn.app.utag.utils.tasker.TaskerIntent

/**
 *  This service has the sole intention of launching, adding an overlay (to satisfy the requirement
 *  to have an overlay on screen to launch from the background on newer Android versions), launching
 *  the intent then closing again. It's not a foreground service as it should be done almost
 *  immediately, and won't stay running in the background
 */
class UTagLaunchIntentService: LifecycleService() {

    companion object {
        private const val EXTRA_INTENT = "intent"
        const val ACTION_CAMERA = "${BuildConfig.APPLICATION_ID}.launch.CAMERA"

        private val WINDOW_MANAGER_FLAGS = arrayOf(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        fun startService(context: Context, intentToLaunch: Intent) {
            val intent = Intent(context, UTagLaunchIntentService::class.java).apply {
                putExtra(EXTRA_INTENT, intentToLaunch)
                applySecurity(context)
            }
            context.startService(intent)
        }

        private fun Array<Int>.or(): Int {
            var flag = 0
            forEach {
                flag = flag or it
            }
            return flag
        }
    }

    private val layoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WINDOW_MANAGER_FLAGS.or(),
            PixelFormat.TRANSLUCENT
        ).apply {
            width = 1.dp
            height = 1.dp
        }
    }

    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val keyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.verifySecurity(BuildConfig.APPLICATION_ID)
        val intentToLaunch = intent?.getParcelableExtraCompat(EXTRA_INTENT, Intent::class.java)
        if(intentToLaunch != null) {
            handleIntent(intentToLaunch)
        }
        stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }

    @Suppress("DEPRECATION")
    private fun handleIntent(intent: Intent) {
        //Add an overlay of 1x1 size so we can launch an intent
        val view = View(this)
        try {
            windowManager.addView(view, layoutParams)
        }catch (e: IllegalStateException){
            //Already added
        }
        //If this is a Tasker intent, send it as a Broadcast, otherwise launch the activity
        when (intent.action) {
            TaskerIntent.ACTION_TASK -> {
                sendBroadcast(intent)
            }
            ACTION_CAMERA -> {
                //Launch the relevant Camera intent
                try {
                    startActivity(getCameraLaunchIntent(keyguardManager.isKeyguardSecure))
                } catch (e: Exception) {
                    //Can't launch this intent anymore
                }
            }
            else -> {
                //Load the shortcut intent if this is a shortcut
                val intentToLaunch = intent.getParcelableExtraCompat(
                    Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java
                ) ?: intent
                intentToLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                //Launch the intent
                try {
                    startActivity(intentToLaunch)
                } catch (e: Exception) {
                    //Can't launch this intent anymore
                }
            }
        }
        //Now we can remove the overlay
        try {
            windowManager.removeViewImmediate(view)
        }catch (e: IllegalArgumentException){
            //Already removed
        }
    }

}