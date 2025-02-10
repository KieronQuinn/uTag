package com.kieronquinn.app.utag.repositories

import android.content.Intent
import android.os.IBinder
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.samsung.android.oneconnect.smarttag.service.ISmartTagSupportService

interface SmartTagServiceRepository: BaseSmartThingsServiceRepository<ISmartTagSupportService>

class SmartTagServiceRepositoryImpl(
    smartThingsRepository: SmartThingsRepository
): SmartTagServiceRepository, BaseSmartThingsServiceRepositoryImpl<ISmartTagSupportService>(smartThingsRepository) {

    companion object {
        private const val ACTION_START_SMARTTAG_SERVICE =
            "com.samsung.android.oneconnect.smarttag.START_SMARTTAG_SUPPORT_SERVICE"
    }

    override val serviceIntent = Intent(ACTION_START_SMARTTAG_SERVICE).apply {
        `package` = PACKAGE_NAME_ONECONNECT
    }

    override fun IBinder.asInterface(): ISmartTagSupportService {
        return ISmartTagSupportService.Stub.asInterface(this)
    }

}