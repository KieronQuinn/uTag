package com.kieronquinn.app.utag.repositories

import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.samsung.android.oneconnect.serviceinterface.IQcService

interface QcServiceRepository: BaseSmartThingsServiceRepository<IQcService>

class QcServiceRepositoryImpl(
    smartThingsRepository: SmartThingsRepository
): QcServiceRepository, BaseSmartThingsServiceRepositoryImpl<IQcService>(smartThingsRepository) {

    companion object {
        private const val CLASS_QC_SERVICE =
            "com.samsung.android.oneconnect.core.QcService"
    }

    override val serviceIntent = Intent().apply {
        component = ComponentName(PACKAGE_NAME_ONECONNECT, CLASS_QC_SERVICE)
        putExtra("CALLER", "PLUGIN")
    }

    override fun IBinder.asInterface(): IQcService {
        return IQcService.Stub.asInterface(this)
    }

}