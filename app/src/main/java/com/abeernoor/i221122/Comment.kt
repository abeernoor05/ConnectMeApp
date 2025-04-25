package com.abeernoor.i221122

import android.os.Parcel
import android.os.Parcelable

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Long = 0L
) : Parcelable {
    constructor(parcel: Parcel) : this(
        commentId = parcel.readString() ?: "",
        userId = parcel.readString() ?: "",
        username = parcel.readString() ?: "",
        text = parcel.readString() ?: "",
        timestamp = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(commentId)
        parcel.writeString(userId)
        parcel.writeString(username)
        parcel.writeString(text)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Comment> {
        override fun createFromParcel(parcel: Parcel): Comment {
            return Comment(parcel)
        }

        override fun newArray(size: Int): Array<Comment?> {
            return arrayOfNulls(size)
        }
    }
}