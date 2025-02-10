package com.samsung.android.oneconnect.smarttag.service;

interface IGattConnectionCallback {
    void onConnectionStateChange(in String deviceId, int state);
}