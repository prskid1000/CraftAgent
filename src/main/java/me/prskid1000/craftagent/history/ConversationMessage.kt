package me.prskid1000.craftagent.history

/**
 * Represents a message in the conversation history (system, user, or assistant messages)
 */
data class ConversationMessage(
    val message: String,
    val role: String,
    val timestamp: Long = System.currentTimeMillis()
)
