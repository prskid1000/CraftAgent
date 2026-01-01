package me.prskid1000.craftagent.model.database

import java.util.UUID

data class Contact(
    val npcUuid: UUID,
    val contactUuid: UUID,
    val contactName: String,
    val contactType: String, // "npc" or "player"
    val relationship: String = "neutral", // "friend", "enemy", "neutral", "teammate"
    val lastSeen: Long = System.currentTimeMillis(),
    val notes: String = "",
    val enmityLevel: Double = 0.0, // 0.0 (no enmity) to 1.0 (maximum enmity)
    val friendshipLevel: Double = 0.0 // 0.0 (stranger) to 1.0 (best friend)
)

