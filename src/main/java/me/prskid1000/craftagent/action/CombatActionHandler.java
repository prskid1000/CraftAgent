package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.context.ActionStateManager;
import me.prskid1000.craftagent.context.ContextProvider;
import me.prskid1000.craftagent.util.LogUtil;
import me.prskid1000.craftagent.util.MCDataUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.Arrays;
import java.util.List;

/**
 * Handles combat actions for NPCs.
 * Supports attacking entities and defending.
 * 
 * Formats:
 * - "attack <entity_name>" - Attack specific entity
 * - "attack <entity_type>" - Attack entity by type
 * - "defend" - Enter defensive stance
 */
public class CombatActionHandler implements ActionSyntaxProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final ContextProvider contextProvider;
    private final BaseConfig baseConfig;
    
    public CombatActionHandler(ServerPlayerEntity npcEntity, ContextProvider contextProvider, BaseConfig baseConfig) {
        this.npcEntity = npcEntity;
        this.contextProvider = contextProvider;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 1) {
            LogUtil.error("CombatActionHandler: Invalid action format: " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        
        return switch (actionType) {
            case "attack" -> attackEntity(parsed);
            case "defend" -> defend();
            default -> false;
        };
    }
    
    private boolean attackEntity(String[] parsed) {
        // Format: attack <entity_name> or attack <entity_type>
        if (parsed.length < 2) {
            LogUtil.error("CombatActionHandler: 'attack' requires entity name or type");
            return false;
        }
        
        String targetName = parsed[1].toLowerCase();
        
        try {
            // Find nearby entity
            List<Entity> nearbyEntities = MCDataUtil.getNearbyEntities(npcEntity);
            Entity target = nearbyEntities.stream()
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> {
                    String entityName = e.getType().getName().getString().toLowerCase();
                    String displayName = e.getName().getString().toLowerCase();
                    return entityName.equals(targetName) || 
                           entityName.contains(targetName) ||
                           displayName.equals(targetName) ||
                           displayName.contains(targetName);
                })
                .findFirst()
                .orElse(null);
            
            if (target == null) {
                LogUtil.error("CombatActionHandler: Entity not found: " + targetName);
                return false;
            }
            
            if (!(target instanceof LivingEntity)) {
                LogUtil.error("CombatActionHandler: Target is not a living entity");
                return false;
            }
            
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            var actionData = new java.util.HashMap<String, Object>();
            actionData.put("target", targetName);
            actionData.put("targetId", target.getId());
            actionState.setAction(ActionStateManager.ActionType.COMBAT, actionData);
            
            // Attack the entity
            LivingEntity livingTarget = (LivingEntity) target;
            
            // Use attack method
            npcEntity.attack(livingTarget);
            
            // Swing hand for animation
            npcEntity.swingHand(Hand.MAIN_HAND);
            
            return true;
            
        } catch (Exception e) {
            LogUtil.error("CombatActionHandler: Error attacking entity: " + targetName, e);
            return false;
        }
    }
    
    private boolean defend() {
        try {
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            var actionData = new java.util.HashMap<String, Object>();
            actionData.put("stance", "defensive");
            actionState.setAction(ActionStateManager.ActionType.COMBAT, actionData);
            
            // Enter defensive stance (could be implemented as holding shield, etc.)
            // For now, just log the action
            // In a full implementation, you might equip a shield or change behavior
            
            // Check if NPC has a shield
            var inventory = npcEntity.getInventory();
            // Could equip shield to off-hand here
            
            return true;
            
        } catch (Exception e) {
            LogUtil.error("CombatActionHandler: Error defending", e);
            return false;
        }
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 1) return false;
        
        String actionType = parsed[0].toLowerCase();
        
        return switch (actionType) {
            case "attack" -> parsed.length >= 2;
            case "defend" -> true;
            default -> false;
        };
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "attack <entity_name>",
            "attack <entity_type>",
            "defend"
        );
    }
}

