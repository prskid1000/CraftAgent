package me.prskid1000.craftagent.model.database

import java.util.UUID

data class PrivateBookPage(
    val id: Long = 0,
    val npcUuid: UUID, // UUID of the NPC who owns this page
    val pageTitle: String, // Title of the page
    val content: String, // Content of the page
    val timestamp: Long = System.currentTimeMillis() // Last update time
)

