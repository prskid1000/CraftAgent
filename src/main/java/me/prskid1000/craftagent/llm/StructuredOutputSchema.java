package me.prskid1000.craftagent.llm;

import java.util.Map;

/**
 * JSON Schema definitions for structured output from LLMs.
 * This ensures the LLM returns responses in a consistent format.
 */
public class StructuredOutputSchema {

    private StructuredOutputSchema() {}

    /**
     * Returns the JSON schema for ActionResponse structure.
     * Uses simplified format: thought + action array of custom commands.
     */
    public static Map<String, Object> getActionResponseSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "thought", Map.of(
                    "type", "string",
                    "description", "Your reasoning about what to do next. Keep it brief (1-2 sentences)."
                ),
                "action", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "description", "Array of custom commands to execute in sequence. Use simple commands like 'walk forward', 'get wood', 'craft pickaxe', 'save location', etc. See available custom commands in the system prompt. Use 'idle' to do nothing."
                ),
                "message", Map.of(
                    "type", "string",
                    "description", "Optional chat message to say (under 250 characters). Use empty string \"\" if no message."
                )
            ),
            "required", new String[]{"thought", "action", "message"}
        );
    }
    
    /**
     * Legacy method - kept for backward compatibility.
     * Returns the JSON schema for CommandMessage structure.
     */
    public static Map<String, Object> getCommandMessageSchema() {
        return getActionResponseSchema();
    }

    /**
     * Returns the JSON schema as a JSON string for embedding in prompts.
     */
    public static String getCommandMessageSchemaJson() {
        return """
            {
              "type": "object",
              "properties": {
                "command": {
                  "type": "string",
                  "description": "A valid AltoClef command from the available commands list. Must be exactly one command name, not a description or sentence. Use 'idle' to do nothing."
                },
                "message": {
                  "type": "string",
                  "description": "An optional in-character chat message (under 250 characters). Use empty string \\\"\\\" if the NPC should not speak."
                }
              },
              "required": ["command", "message"]
            }
            """;
    }
}

