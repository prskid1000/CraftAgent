package me.prskid1000.craftagent.history

import io.github.sashirestela.openai.domain.chat.ChatMessage

/**
 * Converter utilities for converting between ChatMessage (OpenAI/LM Studio API) and ConversationMessage
 */
object MessageConverter {
    /**
     * Converts OpenAI ChatMessage.ResponseMessage to ConversationMessage
     */
    fun ChatMessage.ResponseMessage.toConversationMessage(): ConversationMessage {
        return ConversationMessage(this.content, this.role.toString().lowercase())
    }

    /**
     * Converts ConversationMessage to OpenAI ChatMessage
     */
    fun ConversationMessage.toChatMessage(): ChatMessage {
        val role = ChatMessage.ChatRole.valueOf(this.role.uppercase())
        return if (role == ChatMessage.ChatRole.SYSTEM) {
            ChatMessage.SystemMessage.of(this.message)
        } else {
            ChatMessage.UserMessage.of(this.message)
        }
    }
}
}
