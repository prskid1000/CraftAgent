package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Executes actions parsed from LLM structured output.
 * Routes actions to appropriate handlers via ActionProvider.
 * 
 * Actions format examples:
 * - Memory: "sharedbook add location_iron_mine Iron mine at 150, 64, -200"
 * - Memory: "privatebook add player_alice Alice is friendly"
 * - Minecraft: "mine stone 10" (TODO: to be implemented)
 * - Minecraft: "craft wooden_pickaxe" (TODO: to be implemented)
 */
public class ActionExecutor {
    
    private final ServerPlayerEntity entity;
    private final ActionProvider actionProvider;
    
    public ActionExecutor(ServerPlayerEntity entity, ActionProvider actionProvider) {
        this.entity = entity;
        this.actionProvider = actionProvider;
    }
    
    /**
     * Executes a list of actions.
     * 
     * @param actions List of action strings to execute
     */
    public void executeActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        
        for (String action : actions) {
            if (action == null || action.trim().isEmpty()) {
                continue;
            }
            
            try {
                executeAction(action.trim());
            } catch (Exception e) {
                LogUtil.error("Error executing action: " + action, e);
            }
        }
    }
    
    /**
     * Executes a single action.
     * 
     * @param action The action string to execute
     */
    public void executeAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            return;
        }
        
        boolean success = actionProvider.executeAction(action);
        if (success) {
            LogUtil.info("Action executed successfully: " + action);
        } else {
            LogUtil.info("Action execution failed or not implemented: " + action);
        }
    }
    
    /**
     * Validates if an action string is in a valid format.
     * 
     * @param action The action string to validate
     * @return true if action format is valid
     */
    public boolean isValidAction(String action) {
        return actionProvider.isValidAction(action);
    }
}

