package com.kieronquinn.app.utag.service.callback;

interface ITagAutoSyncLocationCallback {
    void onStartSync(in String deviceId);
    void onSyncFinished(in String deviceId, in String result);
}