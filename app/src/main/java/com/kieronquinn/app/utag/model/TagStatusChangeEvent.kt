package com.kieronquinn.app.utag.model

import android.os.Parcel
import android.os.Parcelable

enum class TagStatusChangeEvent(val value: String): Parcelable {
    RING_START("01"),
    RING_STOP("00"),
    BUTTON_CLICK("01"),
    BUTTON_LONG_CLICK("02"),
    BUTTON_DOUBLE_CLICK("03");

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(this.name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TagStatusChangeEvent> {
        override fun createFromParcel(parcel: Parcel): TagStatusChangeEvent {
            return TagStatusChangeEvent.valueOf(parcel.readString()!!)
        }

        override fun newArray(size: Int): Array<TagStatusChangeEvent?> {
            return arrayOfNulls(size)
        }
    }
}