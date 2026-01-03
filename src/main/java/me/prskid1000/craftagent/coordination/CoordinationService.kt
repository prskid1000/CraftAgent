package me.prskid1000.craftagent.coordination

import me.prskid1000.craftagent.common.NPCService
import me.prskid1000.craftagent.model.NPC
import me.prskid1000.craftagent.util.LogUtil
import java.util.UUID

/**
 * Service for coordinating between NPCs and broadcasting events
 */
class CoordinationService(
    private val npcService: NPCService
) {
    /**
     * Send a direct message from one NPC to another.
     * Stores in database, displays in chat, and updates state (no LLM trigger).
     */
    fun sendDirectMessage(fromNpc: NPC, toNpcUuid: UUID, message: String) {
        val targetNpc = npcService.uuidToNpc[toNpcUuid] ?: return
        
        // Get message repository from target NPC's context provider
        val messageRepository = targetNpc.contextProvider.getMessageRepository() ?: return
        
        // Store message in database
        val dbMessage = me.prskid1000.craftagent.model.database.Message(
            recipientUuid = toNpcUuid,
            senderUuid = fromNpc.config.uuid,
            senderName = fromNpc.config.npcName,
            senderType = "npc",
            subject = "Direct message from ${fromNpc.config.npcName}",
            content = message
        )
        
        val maxMessages = targetNpc.contextProvider.getBaseConfig().getMaxMessages()
        messageRepository.insert(dbMessage, maxMessages)
        
        // Display in chat
        val chatMessage = "${fromNpc.config.npcName} says to ${targetNpc.config.npcName}: $message"
        me.prskid1000.craftagent.util.ChatUtil.sendChatMessage(targetNpc.entity, chatMessage)
        
        // Update state (no LLM trigger)
        val formattedMessage = "${fromNpc.config.npcName} says to you: $message"
        targetNpc.eventHandler.updateState(formattedMessage)
        
        LogUtil.info("NPC ${fromNpc.config.npcName} sent direct message to ${targetNpc.config.npcName}: $message")
    }

    /**
     * Get list of all active NPCs for coordination
     */
    fun getActiveNpcs(): List<Pair<UUID, String>> {
        return npcService.uuidToNpc.map { (uuid, npc) ->
            Pair(uuid, npc.config.npcName)
        }
    }
}

