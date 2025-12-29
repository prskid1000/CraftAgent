package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.model.NPC
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import java.util.UUID

class ChatMessageListener(
    npcs: Map<UUID, NPC>
) : AEventListener(npcs) {

    override fun register() {
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, _ ->
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
