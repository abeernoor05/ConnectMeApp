package com.abeernoor.i221122

import android.os.Parcel
import android.os.Parcelable

data class story(
    val userId: String = "",
    val username: String = "",
    val image: String = "", // Profile picture
    val storyContent: String = "", // Story image or video URL
    val videoUrl: String = "", // Optional video URL
    val timestamp: Long = 0L,
    val isUserStory: Boolean = false,
    val onlineStatus: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        userId = parcel.readString() ?: "",
        username = parcel.readString() ?: "",
        image = parcel.readString() ?: "",
        storyContent = parcel.readString() ?: "",
        videoUrl = parcel.readString() ?: "",
        timestamp = parcel.readLong(),
        isUserStory = parcel.readByte() != 0.toByte(),
        onlineStatus = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userId)
        parcel.writeString(username)
        parcel.writeString(image)
        parcel.writeString(storyContent)
        parcel.writeString(videoUrl)
        parcel.writeLong(timestamp)
        parcel.writeByte(if (isUserStory) 1 else 0)
        parcel.writeByte(if (onlineStatus) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<story> {
        override fun createFromParcel(parcel: Parcel): story {
            return story(parcel)
        }

        override fun newArray(size: Int): Array<story?> {
            return arrayOfNulls(size)
        }
    }
}