package com.samsung.android.oneconnect.smarttag.service;

interface IGattDescriptorWriteCallback {
    void onDescriptorWrite(in String stateJson, int isSuccess);
}