package me.prskid1000.craftagent.llm;

import java.util.Map;

/**
 * Schema definitions for LLM structured output.
 */
public class LLMSchema {

    private LLMSchema() {}

    /**
     * Creates a message schema for structured output.
     */
    public static Map<String, Object> getMessageSchema() {
        return StructuredOutputSchema.getMessageSchema();
    }
}

