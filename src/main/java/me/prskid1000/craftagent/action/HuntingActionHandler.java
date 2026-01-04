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
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

/**
 * Handles hunting actions for NPCs.
 * Supports attacking mobs and collecting drops.
 * 
 * Formats:
 * - "hunt <mob_type>" - Hunt/attack specific mob type
 * - "hunt <entity_name>" - Hunt/attack entity by name
 */
public class HuntingActionHandler implements ActionSyntaxProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final ContextProvider contextProvider;
    private final BaseConfig baseConfig;
    
    public HuntingActionHandler(ServerPlayerEntity npcEntity, ContextProvider contextProvider, BaseConfig baseConfig) {
        this.npcEntity = npcEntity;
        this.contextProvider = contextProvider;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 2) {
            LogUtil.error("HuntingActionHandler: Invalid action format: " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        if (!"hunt".equals(actionType)) {
            return false;
        }
        
        // Format: hunt <mob_type> or hunt <entity_name>
        if (parsed.length < 2) {
            LogUtil.error("HuntingActionHandler: 'hunt' requires mob type or entity name");
            return false;
        }
        
        String targetName = parsed[1].toLowerCase();
        return attackEntity(targetName);
    }
    
    private boolean attackEntity(String targetName) {
        try {
            // Find nearby entity
            List<Entity> nearbyEntities = MCDataUtil.getNearbyEntities(npcEntity);
            Entity target = nearbyEntities.stream()
                .filter(e -> !e.isPlayer()) // Don't attack players
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> {
                    String entityName = e.getType().getName().getString().toLowerCase();
                    return entityName.equals(targetName) || 
                           entityName.contains(targetName) ||
                           e.getName().getString().toLowerCase().equals(targetName);
                })
                .findFirst()
                .orElse(null);
            
            if (target == null) {
                LogUtil.error("HuntingActionHandler: Entity not found: " + targetName);
                return false;
            }
            
            if (!(target instanceof LivingEntity)) {
                LogUtil.error("HuntingActionHandler: Target is not a living entity");
                return false;
            }
            
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            var actionData = new java.util.HashMap<String, Object>();
            actionData.put("target", targetName);
            actionData.put("targetId", target.getId());
            actionState.setAction(ActionStateManager.ActionType.HUNTING, actionData);
            
            // Attack the entity
            LivingEntity livingTarget = (LivingEntity) target;
            
            // Use attack method
            npcEntity.attack(livingTarget);
            
            // Also swing hand for animation
            npcEntity.swingHand(Hand.MAIN_HAND);
            
            return true;
            
        } catch (Exception e) {
            LogUtil.error("HuntingActionHandler: Error attacking entity: " + targetName, e);
            return false;
        }
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 2) return false;
        
        String actionType = parsed[0].toLowerCase();
        return "hunt".equals(actionType);
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "hunt <mob_type>",
            "hunt <entity_name>"
        );
    }
}

