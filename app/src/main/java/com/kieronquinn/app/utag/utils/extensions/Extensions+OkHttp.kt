package com.kieronquinn.app.utag.utils.extensions

import android.content.Context
import com.kieronquinn.app.utag.networking.interceptors.DefaultUserAgentInterceptor
import com.kieronquinn.app.utag.networking.interceptors.FindAuthInterceptor
import com.kieronquinn.app.utag.networking.interceptors.OspInterceptor
import com.kieronquinn.app.utag.networking.interceptors.SmartThingsAuthInterceptor
import okhttp3.OkHttpClient

fun smartThingsClient(context: Context, updateBody: Boolean = false): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(SmartThingsAuthInterceptor(context, updateBody))
    .build()

fun findClient(context: Context): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(FindAuthInterceptor(context))
    .build()

fun defaultUserAgentClient(): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(DefaultUserAgentInterceptor())
    .build()

fun ospClient(): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(OspInterceptor())
    .build()