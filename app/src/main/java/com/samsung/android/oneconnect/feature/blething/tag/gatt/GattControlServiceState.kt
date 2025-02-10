package com.samsung.android.oneconnect.feature.blething.tag.gatt

import android.os.Parcel
import android.os.Parcelable

enum class GattControlServiceState: Parcelable {
    NONE,
    ALARM,
    ACTIIVTY,
    TAG_BUTTON,
    TAG_RINGTONE,
    AUDIO_VOLUME,
    BATTERY,
    FACTORY_RESET,
    E2EENCRYPTION,
    UWB_ACTIVATION,
    UWB_PARAMETER,
    DEBUG,
    BLINKING,
    BLINKING_FOR_NON_OWNER,
    MOTION_DETECTION,
    MOTION_DETECTION_FOR_NON_OWNER;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(this.name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GattControlServiceState> {
        override fun createFromParcel(parcel: Parcel): GattControlServiceState {
            return GattControlServiceState.valueOf(parcel.readString()!!)
        }

        override fun newArray(size: Int): Array<GattControlServiceState?> {
            return arrayOfNulls(size)
        }
    }
}