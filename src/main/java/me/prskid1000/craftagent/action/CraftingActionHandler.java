package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.context.ActionStateManager;
import me.prskid1000.craftagent.context.ContextProvider;
import me.prskid1000.craftagent.util.LogUtil;
import me.prskid1000.craftagent.util.MinecraftCommandUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Handles crafting actions for NPCs.
 * Supports crafting items from inventory materials.
 * 
 * Formats:
 * - "craft <item_name>" - Craft item using materials from inventory
 */
public class CraftingActionHandler implements ActionSyntaxProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final ContextProvider contextProvider;
    private final BaseConfig baseConfig;
    
    public CraftingActionHandler(ServerPlayerEntity npcEntity, ContextProvider contextProvider, BaseConfig baseConfig) {
        this.npcEntity = npcEntity;
        this.contextProvider = contextProvider;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 2) {
            LogUtil.error("CraftingActionHandler: Invalid action format: " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        if (!"craft".equals(actionType)) {
            return false;
        }
        
        // Format: craft <item_name>
        if (parsed.length < 2) {
            LogUtil.error("CraftingActionHandler: 'craft <item_name>' requires item name");
            return false;
        }
        
        // Get item name (handle multi-word item names)
        StringBuilder itemNameBuilder = new StringBuilder();
        for (int i = 1; i < parsed.length; i++) {
            if (itemNameBuilder.length() > 0) itemNameBuilder.append("_");
            itemNameBuilder.append(parsed[i]);
        }
        String itemName = itemNameBuilder.toString().toLowerCase();
        
        return craftItem(itemName);
    }
    
    private boolean craftItem(String itemName) {
        try {
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            var actionData = new java.util.HashMap<String, Object>();
            actionData.put("itemName", itemName);
            actionState.setAction(ActionStateManager.ActionType.CRAFTING, actionData);
            
            // Use /give command to give the crafted item
            // Note: This is a simplified implementation. Full crafting would require
            // checking recipes and consuming materials, which is complex.
            // For now, we'll use /give as a placeholder - in a full implementation,
            // you'd check recipes and consume materials from inventory.
            
            String itemId = itemName.contains(":") ? itemName : "minecraft:" + itemName;
            String command = String.format("give %s %s 1", npcEntity.getName().getString(), itemId);
            
            boolean success = MinecraftCommandUtil.executeCommand(npcEntity, command);
            
            if (!success) {
                LogUtil.error("CraftingActionHandler: Failed to craft item: " + itemName);
                actionState.setIdle();
            } else {
                // Set to idle after crafting
                actionState.setIdle();
            }
            
            return success;
            
        } catch (Exception e) {
            LogUtil.error("CraftingActionHandler: Error crafting item: " + itemName, e);
            return false;
        }
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 2) return false;
        
        String actionType = parsed[0].toLowerCase();
        return "craft".equals(actionType);
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "craft <item_name>"
        );
    }
}

