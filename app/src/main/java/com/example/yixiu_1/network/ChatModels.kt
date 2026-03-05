package com.example.yixiu_1.network

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val role: Role, // user 或 assistant
    val timestamp: Long = System.currentTimeMillis()
)

enum class Role {
    USER, ASSISTANT
}


