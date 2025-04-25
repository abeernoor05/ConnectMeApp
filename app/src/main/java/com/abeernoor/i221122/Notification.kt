package com.abeernoor.i221122

data class Notification(
    val notificationId: String = "",
    val type: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)