package com.kieronquinn.app.utag.service;

import android.content.ComponentName;

interface IServiceConnection {

    void onServiceConnected(in IBinder binder, in ComponentName componentName);
    void onServiceDisconnected(in ComponentName componentName);

}