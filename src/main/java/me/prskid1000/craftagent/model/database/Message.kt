package me.prskid1000.craftagent.model.database

import java.util.UUID

data class Message(
    val id: Long = 0,
    val recipientUuid: UUID, // NPC or player who receives the message
    val senderUuid: UUID, // NPC or player who sent the message
    val senderName: String, // Name of sender (for display)
    val senderType: String, // "npc" or "player"
    val subject: String, // Message subject/title
    val content: String, // Message content
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false // Whether the message has been read
)

