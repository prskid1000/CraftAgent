package me.prskid1000.craftagent.event

import me.prskid1000.craftagent.action.ActionExecutor
import me.prskid1000.craftagent.common.NPCService
import me.prskid1000.craftagent.config.NPCConfig
import me.prskid1000.craftagent.context.ContextProvider
import me.prskid1000.craftagent.database.repositories.MessageRepository
import me.prskid1000.craftagent.database.repositories.SharebookRepository
import me.prskid1000.craftagent.history.ConversationHistory
import me.prskid1000.craftagent.history.ConversationMessage
import me.prskid1000.craftagent.llm.LLMClient
import me.prskid1000.craftagent.llm.StructuredLLMResponse
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
            // Perform summarization if needed
            history.performSummarizationIfNeeded()

            // Build messages for LLM: system prompt (fresh) + history + current state with context
            val messagesForLLM = mutableListOf<ConversationMessage>()
            // Add system prompt first (generated fresh, never stored)
            messagesForLLM.add(ConversationMessage(history.getSystemPrompt(), "system"))
            // Add all history messages (without context) - these are only user/assistant, no system
            // Filter out actions from assistant messages - only send message part to LLM
            history.latestConversations.forEach { msg ->
                if (msg.role == "assistant") {
                    // Extract only the message part from structured response (filter out actions)
                    // The full JSON is stored in DB, but we only send the message text to LLM
                    val messageOnly = extractMessageFromStructuredResponse(msg.message)
                    messagesForLLM.add(ConversationMessage(messageOnly, "assistant"))
                } else {
                    // User messages are sent as-is
                    messagesForLLM.add(msg)
                }
            }
            
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
                
                // Create action provider with all handlers using factory
                val actionProvider = me.prskid1000.craftagent.action.ActionProviderFactory.create(
                    npcEntity,
                    contextProvider,
                    config.uuid,
                    config.npcName,
                    contextProvider.memoryManager,
                    messageRepository,
                    sharebookRepository,
                    npcService,
                    contextProvider.baseConfig
                )
                val actionExecutor = ActionExecutor(npcEntity, actionProvider)
                actionExecutor.executeActions(actions)
                
            }
            
            // Send message if present (non-empty, not whitespace-only) and different from last
            // Empty string ("") means NPC doesn't want to speak publicly
            // Extract message part from last stored response for comparison
            val lastMessage = extractMessageFromStructuredResponse(history.getLastMessage())
            if (structuredResponse.hasNonEmptyMessage() && message.trim() != lastMessage.trim()) {
                val npcEntity = contextProvider.getNpcEntity()
                me.prskid1000.craftagent.util.ChatUtil.sendChatMessage(npcEntity, message)
            }
            
            // Store structured response in history (store the full JSON for web UI display)
            // The web UI will parse and display both message and actions
            val responseContent = llmResponse.content.trim()
            history.add(ConversationMessage(responseContent, "assistant"))
            
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
    
    /**
     * Extracts only the message part from a structured response JSON string.
     * If the content is not valid JSON or doesn't contain a message field,
     * returns the original content as fallback (for backward compatibility with plain text).
     * 
     * This ensures that when sending conversation history to LLM, only the message text
     * is sent, not the actions. The full JSON (with actions) is still stored in the database.
     */
    private fun extractMessageFromStructuredResponse(content: String): String {
        if (content.isBlank()) {
            return ""
        }
        
        try {
            // Try to parse as structured response
            val structured = StructuredLLMResponse.parse(content)
            val message = structured.message
            
            // Return the message part, or empty string if no message
            return message ?: ""
        } catch (e: Exception) {
            // If parsing fails, it might be plain text (backward compatibility)
            // Return as-is in that case
            return content
        }
    }

}