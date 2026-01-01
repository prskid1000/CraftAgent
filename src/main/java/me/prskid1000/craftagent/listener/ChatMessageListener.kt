package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.common.NPCService
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents

class ChatMessageListener(
    private val npcService: NPCService
) : AEventListener() {

    override fun register() {
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, _ ->
            val npcs = npcService.uuidToNpc
            npcs.forEach { npcEntry ->
                if (npcEntry.value.entity.uuid == sender.uuid) {
                    return@forEach
                }
                val chatMessage =
                    String.format(
                        "Player '%s' has written the message: %s",
                        sender.name.string ?: "Server Console",
                        message.content.string,
                    )
                npcEntry.value.eventHandler.onEvent(chatMessage)
            }
        }
    }
}
