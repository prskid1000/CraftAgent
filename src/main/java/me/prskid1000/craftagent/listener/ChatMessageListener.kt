package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.common.NPCService
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents

class ChatMessageListener(
    private val npcService: NPCService
) : BaseEventListener() {

    override fun register() {
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, _ ->
            val npcs = npcService.uuidToNpc
            val playerName = sender.name.string ?: "Server Console"
            val messageContent = message.content.string
            
            // Check if message is targeted to a specific NPC (format: "name: message")
            val colonIndex = messageContent.indexOf(':')
            if (colonIndex > 0 && colonIndex < messageContent.length - 1) {
                val potentialName = messageContent.substring(0, colonIndex).trim()
                val actualMessage = messageContent.substring(colonIndex + 1).trim()
                
                // Only process if there's actual message content after the colon
                if (actualMessage.isNotEmpty()) {
                    // Find NPC by name (case-insensitive comparison)
                    val targetNpc = npcs.values.firstOrNull { npc ->
                        npc.config.npcName.equals(potentialName, ignoreCase = true)
                    }
                    
                    if (targetNpc != null && targetNpc.entity.uuid != sender.uuid) {
                        // Send message only to the targeted NPC
                        npcService.sendPlayerMessageToNpc(
                            sender.uuid,
                            playerName,
                            targetNpc.config.uuid,
                            actualMessage
                        )
                        return@register
                    }
                }
            }
            
            // No target found or no colon format - send to all NPCs
            npcs.forEach { npcEntry ->
                if (npcEntry.value.entity.uuid == sender.uuid) {
                    return@forEach
                }
                // Send message to NPC via mail system instead of directly to prompt
                npcService.sendPlayerMessageToNpc(
                    sender.uuid,
                    playerName,
                    npcEntry.value.config.uuid,
                    messageContent
                )
            }
        }
    }
}
