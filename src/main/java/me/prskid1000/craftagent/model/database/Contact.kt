package me.prskid1000.craftagent.model.database

import java.util.UUID

data class Contact(
    val npcUuid: UUID,
    val contactUuid: UUID,
    val contactName: String,
    val contactType: String, // "npc" or "player"
    val relationship: String = "neutral", // "friend", "enemy", "neutral", "teammate"
    val lastSeen: Long = System.currentTimeMillis(),
    val notes: String = ""
)

