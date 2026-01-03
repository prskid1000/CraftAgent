package me.prskid1000.craftagent.exception;

/**
 * Base exception class for all CraftAgent-related exceptions.
 * Provides factory methods for creating specific exception types.
 */
public class CraftAgentException extends RuntimeException {
    
    public CraftAgentException(String message) {
        super(message);
    }

    public CraftAgentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Factory method for LLM service-related exceptions
     */
    public static CraftAgentException llmService(String message) {
        return new CraftAgentException("LLM Service Error: " + message);
    }

    public static CraftAgentException llmService(String message, Throwable cause) {
        return new CraftAgentException("LLM Service Error: " + message, cause);
    }

    /**
     * Factory method for NPC creation-related exceptions
     */
    public static CraftAgentException npcCreation(String message) {
        return new CraftAgentException("NPC Creation Error: " + message);
    }

    public static CraftAgentException npcCreation(String message, Throwable cause) {
        return new CraftAgentException("NPC Creation Error: " + message, cause);
    }

    /**
     * Factory method for custom event-related exceptions
     */
    public static CraftAgentException customEvent(String message, Throwable cause) {
        return new CraftAgentException("Custom Event Error: " + message, cause);
    }
}

