package com.abeernoor.i221122

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val type: String = "text", // text, image, post
    val content: String = "",
    val timestamp: Long = 0L,
    val seen: Boolean = false,
    val vanishMode: Boolean = false
)