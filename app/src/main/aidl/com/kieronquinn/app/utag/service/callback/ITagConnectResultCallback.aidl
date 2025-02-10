package com.kieronquinn.app.utag.service.callback;

import com.samsung.android.oneconnect.base.device.tag.TagConnectionState;

interface ITagConnectResultCallback {
    void onTagConnectResult(in String deviceId, in TagConnectionState state);
}