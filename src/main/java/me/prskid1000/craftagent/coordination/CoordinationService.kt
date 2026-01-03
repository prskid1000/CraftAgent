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
     * Broadcast a message to all NPCs and optionally to a specific player.
     * Updates state only (no LLM trigger).
     */
    fun broadcastMessage(message: String, excludeUuid: UUID? = null, targetPlayer: String? = null) {
        npcService.uuidToNpc.forEach { (uuid, npc) ->
            if (excludeUuid != null && uuid == excludeUuid) {
                return@forEach
            }
            try {
                // Just update state, no LLM trigger
                npc.eventHandler.updateState(message)
            } catch (e: Exception) {
                LogUtil.error("Error broadcasting message to NPC: ${npc.config.npcName}", e)
            }
        }
        
        // If target player specified, send to them via chat
        if (targetPlayer != null) {
            // This would need to be implemented via server player manager
            LogUtil.info("Broadcast to player $targetPlayer: $message")
        }
    }

    /**
     * Notify all NPCs about a new NPC being added
     */
    fun notifyNpcAdded(newNpc: NPC) {
        val message = "NPC '${newNpc.config.npcName}' (${newNpc.entity.uuid}) has joined the world. " +
                "You can now interact with them."
        broadcastMessage(message, excludeUuid = newNpc.config.uuid)
    }

    /**
     * Notify all NPCs about an NPC being removed
     */
    fun notifyNpcRemoved(removedNpcName: String, removedUuid: UUID) {
        val message = "NPC '$removedNpcName' (${removedUuid}) has left the world."
        broadcastMessage(message, excludeUuid = removedUuid)
    }

    /**
     * Notify all NPCs about an NPC death
     */
    fun notifyNpcDeath(deadNpcName: String, deadUuid: UUID) {
        val message = "NPC '$deadNpcName' (${deadUuid}) has died and will respawn."
        broadcastMessage(message, excludeUuid = deadUuid)
    }

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

