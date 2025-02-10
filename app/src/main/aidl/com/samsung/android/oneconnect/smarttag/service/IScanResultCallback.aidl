package com.samsung.android.oneconnect.smarttag.service;

interface IScanResultCallback {
    void onScanResult(
        in String deviceId,
        int batteryLevel,
        in String bleMac,
        int uwbFlag,
        int advertisementType,
        int motionDetection
    );
}