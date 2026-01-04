package me.prskid1000.craftagent.model.database

import java.util.UUID

data class Conversation(
    val id: Long = 0,
    val uuid: UUID,
    val role: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
