package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.model.NPC
import java.util.UUID

/**
 * Registers listeners for block interactions, chat messages and so on.
 */
class EventListenerRegisterer(
    private val npcs: Map<UUID, NPC>
) {
    /**
     * Register the event listeners.
     */
    fun register() {
        listOf<IEventListener>(
            ChatMessageListener(npcs)
        ).forEach { listener -> listener.register() }
    }
}
