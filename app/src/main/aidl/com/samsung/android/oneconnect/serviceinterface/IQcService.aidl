package com.samsung.android.oneconnect.serviceinterface;

interface IQcService {
    void startDiscovery(int scanType, int id, boolean flush, boolean showExceptionCaseMsg) = 5;
    void stopDiscovery(int id, boolean isUiStopped) = 6;
    void forceStopDiscovery(int id) = 7;
}