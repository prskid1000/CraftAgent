package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.common.NPCService

/**
 * Registers listeners for block interactions, chat messages and so on.
 */
class EventListenerRegisterer(
    private val npcService: NPCService
) {
    /**
     * Register the event listeners.
     */
    fun register() {
        listOf<IEventListener>(
            ChatMessageListener(npcService)
        ).forEach { listener -> listener.register() }
    }
}
