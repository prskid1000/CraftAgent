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
    
    public boolean executeAction(String action) {
        if (action == null || action.trim().isEmpty()) return false;
        
        String[] parts = action.trim().split("\\s+", 2);
        if (parts.length < 1) return false;
        
        String actionType = parts[0].toLowerCase();
        
        return switch (actionType) {
            case "sharedbook", "privatebook" -> memoryHandler.handleAction(action);
            case "mail" -> communicationHandler.handleAction(action);
            default -> false;
        };
    }
    
    public boolean isValidAction(String action) {
        if (action == null || action.trim().isEmpty()) return false;
        
        String[] parts = action.trim().split("\\s+", 2);
        if (parts.length < 1) return false;
        
        String actionType = parts[0].toLowerCase();
        
        return switch (actionType) {
            case "sharedbook", "privatebook" -> memoryHandler.isValidAction(action);
            case "mail" -> communicationHandler.isValidAction(action);
            default -> false;
        };
    }
}

