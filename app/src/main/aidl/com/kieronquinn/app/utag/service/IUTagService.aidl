package com.kieronquinn.app.utag.service;

import android.content.Intent;
import com.kieronquinn.app.utag.service.callback.ITagStateCallback;
import com.kieronquinn.app.utag.service.callback.ITagStatusCallback;
import com.kieronquinn.app.utag.service.callback.IBooleanCallback;
import com.kieronquinn.app.utag.service.callback.IStringCallback;
import com.kieronquinn.app.utag.service.callback.ITagAutoSyncLocationCallback;
import com.kieronquinn.app.utag.service.callback.ITagConnectResultCallback;
import com.samsung.android.oneconnect.smarttag.service.IGattRssiCallback;
import com.samsung.android.oneconnect.feature.blething.tag.priority.TagPriorityConnectionOptions;
import com.samsung.android.oneconnect.smarttag.service.ITagPriorityConnectionProgressListener;

interface IUTagService {
    String addTagStateCallback(in ITagStateCallback callback);
    void removeTagStateCallback(in String callbackId);
    String addTagStatusCallback(in ITagStatusCallback callback);
    void removeTagStatusCallback(in String callbackId);
    String addAutoSyncLocationCallback(in ITagAutoSyncLocationCallback callback);
    void removeAutoSyncLocationCallback(in String callbackId);
    String addTagConnectResultCallback(in ITagConnectResultCallback callback);
    void removeTagConnectResultCallback(in String callbackId);
    void onLocationPermissionsChanged();
    void onGeofenceIntentReceived(in Intent intent);

    void startTagScanNow(long timeout);
    void syncLocation(in String deviceId, boolean onDemand, in IStringCallback callback);
    void getTagBatteryLevel(in String deviceId, in IStringCallback callback);
    void startTagRinging(in String deviceId, in IStringCallback callback);
    void setTagRingVolume(in String deviceId, in String volumeLevel, in IBooleanCallback callback);
    void stopTagRinging(in String deviceId, in IBooleanCallback callback);
    void startTagRanging(in String deviceId, in byte[] config, in IBooleanCallback callback);
    void stopTagRanging(in String deviceId, in IBooleanCallback callback);
    void setButtonConfig(in String deviceId, boolean pressEnabled, boolean holdEnabled, in IBooleanCallback callback);
    void getE2EEnabled(in String deviceId, in IBooleanCallback callback);
    void setE2EEnabled(in String deviceId, boolean enabled, in IBooleanCallback callback);
    void getButtonVolumeLevel(in String deviceId, in IStringCallback callback);
    void setButtonVolumeLevel(in String deviceId, in String volumeLevel, in IBooleanCallback callback);
    void getLostModeUrl(in String deviceId, in IStringCallback callback);
    void setLostModeUrl(in String deviceId, in String url, in IBooleanCallback callback);
    boolean startTagReadRssi(in String deviceId, long refreshRate, in IGattRssiCallback callback);
    boolean stopTagReadRssi(in String deviceId);

    void killProcess();
}