package me.prskid1000.craftagent.history

data class Message(
    val message: String,
    val role: String,
    val timestamp: Long = System.currentTimeMillis()
)
