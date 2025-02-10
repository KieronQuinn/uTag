package com.samsung.android.oneconnect.smarttag.service;

interface ITagPriorityConnectionStateCallback {
    //Not actually in APK, guessing
    void onTagPriorityConnectionStateChanged(in String deviceId, in String state);
}