package me.prskid1000.craftagent.action;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes actions to appropriate handlers.
 */
public class ActionProvider {
    
    private final MemoryActionHandler memoryHandler;
    private final CommunicationActionHandler communicationHandler;
    private final NavigationActionHandler navigationHandler;
    
    public ActionProvider(MemoryActionHandler memoryHandler, CommunicationActionHandler communicationHandler) {
        this.memoryHandler = memoryHandler;
        this.communicationHandler = communicationHandler;
        this.navigationHandler = null;
    }
    
    public ActionProvider(MemoryActionHandler memoryHandler, CommunicationActionHandler communicationHandler, NavigationActionHandler navigationHandler) {
        this.memoryHandler = memoryHandler;
        this.communicationHandler = communicationHandler;
        this.navigationHandler = navigationHandler;
    }
    
    /**
     * Gets all available action syntax from registered handlers.
     * 
     * @return List of action syntax strings
     */
    public List<String> getAllActionSyntax() {
        List<String> syntax = new ArrayList<>();
        if (memoryHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) memoryHandler).getActionSyntax());
        }
        if (communicationHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) communicationHandler).getActionSyntax());
        }
        if (navigationHandler != null && navigationHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) navigationHandler).getActionSyntax());
        }
        return syntax;
    }
    
    /**
     * Static method to get all available action syntax without creating an instance.
     * Used for generating instructions at runtime.
     * 
     * @return List of action syntax strings
     */
    public static List<String> getAllStaticActionSyntax() {
        List<String> syntax = new ArrayList<>();
        syntax.addAll(MemoryActionHandler.getStaticActionSyntax());
        syntax.addAll(CommunicationActionHandler.getStaticActionSyntax());
        syntax.addAll(NavigationActionHandler.getStaticActionSyntax());
        return syntax;
    }
    
    public boolean executeAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length == 0) return false;
        
        String actionType = parsed[0].toLowerCase();
        
        return switch (actionType) {
            case "sharedbook", "privatebook" -> memoryHandler.handleAction(originalAction, parsed);
            case "mail" -> communicationHandler.handleAction(originalAction, parsed);
            case "travel" -> navigationHandler != null && navigationHandler.handleAction(originalAction, parsed);
            default -> false;
        };
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length == 0) return false;
        
        String actionType = parsed[0].toLowerCase();
        
        return switch (actionType) {
            case "sharedbook", "privatebook" -> memoryHandler.isValidAction(action, parsed);
            case "mail" -> communicationHandler.isValidAction(action, parsed);
            case "travel" -> navigationHandler != null && navigationHandler.isValidAction(action, parsed);
            default -> false;
        };
    }
}

