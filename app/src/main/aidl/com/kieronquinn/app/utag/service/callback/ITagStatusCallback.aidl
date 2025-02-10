package com.kieronquinn.app.utag.service.callback;

import com.kieronquinn.app.utag.model.TagStatusChangeEvent;

interface ITagStatusCallback {
    void onTagStatusChanged(in String deviceId, in TagStatusChangeEvent status);
}