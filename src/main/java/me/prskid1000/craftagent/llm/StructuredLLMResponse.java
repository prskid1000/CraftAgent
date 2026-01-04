package me.prskid1000.craftagent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.prskid1000.craftagent.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Structured response from LLM containing message and actions.
 * Format: {"message": "text", "actions": ["action1", "action2", ...]}
 */
public class StructuredLLMResponse {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String message;
    private final List<String> actions;
    
    public StructuredLLMResponse(String message, List<String> actions) {
        this.message = message != null ? message : "";
        this.actions = actions != null ? actions : new ArrayList<>();
    }
    
    public String getMessage() {
        return message;
    }
    
    public List<String> getActions() {
        return new ArrayList<>(actions);
    }
    
    public boolean hasMessage() {
        return message != null && !message.isEmpty();
    }
    
    /**
     * Checks if message is non-empty (not empty or whitespace-only).
     * Used to determine if message should be broadcast to players.
     */
    public boolean hasNonEmptyMessage() {
        return message != null && !message.trim().isEmpty();
    }
    
    public boolean hasActions() {
        return actions != null && !actions.isEmpty();
    }
    
    /**
     * Parses JSON string into StructuredLLMResponse.
     * Handles both structured format and plain text fallback.
     * 
     * @param jsonContent The JSON string from LLM response
     * @return Parsed StructuredLLMResponse
     */
    public static StructuredLLMResponse parse(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return new StructuredLLMResponse("", new ArrayList<>());
        }
        
        String trimmed = jsonContent.trim();
        
        // Try to parse as JSON
        try {
            // Remove markdown code blocks if present
            if (trimmed.startsWith("```json")) {
                trimmed = trimmed.substring(7);
            }
            if (trimmed.startsWith("```")) {
                trimmed = trimmed.substring(3);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
            
            Map<String, Object> jsonMap = objectMapper.readValue(trimmed, Map.class);
            
            String message = "";
            List<String> actions = new ArrayList<>();
            
            // Extract message
            if (jsonMap.containsKey("message")) {
                Object msgObj = jsonMap.get("message");
                if (msgObj != null) {
                    message = msgObj.toString();
                }
            }
            
            // Extract actions
            if (jsonMap.containsKey("actions")) {
                Object actionsObj = jsonMap.get("actions");
                if (actionsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> actionsList = (List<Object>) actionsObj;
                    for (Object action : actionsList) {
                        if (action != null) {
                            actions.add(action.toString());
                        }
                    }
                } else if (actionsObj != null) {
                    // Single action as string
                    actions.add(actionsObj.toString());
                }
            }
            
            return new StructuredLLMResponse(message, actions);
        } catch (Exception e) {
            // Fallback: treat as plain text message
                    LogUtil.info("Failed to parse structured response, treating as plain text: " + e.getMessage());
            return new StructuredLLMResponse(trimmed, new ArrayList<>());
        }
    }
    
    /**
     * Creates a StructuredLLMResponse from a plain text message (backward compatibility).
     */
    public static StructuredLLMResponse fromPlainText(String text) {
        return new StructuredLLMResponse(text, new ArrayList<>());
    }
}

