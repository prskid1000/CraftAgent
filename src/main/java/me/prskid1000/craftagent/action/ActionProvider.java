package me.prskid1000.craftagent.action;

/**
 * Routes actions to appropriate handlers.
 */
public class ActionProvider {
    
    private final MemoryActionHandler memoryHandler;
    private final CommunicationActionHandler communicationHandler;
    
    public ActionProvider(MemoryActionHandler memoryHandler, CommunicationActionHandler communicationHandler) {
        this.memoryHandler = memoryHandler;
        this.communicationHandler = communicationHandler;
    }
    
    public boolean executeAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length == 0) return false;
        
        String actionType = parsed[0].toLowerCase();
        
        return switch (actionType) {
            case "sharedbook", "privatebook" -> memoryHandler.handleAction(originalAction, parsed);
            case "mail" -> communicationHandler.handleAction(originalAction, parsed);
            default -> false;
        };
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length == 0) return false;
        
        String actionType = parsed[0].toLowerCase();
        
        return switch (actionType) {
            case "sharedbook", "privatebook" -> memoryHandler.isValidAction(action, parsed);
            case "mail" -> communicationHandler.isValidAction(action, parsed);
            default -> false;
        };
    }
}

