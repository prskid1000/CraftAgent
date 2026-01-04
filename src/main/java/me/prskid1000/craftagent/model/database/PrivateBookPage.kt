package me.prskid1000.craftagent.model.database

import java.util.UUID

data class PrivateBookPage(
    val npcUuid: UUID,
    val pageTitle: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

