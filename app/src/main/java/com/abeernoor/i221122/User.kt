package com.abeernoor.i221122

data class User(
    val userId: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val image: String = "",
    val contactNumber: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val onlineStatus: Boolean = false
)