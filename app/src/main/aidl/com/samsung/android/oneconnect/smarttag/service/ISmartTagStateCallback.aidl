package com.samsung.android.oneconnect.smarttag.service;

interface ISmartTagStateCallback {
    void onSmartTagStateChanged(in String deviceId, int state);
}