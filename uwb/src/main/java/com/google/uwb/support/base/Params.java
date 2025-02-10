/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.uwb.support.base;

import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Arrays;

/** Provides common parameter operations. */
@RequiresApi(VERSION_CODES.LOLLIPOP)
public abstract class Params {
    private static final String KEY_BUNDLE_VERSION = "bundle_version";
    protected static final int BUNDLE_VERSION_UNKNOWN = -1;

    protected static final String KEY_PROTOCOL_NAME = "protocol_name";
    protected static final String PROTOCOL_NAME_UNKNOWN = "unknown";

    public PersistableBundle toBundle() {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(KEY_BUNDLE_VERSION, getBundleVersion());
        bundle.putString(KEY_PROTOCOL_NAME, getProtocolName());
        return bundle;
    }

    private byte[] toBytes() {
        Parcel parcel = Parcel.obtain();
        toBundle().writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public abstract String getProtocolName();

    protected abstract int getBundleVersion();

    public static int getBundleVersion(PersistableBundle bundle) {
        return bundle.getInt(KEY_BUNDLE_VERSION, BUNDLE_VERSION_UNKNOWN);
    }

    public static boolean isProtocol(PersistableBundle bundle, String protocol) {
        return bundle.getString(KEY_PROTOCOL_NAME, PROTOCOL_NAME_UNKNOWN).equals(protocol);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBytes());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Params && Arrays.equals(this.toBytes(), ((Params) obj).toBytes());
    }
}
