package me.prskid1000.craftagent.history

import com.fasterxml.jackson.databind.ObjectMapper
import me.prskid1000.craftagent.constant.Instructions
import me.prskid1000.craftagent.llm.LLMClient

class ConversationHistory(
    private val llmClient: LLMClient,
    initMessage: String,
    val latestConversations: MutableList<Message>,
    private val maxHistoryLength: Int = 5
) {
    companion object {
        private val objectMapper = ObjectMapper()
    }

    init {
        setInitMessage(initMessage)
    }

    @Synchronized
    fun add(message: Message) {
        latestConversations.add(message)

        if (latestConversations.size >= maxHistoryLength) {
            updateConversations()
        }
    }

    private fun updateConversations() {
        val removeCount = maxHistoryLength / 3
        val toSummarize = latestConversations.subList(1, removeCount).toList()
        val message = summarize(toSummarize)
        latestConversations.removeAll(toSummarize)
        latestConversations.add(1, message)
    }

    private fun summarize(conversations: List<Message>): Message {
        val summarizeMessage = Message(
            Instructions.SUMMARY_PROMPT.format( objectMapper.writeValueAsString(conversations)),
            "user")
        // Use chatWithTools and extract content (summarization doesn't need tool calls)
        val toolResponse = llmClient.chatWithTools(listOf(summarizeMessage))
        return Message(toolResponse.content, "assistant")
    }

    private fun setInitMessage(initMessage: String) {
        // Only add system message if it doesn't already exist (avoid duplicates when loading from DB)
        val hasSystemMessage = latestConversations.any { it.role == "system" }
        if (!hasSystemMessage) {
            latestConversations.add(0, Message(initMessage, "system"))
        }
    }

    fun getLastMessage(): String {
        return latestConversations.last().message
    }
}