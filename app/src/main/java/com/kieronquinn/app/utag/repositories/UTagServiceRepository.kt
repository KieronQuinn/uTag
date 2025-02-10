package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.kieronquinn.app.utag.service.IUTagService
import com.kieronquinn.app.utag.service.UTagForegroundService
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper.RoomEncryptionFailedCallback

interface UTagServiceRepository: BaseServiceRepository<IUTagService>, RoomEncryptionFailedCallback

class UTagServiceRepositoryImpl(
    private val context: Context
): UTagServiceRepository, BaseServiceRepositoryImpl<IUTagService>(context) {

    override val serviceIntent = Intent(context, UTagForegroundService::class.java)

    override fun IBinder.asInterface(): IUTagService {
        return IUTagService.Stub.asInterface(this)
    }

    override fun onEncryptionFailed() {
        //Key & IV have changed, forcibly restart the service
        runWithServiceIfAvailable {
            it.killProcess()
        }
        UTagForegroundService.startIfNeeded(context)
    }

}