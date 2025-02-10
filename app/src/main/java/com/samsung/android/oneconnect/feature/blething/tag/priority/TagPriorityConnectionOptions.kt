package com.samsung.android.oneconnect.feature.blething.tag.priority

import android.os.Parcel
import android.os.Parcelable

enum class TagPriorityConnectionOptions: Parcelable {
    LEFT_BEHIND_ALERTS_OFF,
    LEFT_BEHIND_ALERTS_ON,
    CAMERA_SHUTTER_OFF,
    CAMERA_SHUTTER_ON,
    LEFT_BEHIND_ALERTS_OFF_FOR_EXTERNAL,
    LEFT_BEHIND_ALERTS_ON_FOR_EXTERNAL;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(this.name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TagPriorityConnectionOptions> {
        override fun createFromParcel(parcel: Parcel): TagPriorityConnectionOptions {
            return TagPriorityConnectionOptions.valueOf(parcel.readString()!!)
        }

        override fun newArray(size: Int): Array<TagPriorityConnectionOptions?> {
            return arrayOfNulls(size)
        }
    }
}