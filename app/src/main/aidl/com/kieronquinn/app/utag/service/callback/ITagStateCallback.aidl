package com.kieronquinn.app.utag.service.callback;

interface ITagStateCallback {
    void onConnectedTagsChanged(in String[] connectedDeviceIds, in String[] scannedDeviceIds);
}