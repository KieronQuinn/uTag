package com.samsung.android.oneconnect.base.device.tag

import android.os.Parcel
import android.os.Parcelable

enum class TagConnectionState: Parcelable {
    SUCCESS,
    FAIL,
    NOT_AVAILABLE,
    CONNECTING,
    FAIL_PRIORITY_CONNECTION_CHANNEL_RESERVED,
    FAIL_CONNECTION_CHANNEL_SHOULD_BE_RESERVED_FOR_OWNER;

    fun isLimitReached(): Boolean {
        return this == FAIL_PRIORITY_CONNECTION_CHANNEL_RESERVED ||
                this == FAIL_CONNECTION_CHANNEL_SHOULD_BE_RESERVED_FOR_OWNER
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(this.name)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun isError(): Boolean {
        return this != SUCCESS && this != CONNECTING
    }

    companion object CREATOR : Parcelable.Creator<TagConnectionState> {
        override fun createFromParcel(parcel: Parcel): TagConnectionState {
            return TagConnectionState.valueOf(parcel.readString()!!)
        }

        override fun newArray(size: Int): Array<TagConnectionState?> {
            return arrayOfNulls(size)
        }
    }
}