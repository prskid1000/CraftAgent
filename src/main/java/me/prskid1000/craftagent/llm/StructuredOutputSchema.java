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
     * Simple format: just a message string.
     */
    public static Map<String, Object> getMessageSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "message", Map.of(
                    "type", "string",
                    "description", "Chat message to say (under 500 characters). Use empty string \"\" if no message."
                )
            ),
            "required", new String[]{"message"}
        );
    }
}

