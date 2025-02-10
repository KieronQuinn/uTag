package com.kieronquinn.app.utag.repositories

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.utag.providers.HistorySmartspacerTargetProvider
import com.kieronquinn.app.utag.providers.LocationSmartspacerTargetProvider
import com.kieronquinn.app.utag.repositories.SmartspacerRepository.TargetData

interface SmartspacerRepository {

    fun getTargetData(smartspacerId: String): TargetData?
    fun setTargetData(smartspacerId: String, targetData: TargetData?, isHistory: Boolean)

    data class TargetData(
        val title: String,
        val subtitle: String,
        val image: Bitmap?,
        val onClick: PendingIntent,
        val remoteViews: RemoteViews
    )

}

class SmartspacerRepositoryImpl(private val context: Context): SmartspacerRepository {

    private val targetData = HashMap<String, TargetData>()

    override fun getTargetData(smartspacerId: String): TargetData? {
        return targetData[smartspacerId]
    }

    override fun setTargetData(smartspacerId: String, targetData: TargetData?, isHistory: Boolean) {
        if(targetData != null) {
            this.targetData[smartspacerId] = targetData
        }else{
            this.targetData.remove(smartspacerId)
        }
        if(isHistory) {
            SmartspacerTargetProvider.notifyChange(
                context,
                HistorySmartspacerTargetProvider::class.java
            )
        }else{
            SmartspacerTargetProvider.notifyChange(
                context,
                LocationSmartspacerTargetProvider::class.java
            )
        }
    }

}