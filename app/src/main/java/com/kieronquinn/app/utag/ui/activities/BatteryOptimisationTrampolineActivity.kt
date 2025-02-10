package com.kieronquinn.app.utag.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.kieronquinn.app.utag.service.UTagForegroundService
import com.kieronquinn.app.utag.utils.extensions.getIgnoreBatteryOptimisationsIntent
import com.kieronquinn.app.utag.xposed.extensions.applySecurity

class BatteryOptimisationTrampolineActivity: BaseActivity() {

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun createIntent(context: Context, `package`: String): Intent {
            return Intent(context, BatteryOptimisationTrampolineActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE, `package`)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
        }
    }

    private var hasResumed = false

    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val requiredPackage by lazy {
        intent.getStringExtra(EXTRA_PACKAGE)!!
    }

    @SuppressLint("BatteryLife")
    override fun onResume() {
        super.onResume()
        val isExcluded = powerManager.isIgnoringBatteryOptimizations(requiredPackage)
        if(isExcluded) {
            //Start the service now we're excluded
            if(!UTagForegroundService.startIfNeeded(this)) {
                //Already running, send a retry ping
                sendBroadcast(UTagForegroundService.getRetryIntent().apply {
                    applySecurity(this@BatteryOptimisationTrampolineActivity)
                })
            }
        }
        if(hasResumed || isExcluded) {
            //User has returned from prompt or the prompt is not required, close the trampoline
            finishAndRemoveTask()
        }else{
            //User has not seen the prompt yet, show it
            startActivity(getIgnoreBatteryOptimisationsIntent(requiredPackage))
        }
        hasResumed = true
    }

}