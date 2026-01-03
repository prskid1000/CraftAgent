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
     * Data class representing a parsed action response from LLM.
     * New format: thought + action array + message
     */
    data class ActionResponse(
        val thought: String? = null,
        val action: List<String> = emptyList(),
        val message: String = ""
    )
    
    /**
     * Legacy data class for backward compatibility.
     */
    data class CommandMessage(
        val command: String = "",
        val message: String = ""
    ) {
        /**
         * Converts ActionResponse to CommandMessage for backward compatibility.
         */
        companion object {
            fun fromActionResponse(response: ActionResponse): CommandMessage {
                // Use first action as command, or empty if no actions
                val command = response.action.firstOrNull() ?: ""
                return CommandMessage(command, response.message)
            }
        }
    }
    
    /**
     * Parses new ActionResponse format (thought, action array, message).
     */
    fun parseActionResponse(content: String): ActionResponse {
        return try {
            parseActionContent(content)
        } catch (_: JsonParseException) {
            val cleanedContent = content
                .replace("```json", "")
                .replace("```", "")
            try {
                parseActionContent(cleanedContent)
            } catch (e: JsonParseException) {
                // Fallback: try to parse as old format
                try {
                    val oldFormat = parse(content)
                    ActionResponse(
                        thought = null,
                        action = if (oldFormat.command.isNotEmpty()) listOf(oldFormat.command) else emptyList(),
                        message = oldFormat.message
                    )
                } catch (e2: Exception) {
                    throw CustomEventException("Failed to parse LLM response. The model may be too small to produce valid JSON.", e2)
                }
            }
        }
    }
    
    private fun parseActionContent(content: String): ActionResponse {
        return gson.fromJson(content, ActionResponse::class.java)
    }
}

