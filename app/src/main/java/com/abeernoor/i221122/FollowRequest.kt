package com.abeernoor.i221122

data class FollowRequest(
    val requesterId: Int,
    val username: String,
    val profileImage: String = ""
)