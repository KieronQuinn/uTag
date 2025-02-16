package com.kieronquinn.app.utag.ui.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.kieronquinn.app.utag.databinding.ActivityFindMyDeviceBinding
import com.kieronquinn.app.utag.xposed.extensions.getParcelableExtraCompat
import kotlinx.parcelize.Parcelize

class FindMyDeviceActivity: BoundActivity<ActivityFindMyDeviceBinding>(ActivityFindMyDeviceBinding::inflate) {

    companion object {
        private const val EXTRA_CONFIG = "config"

        fun createIntent(context: Context, config: FindMyDeviceActivityConfig): Intent {
            return Intent(context, FindMyDeviceActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_CONFIG, config)
            }
        }

        fun getConfig(activity: Activity): FindMyDeviceActivityConfig {
            return activity.intent?.getParcelableExtraCompat(
                EXTRA_CONFIG, FindMyDeviceActivityConfig::class.java
            ) ?: throw RuntimeException("Config was not provided")
        }
    }

    @Parcelize
    data class FindMyDeviceActivityConfig(
        val deviceLabel: String?,
        val startTime: Long,
        val endTime: Long,
        val delayTime: Long,
        val timeout: Long
    ): Parcelable

}