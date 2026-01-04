package me.prskid1000.craftagent.llm;

/**
 * Response from LLM containing content.
 * Supports both plain text and structured output (message + actions).
 */
public class LLMResponse {
    private final String content;  // Raw JSON content from LLM
    private StructuredLLMResponse structuredResponse;  // Parsed structured response (lazy-loaded)

    public LLMResponse(String content) {
        this.content = content != null ? content : "";
    }

    /**
     * Gets the raw content from LLM (JSON string).
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the parsed structured response (message + actions).
     * Parses the content on first access.
     */
    public StructuredLLMResponse getStructuredResponse() {
        if (structuredResponse == null) {
            structuredResponse = StructuredLLMResponse.parse(content);
        }
        return structuredResponse;
    }

    /**
     * Gets the message text from structured response.
     * Falls back to raw content if parsing fails.
     */
    public String getMessage() {
        StructuredLLMResponse structured = getStructuredResponse();
        if (structured.hasMessage()) {
            return structured.getMessage();
        }
        // Fallback to raw content for backward compatibility
        return content;
    }

    /**
     * Gets the actions list from structured response.
     */
    public java.util.List<String> getActions() {
        return getStructuredResponse().getActions();
    }

    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
    
    /**
     * Checks if response has a message (non-empty).
     */
    public boolean hasMessage() {
        return getStructuredResponse().hasMessage();
    }
    
    /**
     * Checks if response has actions.
     */
    public boolean hasActions() {
        return getStructuredResponse().hasActions();
    }
}

