@file:JvmName("MessageConverter")
package me.sailex.secondbrain.history

import io.github.sashirestela.openai.domain.chat.ChatMessage

// OpenAI-compatible API (used by LM Studio)
fun ChatMessage.ResponseMessage.toMessage(): Message {
    return Message(this.content, this.role.toString().lowercase())
}

fun Message.toChatMessage(): ChatMessage {
    val role = ChatMessage.ChatRole.valueOf(this.role.uppercase())
    return if (role == ChatMessage.ChatRole.SYSTEM) {
        ChatMessage.SystemMessage.of(this.message)
    } else {
        ChatMessage.UserMessage.of(this.message)
    }
}
