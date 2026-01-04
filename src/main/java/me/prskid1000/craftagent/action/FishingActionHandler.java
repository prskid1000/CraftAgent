package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.context.ActionStateManager;
import me.prskid1000.craftagent.context.ContextProvider;
import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

/**
 * Handles fishing actions for NPCs.
 * Supports fishing in water.
 * 
 * Formats:
 * - "fish" - Start fishing (cast fishing rod)
 * - "fish stop" - Stop fishing (reel in)
 */
public class FishingActionHandler implements ActionSyntaxProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final ContextProvider contextProvider;
    private final BaseConfig baseConfig;
    
    public FishingActionHandler(ServerPlayerEntity npcEntity, ContextProvider contextProvider, BaseConfig baseConfig) {
        this.npcEntity = npcEntity;
        this.contextProvider = contextProvider;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 1) {
            LogUtil.error("FishingActionHandler: Invalid action format: " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        if (!"fish".equals(actionType)) {
            return false;
        }
        
        String operation = parsed.length > 1 ? parsed[1].toLowerCase() : "";
        
        return switch (operation) {
            case "stop" -> stopFishing();
            case "" -> startFishing();
            default -> {
                LogUtil.error("FishingActionHandler: Unknown fish operation: " + operation);
                yield false;
            }
        };
    }
    
    private boolean startFishing() {
        try {
            // Check if NPC has a fishing rod
            if (!hasFishingRod()) {
                LogUtil.error("FishingActionHandler: NPC doesn't have a fishing rod");
                return false;
            }
            
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            actionState.setAction(ActionStateManager.ActionType.FISHING);
            
            // Use fishing rod (right-click)
            ItemStack fishingRod = getFishingRod();
            if (fishingRod != null) {
                // Set fishing rod in main hand
                npcEntity.setStackInHand(Hand.MAIN_HAND, fishingRod);
                
                // Use item (cast fishing rod) - interact with item in hand
                fishingRod.use(npcEntity.getWorld(), npcEntity, Hand.MAIN_HAND);
                
                return true;
            }
            
            actionState.setIdle();
            return false;
            
        } catch (Exception e) {
            LogUtil.error("FishingActionHandler: Error starting fishing", e);
            return false;
        }
    }
    
    private boolean stopFishing() {
        try {
            // Find and remove fishing bobber entity
            World world = npcEntity.getWorld();
            List<FishingBobberEntity> bobbers = world.getEntitiesByClass(
                FishingBobberEntity.class,
                npcEntity.getBoundingBox().expand(50),
                bobber -> bobber.getPlayerOwner() == npcEntity
            );
            
            if (!bobbers.isEmpty()) {
                bobbers.forEach(bobber -> bobber.remove(Entity.RemovalReason.DISCARDED));
                // Set to idle after stopping
                contextProvider.getActionStateManager().setIdle();
                return true;
            }
            
            contextProvider.getActionStateManager().setIdle();
            return false;
            
        } catch (Exception e) {
            LogUtil.error("FishingActionHandler: Error stopping fishing", e);
            return false;
        }
    }
    
    private boolean hasFishingRod() {
        var inventory = npcEntity.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.FISHING_ROD) {
                return true;
            }
        }
        return false;
    }
    
    private ItemStack getFishingRod() {
        var inventory = npcEntity.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.FISHING_ROD) {
                return stack;
            }
        }
        return null;
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 1) return false;
        
        String actionType = parsed[0].toLowerCase();
        if (!"fish".equals(actionType)) return false;
        
        if (parsed.length > 1) {
            String operation = parsed[1].toLowerCase();
            return "stop".equals(operation);
        }
        
        return true; // "fish" alone is valid
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "fish",
            "fish stop"
        );
    }
}

