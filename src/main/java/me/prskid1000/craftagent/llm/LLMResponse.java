package me.prskid1000.craftagent.llm;

/**
 * Response from LLM containing content.
 */
public class LLMResponse {
    private final String content;  // Chat message content

    public LLMResponse(String content) {
        this.content = content != null ? content : "";
    }

    public String getContent() {
        return content;
    }

    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
}

