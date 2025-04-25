package com.abeernoor.i221122

import android.os.Parcel
import android.os.Parcelable

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val profileImage: String = "",
    val postImage: String = "",
    val caption: String = "",
    val timestamp: Long = 0L,
    val likes: MutableList<String> = mutableListOf(),
    val comments: MutableList<Comment> = mutableListOf()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        postId = parcel.readString() ?: "",
        userId = parcel.readString() ?: "",
        username = parcel.readString() ?: "",
        profileImage = parcel.readString() ?: "",
        postImage = parcel.readString() ?: "",
        caption = parcel.readString() ?: "",
        timestamp = parcel.readLong(),
        likes = mutableListOf<String>().apply {
            parcel.readStringList(this)
        },
        comments = mutableListOf<Comment>().apply {
            parcel.readList(this, Comment::class.java.classLoader)
        }
    )
    //parcelization
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(postId)
        parcel.writeString(userId)
        parcel.writeString(username)
        parcel.writeString(profileImage)
        parcel.writeString(postImage)
        parcel.writeString(caption)
        parcel.writeLong(timestamp)
        parcel.writeStringList(likes)
        parcel.writeList(comments)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Post> {
        override fun createFromParcel(parcel: Parcel): Post {
            return Post(parcel)
        }

        override fun newArray(size: Int): Array<Post?> {
            return arrayOfNulls(size)
        }
    }
}