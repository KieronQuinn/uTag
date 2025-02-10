package com.samsung.android.oneconnect.smarttag.service;

import com.samsung.android.oneconnect.smarttag.service.ISmartTagStateCallback;
import com.samsung.android.oneconnect.smarttag.service.IGattConnectionCallback;
import com.samsung.android.oneconnect.smarttag.service.IGattWriteCallback;
import com.samsung.android.oneconnect.smarttag.service.IGattCharacteristicChangedCallback;
import com.samsung.android.oneconnect.smarttag.service.IGattReadCallback;
import com.samsung.android.oneconnect.smarttag.service.IGattDescriptorWriteCallback;
import com.samsung.android.oneconnect.smarttag.service.ITagPriorityConnectionProgressListener;
import com.samsung.android.oneconnect.smarttag.service.ITagPriorityConnectionStateCallback;
import com.samsung.android.oneconnect.smarttag.service.IGattRssiCallback;
import com.samsung.android.oneconnect.smarttag.service.IScanResultCallback;
import com.samsung.android.oneconnect.feature.blething.tag.priority.TagPriorityConnectionOptions;
import com.samsung.android.oneconnect.base.device.tag.TagConnectionState;

interface ISmartTagSupportService {
    void setClientForeground(in String appName, int foreground); //1
    void registerSmartTagStateCallback(in ISmartTagStateCallback callback); //2
    void unregisterSmartTagStateCallback(in ISmartTagStateCallback callback); //3
    //1 = d2dConnected, 2 = d2dScanned, 3 = default
    int getSmartTagState(in String deviceId); //4
    TagConnectionState connect(in String deviceId); //5
    int disconnect(in String deviceId); //6
    void registerGattConnectionCallback(in IGattConnectionCallback callback); //7
    void unregisterGattConnectionCallback(in IGattConnectionCallback callback); //8
    int writeCharacteristic( //9
        in String deviceId,
        in String serviceId,
        in String characteristicId,
        in String valueAsHex,
        in IGattWriteCallback callback
    );
    void registerCharacteristicChangedCallback( //10
        in String deviceId,
        in IGattCharacteristicChangedCallback callback
    );
    void unregisterCharacteristicChangedCallback( //11
        in String deviceId,
        in IGattCharacteristicChangedCallback callback
    );
    int readCharacteristic( //12
        in String deviceId,
        in String serviceId,
        in String characteristicId,
        in IGattReadCallback callback
    );
    int writeDescriptor( //13
        in String deviceId,
        in String serviceId,
        in String characteristicId,
        int notify,
        in IGattDescriptorWriteCallback callback
    );
    int setAllowedPriorityConnectionOptions( //14
        in String deviceId,
        in TagPriorityConnectionOptions options,
        in ITagPriorityConnectionProgressListener listener
    );
    List<String> getPriorityConnectionTagDevices(); //15
    void registerTagPriorityConnectionCallback( //16
        in List<String> deviceIds,
        in ITagPriorityConnectionStateCallback callback
    );
    void unregisterTagPriorityConnectionCallback( //17
        in String deviceId,
        in ITagPriorityConnectionStateCallback callback
    );
    void startReadRemoteRssi( //18
        in String deviceId,
        long refreshRate,
        in IGattRssiCallback bleRssiCallback
    );
    void stopReadRemoteRssi(in String deviceId); //19
    void registerScanResultCallback( //20
        in String deviceId,
        in IScanResultCallback callback
    );
    void unregisterScanResultCallback(in String deviceId); //21
    int isPpAgreed(); //22
}