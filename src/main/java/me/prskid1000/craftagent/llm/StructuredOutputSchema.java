package me.prskid1000.craftagent.llm;

import java.util.Map;

/**
 * JSON Schema definitions for structured output from LLMs.
 * This ensures the LLM returns responses in a consistent format.
 */
public class StructuredOutputSchema {

    private StructuredOutputSchema() {}

    /**
     * Returns the JSON schema for message output.
     * Structured format: message string and actions array.
     */
    public static Map<String, Object> getMessageSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "message", Map.of(
                    "type", "string",
                    "description", "Chat message to say (under 500 characters). Use empty string \"\" if no message."
                ),
                "actions", Map.of(
                    "type", "array",
                    "items", Map.of(
                        "type", "string",
                        "description", "Action to perform (e.g., 'mine stone 10', 'craft wooden_pickaxe', 'move to 100 64 200')"
                    ),
                    "description", "List of actions to execute. Can be empty array [] if no actions."
                )
            ),
            "required", new String[]{"message", "actions"}
        );
    }
}

