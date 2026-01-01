package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.common.NPCService
import me.prskid1000.craftagent.config.ConfigProvider

/**
 * Registers listeners for block interactions, chat messages and so on.
 */
class EventListenerRegisterer(
    private val npcService: NPCService,
    private val configProvider: ConfigProvider
) {
    /**
     * Register the event listeners.
     */
    fun register() {
        listOf<IEventListener>(
            ChatMessageListener(npcService),
            AgeUpdateListener(npcService),
            LLMProcessingScheduler(npcService, configProvider)
        ).forEach { listener -> listener.register() }
    }
}
