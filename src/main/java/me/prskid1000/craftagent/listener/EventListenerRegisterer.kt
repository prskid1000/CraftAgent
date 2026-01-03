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
    private var llmProcessingScheduler: LLMProcessingScheduler? = null
    
    /**
     * Register the event listeners.
     */
    fun register() {
        val scheduler = LLMProcessingScheduler(npcService, configProvider)
        llmProcessingScheduler = scheduler
        
        listOf<EventListener>(
            ChatMessageListener(npcService),
            AgeUpdateListener(npcService),
            scheduler
        ).forEach { listener -> listener.register() }
    }
    
    /**
     * Shutdown all listeners that require cleanup.
     */
    fun shutdown() {
        llmProcessingScheduler?.shutdown()
    }
}
