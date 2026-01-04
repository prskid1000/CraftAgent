package me.prskid1000.craftagent.model.database

import java.util.UUID

data class Message(
    val id: Long = 0,
    val recipientUuid: UUID,
    val senderUuid: UUID,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

