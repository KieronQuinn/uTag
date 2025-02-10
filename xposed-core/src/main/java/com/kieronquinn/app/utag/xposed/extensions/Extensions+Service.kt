package com.kieronquinn.app.utag.xposed.extensions

import android.os.IInterface
import android.os.RemoteException

fun <T, I: IInterface> I.runSafely(block: I.() -> T): T? {
    return try {
        block(this)
    }catch (e: RemoteException) {
        null
    }
}