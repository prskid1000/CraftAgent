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
        
        // Add new NPC to all existing NPCs' contact lists
        npcService.uuidToNpc.forEach { (uuid, npc) ->
            if (uuid != newNpc.config.uuid) {
                try {
                    npc.contextProvider.memoryManager?.addOrUpdateContact(
                        newNpc.config.uuid,
                        newNpc.config.npcName,
                        "npc",
                        "neutral",
                        "Recently joined the world"
                    )
                } catch (e: Exception) {
                    LogUtil.error("Error adding new NPC to contact list", e)
                }
            }
        }
        
        // Add all existing NPCs to new NPC's contact list
        npcService.uuidToNpc.forEach { (uuid, npc) ->
            if (uuid != newNpc.config.uuid) {
                try {
                    newNpc.contextProvider.memoryManager?.addOrUpdateContact(
                        uuid,
                        npc.config.npcName,
                        "npc",
                        "neutral",
                        "Existing NPC in the world"
                    )
                } catch (e: Exception) {
                    LogUtil.error("Error adding existing NPCs to new NPC contact list", e)
                }
            }
        }
    }

    /**
     * Notify all NPCs about an NPC being removed
     */
    fun notifyNpcRemoved(removedNpcName: String, removedUuid: UUID) {
        val message = "NPC '$removedNpcName' (${removedUuid}) has left the world."
        broadcastMessage(message, excludeUuid = removedUuid)
        
        // Update contact lists - mark as inactive or update notes
        npcService.uuidToNpc.forEach { (uuid, npc) ->
            try {
                val contact = npc.contextProvider.memoryManager?.getContact(removedUuid)
                if (contact != null) {
                    // Update contact with removal information
                    npc.contextProvider.memoryManager?.addOrUpdateContact(
                        removedUuid,
                        removedNpcName,
                        "npc",
                        contact.relationship,
                        "${contact.notes}; Left the world at ${System.currentTimeMillis()}"
                    )
                }
            } catch (e: Exception) {
                LogUtil.error("Error updating contact list for removed NPC", e)
            }
        }
    }

    /**
     * Notify all NPCs about an NPC death
     */
    fun notifyNpcDeath(deadNpcName: String, deadUuid: UUID) {
        val message = "NPC '$deadNpcName' (${deadUuid}) has died and will respawn."
        broadcastMessage(message, excludeUuid = deadUuid)
        
        // Update contact lists - mark as dead but will respawn
        npcService.uuidToNpc.forEach { (uuid, npc) ->
            try {
                val contact = npc.contextProvider.memoryManager?.getContact(deadUuid)
                if (contact != null) {
                    // Update contact with death information
                    val updatedNotes = if (contact.notes.isNotEmpty()) {
                        "${contact.notes}; Died at ${System.currentTimeMillis()}, will respawn"
                    } else {
                        "Died at ${System.currentTimeMillis()}, will respawn"
                    }
                    npc.contextProvider.memoryManager?.addOrUpdateContact(
                        deadUuid,
                        deadNpcName,
                        "npc",
                        contact.relationship,
                        updatedNotes
                    )
                }
            } catch (e: Exception) {
                LogUtil.error("Error updating contact list for dead NPC", e)
            }
        }
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
        
        // Update last seen
        targetNpc.contextProvider.memoryManager?.updateContactLastSeen(fromNpc.config.uuid)
        
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

