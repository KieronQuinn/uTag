package com.samsung.android.oneconnect.smarttag.service;

import com.samsung.android.oneconnect.feature.blething.tag.gatt.GattControlServiceState;

interface IGattCharacteristicChangedCallback {
    void onControlServiceChanged(in GattControlServiceState gattControlServiceState);
}