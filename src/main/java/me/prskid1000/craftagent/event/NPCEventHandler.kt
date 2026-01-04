package me.prskid1000.craftagent.event

import me.prskid1000.craftagent.action.ActionExecutor
import me.prskid1000.craftagent.action.ActionProvider
import me.prskid1000.craftagent.action.CommunicationActionHandler
import me.prskid1000.craftagent.action.MemoryActionHandler
import me.prskid1000.craftagent.common.NPCService
import me.prskid1000.craftagent.config.NPCConfig
import me.prskid1000.craftagent.context.ContextProvider
import me.prskid1000.craftagent.database.repositories.MessageRepository
import me.prskid1000.craftagent.database.repositories.SharebookRepository
import me.prskid1000.craftagent.history.ConversationHistory
import me.prskid1000.craftagent.history.ConversationMessage
import me.prskid1000.craftagent.llm.LLMClient
import me.prskid1000.craftagent.util.LogUtil
import me.prskid1000.craftagent.util.StructuredInputFormatter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class NPCEventHandler(
    private val llmClient: LLMClient,
    private val history: ConversationHistory,
    private val contextProvider: ContextProvider,
    private val config: NPCConfig,
    private val messageRepository: MessageRepository,
    private val sharebookRepository: SharebookRepository,
    private val npcService: NPCService
): EventHandler {
    private val executorService: ThreadPoolExecutor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(10),
        ThreadPoolExecutor.DiscardPolicy()
    )

    /**
     * @deprecated Use updateState() instead. This now just calls updateState() for backward compatibility.
     */
    @Deprecated("Use updateState() instead", ReplaceWith("updateState(prompt)"))
    override fun onEvent(prompt: String) {
        updateState(prompt)
    }

    /**
     * Updates state only (store prompt in history) without triggering LLM.
     * Called when events/messages occur.
     */
    override fun updateState(prompt: String) {
        CompletableFuture.runAsync({
            LogUtil.info("updateState: $prompt")
            // Store only the original prompt in history (without context to avoid duplication)
            history.add(ConversationMessage(prompt, "user"))
        }, executorService)
            .exceptionally {
                LogUtil.error("Error updating state: $prompt", it)
                null
            }
    }

    /**
     * Processes LLM call synchronously. Called by scheduler periodically.
     * Processes serially - blocks until completion.
     * @return true if LLM call succeeded, false otherwise
     */
    override fun processLLM(): Boolean {
        return try {
            LogUtil.info("processLLM: Processing LLM for NPC ${config.npcName}")

            // Perform summarization if needed
            history.performSummarizationIfNeeded()

            // Build messages for LLM: history + current state with context
            val messagesForLLM = mutableListOf<ConversationMessage>()
            // Add all history messages (without context)
            messagesForLLM.addAll(history.latestConversations)
            
            // If there's a last user message, replace it with formatted version that includes context
            if (messagesForLLM.isNotEmpty() && messagesForLLM.last().role == "user") {
                val lastUserMessage = messagesForLLM.last().message
                val formattedPrompt: String = StructuredInputFormatter.formatStructured(lastUserMessage, contextProvider.buildContext())
                messagesForLLM[messagesForLLM.size - 1] = ConversationMessage(formattedPrompt, "user")
            } else {
                // No recent user message, create a context-only prompt
                val contextPrompt = "Current state and context. What should I do?"
                val formattedPrompt: String = StructuredInputFormatter.formatStructured(contextPrompt, contextProvider.buildContext())
                messagesForLLM.add(ConversationMessage(formattedPrompt, "user"))
            }
            
            // Call LLM and get response
            val server = contextProvider.getNpcEntity().server
            val llmResponse = llmClient.chat(messagesForLLM, server)
            
            // Parse structured response (message + actions)
            val structuredResponse = llmResponse.structuredResponse
            val message = structuredResponse.message
            val actions = structuredResponse.actions
            
            // Execute actions if present
            if (actions.isNotEmpty()) {
                val npcEntity = contextProvider.getNpcEntity()
                
                // Create action provider with memory and communication handlers
                val memoryHandler = MemoryActionHandler(
                    contextProvider.memoryManager,
                    sharebookRepository,
                    config.uuid,
                    config.npcName,
                    contextProvider.baseConfig
                )
                val communicationHandler = CommunicationActionHandler(
                    messageRepository,
                    npcService,
                    config.uuid,
                    config.npcName,
                    contextProvider.baseConfig
                )
                val actionProvider = ActionProvider(memoryHandler, communicationHandler)
                val actionExecutor = ActionExecutor(npcEntity, actionProvider)
                actionExecutor.executeActions(actions)
                
                // Broadcast memory update if any memory actions were executed
                val hasMemoryActions = actions.any { it.trim().startsWith("sharedbook ") || it.trim().startsWith("privatebook ") }
                if (hasMemoryActions) {
                    npcService.webServer?.broadcastUpdate("memory-updated", mapOf("uuid" to config.uuid.toString()))
                }
            }
            
            // Send message if present and different from last
            if (message.isNotEmpty() && message != history.getLastMessage()) {
                val npcEntity = contextProvider.getNpcEntity()
                me.prskid1000.craftagent.util.ChatUtil.sendChatMessage(npcEntity, message)
            }
            
            // Store structured response in history (store the full JSON for web UI display)
            // The web UI will parse and display both message and actions
            val responseContent = llmResponse.content.trim()
            history.add(ConversationMessage(responseContent, "assistant"))
            
            // Broadcast update to web UI
            npcService.webServer?.broadcastUpdate("messages-updated", mapOf("uuid" to config.uuid.toString()))
            
            true
        } catch (e: Exception) {
            LogUtil.debugInChat("Could not generate a response: " + buildErrorMessage(e))
            LogUtil.error("Error occurred processing LLM for NPC ${config.npcName}", e)
            false
        }
    }

    override fun stopService() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    override fun queueIsEmpty(): Boolean {
        return executorService.queue.isEmpty()
    }

    private fun buildErrorMessage(exception: Throwable): String? {
        val chain = generateSequence(exception) { it.cause }
        val custom = chain.filterIsInstance<me.prskid1000.craftagent.exception.CraftAgentException>().firstOrNull()
        if (custom != null) {
            return custom.message
        }
        return generateSequence(exception) { it.cause }.lastOrNull()?.message ?: exception.message
    }

}