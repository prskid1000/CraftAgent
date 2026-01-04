package me.prskid1000.craftagent.history

import com.fasterxml.jackson.databind.ObjectMapper
import me.prskid1000.craftagent.constant.Instructions
import me.prskid1000.craftagent.database.repositories.ConversationRepository
import me.prskid1000.craftagent.llm.LLMClient
import me.prskid1000.craftagent.model.database.Conversation
import java.util.UUID

/**
 * ConversationHistory that uses direct database calls instead of in-memory storage.
 * All conversations are persisted immediately to the database.
 */
class ConversationHistory(
    private val llmClient: LLMClient,
    private val conversationRepository: ConversationRepository,
    private val npcUuid: UUID,
    private val systemPrompt: () -> String, // Function to generate system prompt fresh
    private val maxHistoryLength: Int = 5
) {
    companion object {
        private val objectMapper = ObjectMapper()
    }

    /**
     * Gets the latest conversations from database, ordered by timestamp (oldest first)
     * Filters out system messages - system prompt is generated fresh, not stored
     */
    val latestConversations: List<ConversationMessage>
        get() = synchronized(this) {
            conversationRepository.selectByUuid(npcUuid, maxHistoryLength * 2)
                .filter { it.role != "system" } // Never return system messages from DB
                .map { ConversationMessage(it.message, it.role, it.timestamp) }
        }
    
    /**
     * Gets the current system prompt (generated fresh)
     */
    fun getSystemPrompt(): String = systemPrompt()

    @Synchronized
    fun add(message: ConversationMessage) {
        // Never store system messages in database
        if (message.role == "system") {
            return
        }
        
        // Insert directly to database (only user/assistant messages)
        val conversation = Conversation(
            uuid = npcUuid,
            role = message.role,
            message = message.message,
            timestamp = message.timestamp
        )
        conversationRepository.insert(conversation)

        // Check if summarization is needed
        val currentCount = conversationRepository.selectByUuid(npcUuid, maxHistoryLength + 1).size
        if (currentCount > maxHistoryLength) {
            // Summarization will be handled in performSummarizationIfNeeded()
        }
    }

    /**
     * Checks if summarization is needed and performs it if so.
     * Should be called during processLLM() to ensure it goes through the scheduler.
     * @return true if summarization was performed, false otherwise
     */
    @Synchronized
    fun performSummarizationIfNeeded(): Boolean {
        val allConversations = conversationRepository.selectByUuid(npcUuid, maxHistoryLength * 2)
        
        // Filter out system messages (they're not stored anyway)
        val nonSystemConversations = allConversations.filter { it.role != "system" }
        
        if (nonSystemConversations.size < maxHistoryLength) {
            return false
        }

        val removeCount = maxHistoryLength / 3
        if (nonSystemConversations.size < removeCount + 1) {
            return false
        }

        // Get conversations to summarize (system messages are never stored)
        val toSummarize = nonSystemConversations.take(removeCount)
        
        if (toSummarize.isEmpty()) {
            return false
        }

        // Convert to ConversationMessage for summarization
        val messagesToSummarize = toSummarize.map { ConversationMessage(it.message, it.role, it.timestamp) }
        val summaryMessage = summarize(messagesToSummarize)
        
        // Delete old conversations from database
        val idsToDelete = toSummarize.map { it.id }
        conversationRepository.deleteByIds(idsToDelete)
        
        // Insert summary message
        val summaryConversation = Conversation(
            uuid = npcUuid,
            role = summaryMessage.role,
            message = summaryMessage.message,
            timestamp = summaryMessage.timestamp
        )
        conversationRepository.insert(summaryConversation)
        
        return true
    }

    private fun summarize(conversations: List<ConversationMessage>): ConversationMessage {
        val summarizeMessage = ConversationMessage(
            Instructions.SUMMARY_PROMPT.format(objectMapper.writeValueAsString(conversations)),
            "user"
        )
        // Use chat and extract content
        // NOTE: This is called during processLLM(), so it's within the scheduler's control
        // Pass null for server since summarization doesn't need server context
        val llmResponse = llmClient.chat(listOf(summarizeMessage), null)
        return ConversationMessage(llmResponse.content, "assistant")
    }

    @Synchronized
    fun updateSystemPrompt(newSystemPrompt: String) {
        // System prompt is generated fresh, not stored in database
        // This method is kept for compatibility but does nothing
        // The systemPrompt function will be called when needed
    }

    fun getLastMessage(): String {
        val conversations = latestConversations
        return conversations.lastOrNull()?.message ?: ""
    }
}