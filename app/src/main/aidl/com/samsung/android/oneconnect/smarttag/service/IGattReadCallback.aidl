package com.samsung.android.oneconnect.smarttag.service;

interface IGattReadCallback {
    void onCharacteristicRead(in String characteristics, in String value);
}