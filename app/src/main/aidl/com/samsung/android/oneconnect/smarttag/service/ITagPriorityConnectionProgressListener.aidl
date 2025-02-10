package com.samsung.android.oneconnect.smarttag.service;

interface ITagPriorityConnectionProgressListener {
    void onError(int errorCode, in String errString);
    void onSucceeded();
}