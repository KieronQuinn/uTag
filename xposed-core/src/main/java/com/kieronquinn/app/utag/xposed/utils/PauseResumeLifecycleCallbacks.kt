package com.kieronquinn.app.utag.xposed.utils

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle

abstract class PauseResumeLifecycleCallbacks: ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        //No-op
    }

    override fun onActivityStarted(activity: Activity) {
        //No-op
    }

    override fun onActivityStopped(activity: Activity) {
    }
    //No-op

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        //No-op
    }

    override fun onActivityDestroyed(activity: Activity) {
        //No-op
    }
}