package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.context.ContextProvider;
import me.prskid1000.craftagent.context.NavigationState;
import me.prskid1000.craftagent.util.LogUtil;
import me.prskid1000.craftagent.util.MCDataUtil;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

/**
 * Handles navigation/travel actions for NPCs.
 * Supports traveling to coordinates, entities, and blocks.
 * 
 * Formats:
 * - "travel to <x> <y> <z>" - Travel to coordinates
 * - "travel to entity <entity_name>" - Travel to nearby entity by name
 * - "travel to block <block_type>" - Travel to nearby block by type
 * - "travel stop" - Stop traveling and return to idle
 */
public class NavigationActionHandler implements ActionSyntaxProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final ContextProvider contextProvider;
    private final BaseConfig baseConfig;
    
    public NavigationActionHandler(ServerPlayerEntity npcEntity, ContextProvider contextProvider, BaseConfig baseConfig) {
        this.npcEntity = npcEntity;
        this.contextProvider = contextProvider;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 2) {
            LogUtil.error("NavigationActionHandler: Invalid action format (need at least 2 parts): " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        if (!"travel".equals(actionType)) {
            return false;
        }
        
        String operation = parsed[1].toLowerCase();
        
        return switch (operation) {
            case "to" -> handleTravelTo(parsed);
            case "stop" -> handleTravelStop();
            default -> {
                LogUtil.error("NavigationActionHandler: Unknown travel operation: " + operation);
                yield false;
            }
        };
    }
    
    private boolean handleTravelTo(String[] parsed) {
        if (parsed.length < 3) {
            LogUtil.error("NavigationActionHandler: 'travel to' requires destination. Format: travel to <x> <y> <z> OR travel to entity <name> OR travel to block <type>");
            return false;
        }
        
        String destinationType = parsed[2].toLowerCase();
        
        return switch (destinationType) {
            case "entity" -> travelToEntity(parsed);
            case "block" -> travelToBlock(parsed);
            default -> travelToCoordinates(parsed);
        };
    }
    
    private boolean travelToCoordinates(String[] parsed) {
        // Format: travel to <x> <y> <z>
        if (parsed.length < 5) {
            LogUtil.error("NavigationActionHandler: 'travel to <x> <y> <z>' requires 3 coordinates");
            return false;
        }
        
        try {
            double x = Double.parseDouble(parsed[2]);
            double y = Double.parseDouble(parsed[3]);
            double z = Double.parseDouble(parsed[4]);
            
            BlockPos destination = BlockPos.ofFloored(x, y, z);
            return executeTravel(destination);
            
        } catch (NumberFormatException e) {
            LogUtil.error("NavigationActionHandler: Invalid coordinates. Format: travel to <x> <y> <z>");
            return false;
        }
    }
    
    private boolean travelToEntity(String[] parsed) {
        // Format: travel to entity <entity_name>
        if (parsed.length < 4) {
            LogUtil.error("NavigationActionHandler: 'travel to entity' requires entity name");
            return false;
        }
        
        String entityName = parsed[3];
        
        // Try to find entity by name
        Entity targetEntity = MCDataUtil.getNearbyPlayer(entityName, npcEntity);
        if (targetEntity == null) {
            // Try to find any nearby entity
            List<Entity> nearbyEntities = MCDataUtil.getNearbyEntities(npcEntity);
            targetEntity = nearbyEntities.stream()
                .filter(e -> e.getName().getString().equalsIgnoreCase(entityName) || 
                           e.getName().getString().toLowerCase().contains(entityName.toLowerCase()))
                .findFirst()
                .orElse(null);
        }
        
        if (targetEntity == null) {
            LogUtil.error("NavigationActionHandler: Entity not found: " + entityName);
            return false;
        }
        
        BlockPos destination = targetEntity.getBlockPos();
        return executeTravel(destination);
    }
    
    private boolean travelToBlock(String[] parsed) {
        // Format: travel to block <block_type>
        if (parsed.length < 4) {
            LogUtil.error("NavigationActionHandler: 'travel to block' requires block type");
            return false;
        }
        
        String blockType = parsed[3].toLowerCase();
        
        // Find block in nearby blocks from context
        var nearbyBlocks = contextProvider.getChunkManager().getNearbyBlocks();
        var targetBlock = nearbyBlocks.stream()
            .filter(block -> block.type().toLowerCase().equals(blockType) || 
                           block.type().toLowerCase().contains(blockType))
            .findFirst()
            .orElse(null);
        
        if (targetBlock == null) {
            LogUtil.error("NavigationActionHandler: Block not found nearby: " + blockType);
            return false;
        }
        
        return executeTravel(targetBlock.position());
    }
    
    private boolean executeTravel(BlockPos destination) {
        try {
            NavigationState navState = contextProvider.getNavigationState();
            
            // Set navigation state to traveling
            navState.setTravelingTo(destination);
            
            // Use Minecraft's teleport command for movement
            // This is the simplest approach - NPCs can teleport to destinations
            // For more realistic pathfinding, we could implement step-by-step movement later
            Vec3d destinationVec = destination.toCenterPos();
            
            // Teleport the NPC to the destination
            // Using teleport method which is available on ServerPlayerEntity
            ServerWorld serverWorld = (ServerWorld) npcEntity.getWorld();
            npcEntity.teleport(
                serverWorld,
                destinationVec.x,
                destinationVec.y,
                destinationVec.z,
                java.util.Set.of(),
                npcEntity.getYaw(),
                npcEntity.getPitch(),
                true
            );
            
            // Update navigation state - check if we're close enough
            navState.update(npcEntity.getPos());
            
            return true;
            
        } catch (Exception e) {
            LogUtil.error("NavigationActionHandler: Error executing travel to " + destination, e);
            return false;
        }
    }
    
    private boolean handleTravelStop() {
        try {
            NavigationState navState = contextProvider.getNavigationState();
            navState.setIdle();
            return true;
        } catch (Exception e) {
            LogUtil.error("NavigationActionHandler: Error stopping travel", e);
            return false;
        }
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 2) return false;
        
        String actionType = parsed[0].toLowerCase();
        if (!"travel".equals(actionType)) return false;
        
        String operation = parsed[1].toLowerCase();
        
        return switch (operation) {
            case "to" -> {
                if (parsed.length < 3) yield false;
                String destType = parsed[2].toLowerCase();
                yield switch (destType) {
                    case "entity" -> parsed.length >= 4;
                    case "block" -> parsed.length >= 4;
                    default -> parsed.length >= 5; // coordinates: x y z
                };
            }
            case "stop" -> true;
            default -> false;
        };
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    /**
     * Static method to get action syntax without creating an instance.
     * Used for generating instructions at runtime.
     */
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "travel to <x> <y> <z>",
            "travel to entity <entity_name>",
            "travel to block <block_type>",
            "travel stop"
        );
    }
}

