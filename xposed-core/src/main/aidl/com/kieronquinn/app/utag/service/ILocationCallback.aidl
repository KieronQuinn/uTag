package com.kieronquinn.app.utag.service;

import android.location.Location;

interface ILocationCallback {
    void onResult(in @nullable Location location);
}