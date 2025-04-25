package com.abeernoor.i221122

data class ChatModel(
    val chatId: String = "",
    val userId: String = "",
    val username: String = "",
    val profileImage: String = "",
    val lastMessage: String = "",
    val lastMessageType: String = "text",
    val timestamp: Long = 0L
)