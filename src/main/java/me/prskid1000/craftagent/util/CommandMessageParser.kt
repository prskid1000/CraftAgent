package me.prskid1000.craftagent.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import me.prskid1000.craftagent.event.CustomEventException

/**
 * Parser for CommandMessage JSON responses from LLM.
 * Handles parsing with fallback for markdown-wrapped JSON.
 */
class CommandMessageParser {
    companion object {
        private val gson = GsonBuilder()
            .setLenient()
            .create()
    }

    /**
     * Parses a JSON string into a CommandMessage.
     * Attempts to clean markdown code blocks if initial parsing fails.
     * 
     * @param content The JSON string to parse
     * @return Parsed CommandMessage
     * @throws CustomEventException if parsing fails after cleanup attempts
     */
    fun parse(content: String): CommandMessage {
        return try {
            parseContent(content)
        } catch (_: JsonParseException) {
            val cleanedContent = content
                .replace("```json", "")
                .replace("```", "")
            try {
                parseContent(cleanedContent)
            } catch (e: JsonParseException) {
                throw CustomEventException("The selected model may be too small to understand the context or to reliably produce valid JSON. " +
                        "Please switch to a larger or more capable LLM model.", e)
            }
        }
    }

    private fun parseContent(content: String): CommandMessage {
        return gson.fromJson(content, CommandMessage::class.java)
    }

    /**
     * Data class representing a parsed command message from LLM response.
     */
    data class CommandMessage(
        val command: String,
        val message: String
    )
}

