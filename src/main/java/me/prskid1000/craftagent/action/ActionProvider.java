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
    private final MiningActionHandler miningHandler;
    private final BuildingActionHandler buildingHandler;
    private final CraftingActionHandler craftingHandler;
    private final HuntingActionHandler huntingHandler;
    private final FarmingActionHandler farmingHandler;
    private final FishingActionHandler fishingHandler;
    private final CombatActionHandler combatHandler;
    
    public ActionProvider(MemoryActionHandler memoryHandler, CommunicationActionHandler communicationHandler) {
        this.memoryHandler = memoryHandler;
        this.communicationHandler = communicationHandler;
        this.navigationHandler = null;
        this.miningHandler = null;
        this.buildingHandler = null;
        this.craftingHandler = null;
        this.huntingHandler = null;
        this.farmingHandler = null;
        this.fishingHandler = null;
        this.combatHandler = null;
    }
    
    public ActionProvider(MemoryActionHandler memoryHandler, CommunicationActionHandler communicationHandler, NavigationActionHandler navigationHandler) {
        this.memoryHandler = memoryHandler;
        this.communicationHandler = communicationHandler;
        this.navigationHandler = navigationHandler;
        this.miningHandler = null;
        this.buildingHandler = null;
        this.craftingHandler = null;
        this.huntingHandler = null;
        this.farmingHandler = null;
        this.fishingHandler = null;
        this.combatHandler = null;
    }
    
    public ActionProvider(
        MemoryActionHandler memoryHandler,
        CommunicationActionHandler communicationHandler,
        NavigationActionHandler navigationHandler,
        MiningActionHandler miningHandler,
        BuildingActionHandler buildingHandler,
        CraftingActionHandler craftingHandler,
        HuntingActionHandler huntingHandler,
        FarmingActionHandler farmingHandler,
        FishingActionHandler fishingHandler,
        CombatActionHandler combatHandler
    ) {
        this.memoryHandler = memoryHandler;
        this.communicationHandler = communicationHandler;
        this.navigationHandler = navigationHandler;
        this.miningHandler = miningHandler;
        this.buildingHandler = buildingHandler;
        this.craftingHandler = craftingHandler;
        this.huntingHandler = huntingHandler;
        this.farmingHandler = farmingHandler;
        this.fishingHandler = fishingHandler;
        this.combatHandler = combatHandler;
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
        if (miningHandler != null && miningHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) miningHandler).getActionSyntax());
        }
        if (buildingHandler != null && buildingHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) buildingHandler).getActionSyntax());
        }
        if (craftingHandler != null && craftingHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) craftingHandler).getActionSyntax());
        }
        if (huntingHandler != null && huntingHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) huntingHandler).getActionSyntax());
        }
        if (farmingHandler != null && farmingHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) farmingHandler).getActionSyntax());
        }
        if (fishingHandler != null && fishingHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) fishingHandler).getActionSyntax());
        }
        if (combatHandler != null && combatHandler instanceof ActionSyntaxProvider) {
            syntax.addAll(((ActionSyntaxProvider) combatHandler).getActionSyntax());
        }
        return syntax;
    }
    
    /**
     * Static method to get all available action syntax without creating an instance.
     * Used for generating instructions at runtime and command suggestions.
     * 
     * IMPORTANT: When adding a new handler, add its getStaticActionSyntax() call here
     * to ensure command suggestions work automatically.
     * 
     * @return List of action syntax strings
     */
    public static List<String> getAllStaticActionSyntax() {
        List<String> syntax = new ArrayList<>();
        syntax.addAll(MemoryActionHandler.getStaticActionSyntax());
        syntax.addAll(CommunicationActionHandler.getStaticActionSyntax());
        syntax.addAll(NavigationActionHandler.getStaticActionSyntax());
        syntax.addAll(MiningActionHandler.getStaticActionSyntax());
        syntax.addAll(BuildingActionHandler.getStaticActionSyntax());
        syntax.addAll(CraftingActionHandler.getStaticActionSyntax());
        syntax.addAll(HuntingActionHandler.getStaticActionSyntax());
        syntax.addAll(FarmingActionHandler.getStaticActionSyntax());
        syntax.addAll(FishingActionHandler.getStaticActionSyntax());
        syntax.addAll(CombatActionHandler.getStaticActionSyntax());
        return syntax;
    }
    
    public boolean executeAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length == 0) return false;
        
        String actionType = parsed[0].toLowerCase();
        
        return switch (actionType) {
            case "sharedbook", "privatebook" -> memoryHandler.handleAction(originalAction, parsed);
            case "mail" -> communicationHandler.handleAction(originalAction, parsed);
            case "travel" -> navigationHandler != null && navigationHandler.handleAction(originalAction, parsed);
            case "mine" -> miningHandler != null && miningHandler.handleAction(originalAction, parsed);
            case "build", "place" -> buildingHandler != null && buildingHandler.handleAction(originalAction, parsed);
            case "craft" -> craftingHandler != null && craftingHandler.handleAction(originalAction, parsed);
            case "hunt" -> huntingHandler != null && huntingHandler.handleAction(originalAction, parsed);
            case "farm" -> farmingHandler != null && farmingHandler.handleAction(originalAction, parsed);
            case "fish" -> fishingHandler != null && fishingHandler.handleAction(originalAction, parsed);
            case "attack", "defend" -> combatHandler != null && combatHandler.handleAction(originalAction, parsed);
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
            case "mine" -> miningHandler != null && miningHandler.isValidAction(action, parsed);
            case "build", "place" -> buildingHandler != null && buildingHandler.isValidAction(action, parsed);
            case "craft" -> craftingHandler != null && craftingHandler.isValidAction(action, parsed);
            case "hunt" -> huntingHandler != null && huntingHandler.isValidAction(action, parsed);
            case "farm" -> farmingHandler != null && farmingHandler.isValidAction(action, parsed);
            case "fish" -> fishingHandler != null && fishingHandler.isValidAction(action, parsed);
            case "attack", "defend" -> combatHandler != null && combatHandler.isValidAction(action, parsed);
            default -> false;
        };
    }
}

