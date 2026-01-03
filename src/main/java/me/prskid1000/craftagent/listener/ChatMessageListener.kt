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
