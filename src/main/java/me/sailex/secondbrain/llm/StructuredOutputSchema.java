package me.sailex.secondbrain.llm;

import java.util.Map;

/**
 * JSON Schema definitions for structured output from LLMs.
 * This ensures the LLM returns responses in a consistent format.
 */
public class StructuredOutputSchema {

    private StructuredOutputSchema() {}

    /**
     * Returns the JSON schema for CommandMessage structure.
     * Used by Ollama's format parameter and OpenAI's tools/response_format.
     */
    public static Map<String, Object> getCommandMessageSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "command", Map.of(
                    "type", "string",
                    "description", "A valid AltoClef command from the available commands list. Must be exactly one command name, not a description or sentence. Use 'idle' to do nothing."
                ),
                "message", Map.of(
                    "type", "string",
                    "description", "An optional in-character chat message (under 250 characters). Use empty string \"\" if the NPC should not speak."
                )
            ),
            "required", new String[]{"command", "message"}
        );
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

