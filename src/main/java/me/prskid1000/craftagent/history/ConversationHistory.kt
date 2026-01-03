package me.prskid1000.craftagent.history

import com.fasterxml.jackson.databind.ObjectMapper
import me.prskid1000.craftagent.constant.Instructions
import me.prskid1000.craftagent.llm.LLMClient

class ConversationHistory(
    private val llmClient: LLMClient,
    initMessage: String,
    val latestConversations: MutableList<ConversationMessage>,
    private val maxHistoryLength: Int = 5
) {
    companion object {
        private val objectMapper = ObjectMapper()
    }

    init {
        setInitMessage(initMessage)
    }

    private var needsSummarization = false

    @Synchronized
    fun add(message: ConversationMessage) {
        latestConversations.add(message)

        if (latestConversations.size >= maxHistoryLength) {
            // Mark that summarization is needed, but don't do it immediately
            // The scheduler will handle it during the next processLLM() call
            needsSummarization = true
        }
    }

    /**
     * Checks if summarization is needed and performs it if so.
     * Should be called during processLLM() to ensure it goes through the scheduler.
     * @return true if summarization was performed, false otherwise
     */
    @Synchronized
    fun performSummarizationIfNeeded(): Boolean {
        if (!needsSummarization || latestConversations.size < maxHistoryLength) {
            needsSummarization = false
            return false
        }

        val removeCount = maxHistoryLength / 3
        if (latestConversations.size < removeCount + 1) {
            needsSummarization = false
            return false
        }

        val toSummarize = latestConversations.subList(1, removeCount + 1).toList()
        val message = summarize(toSummarize)
        latestConversations.removeAll(toSummarize)
        latestConversations.add(1, message)
        needsSummarization = false
        return true
    }

    private fun updateConversations() {
        // This method is now deprecated - use performSummarizationIfNeeded() instead
        // Kept for backward compatibility but should not be called directly
        performSummarizationIfNeeded()
    }

    private fun summarize(conversations: List<ConversationMessage>): ConversationMessage {
        val summarizeMessage = ConversationMessage(
            Instructions.SUMMARY_PROMPT.format( objectMapper.writeValueAsString(conversations)),
            "user")
        // Use chat and extract content
        // NOTE: This is called during processLLM(), so it's within the scheduler's control
        // Pass null for server since summarization doesn't need server context
        val llmResponse = llmClient.chat(listOf(summarizeMessage), null)
        return ConversationMessage(llmResponse.content, "assistant")
    }

    private fun setInitMessage(initMessage: String) {
        // Only add system message if it doesn't already exist (avoid duplicates when loading from DB)
        val hasSystemMessage = latestConversations.any { it.role == "system" }
        if (!hasSystemMessage) {
            latestConversations.add(0, ConversationMessage(initMessage, "system"))
        }
    }

    @Synchronized
    fun updateSystemPrompt(newSystemPrompt: String) {
        // Find and update the system message (should be at index 0)
        val systemIndex = latestConversations.indexOfFirst { it.role == "system" }
        if (systemIndex >= 0) {
            latestConversations[systemIndex] = ConversationMessage(newSystemPrompt, "system")
        } else {
            // If no system message exists, add it at the beginning
            latestConversations.add(0, ConversationMessage(newSystemPrompt, "system"))
        }
    }

    fun getLastMessage(): String {
        return latestConversations.lastOrNull()?.message ?: ""
    }
}