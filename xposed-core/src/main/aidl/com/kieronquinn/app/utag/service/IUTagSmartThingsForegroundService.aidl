package com.kieronquinn.app.utag.service;

//Exists solely to provide a connection from the ST foreground service to uTag for monitoring
interface IUTagSmartThingsForegroundService {
    boolean ping();
    void stop();
    void stopProcess();
}