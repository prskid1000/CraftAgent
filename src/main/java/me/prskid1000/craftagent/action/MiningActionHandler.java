package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.context.ActionStateManager;
import me.prskid1000.craftagent.context.ContextProvider;
import me.prskid1000.craftagent.util.LogUtil;
import me.prskid1000.craftagent.util.MinecraftCommandUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

/**
 * Handles mining actions for NPCs.
 * Supports breaking blocks and collecting resources.
 * 
 * Formats:
 * - "mine <block_type> [count]" - Mine specific block type (default: 1)
 * - "mine at <x> <y> <z>" - Mine block at specific coordinates
 */
public class MiningActionHandler implements ActionSyntaxProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final ContextProvider contextProvider;
    private final BaseConfig baseConfig;
    
    public MiningActionHandler(ServerPlayerEntity npcEntity, ContextProvider contextProvider, BaseConfig baseConfig) {
        this.npcEntity = npcEntity;
        this.contextProvider = contextProvider;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 2) {
            LogUtil.error("MiningActionHandler: Invalid action format: " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        if (!"mine".equals(actionType)) {
            return false;
        }
        
        String operation = parsed[1].toLowerCase();
        
        return switch (operation) {
            case "at" -> mineAtCoordinates(parsed);
            default -> mineBlockType(parsed);
        };
    }
    
    private boolean mineBlockType(String[] parsed) {
        // Format: mine <block_type> [count]
        if (parsed.length < 2) {
            LogUtil.error("MiningActionHandler: 'mine <block_type>' requires block type");
            return false;
        }
        
        String blockType = parsed[1].toLowerCase();
        int count = parsed.length > 2 ? parseInt(parsed[2], 1) : 1;
        
        // Set action state
        var actionState = contextProvider.getActionStateManager();
        var actionData = new java.util.HashMap<String, Object>();
        actionData.put("blockType", blockType);
        actionData.put("count", count);
        actionData.put("mined", 0);
        actionState.setAction(ActionStateManager.ActionType.MINING, actionData);
        
        // Find nearby blocks of this type
        var nearbyBlocks = contextProvider.getChunkManager().getNearbyBlocks();
        var targetBlocks = nearbyBlocks.stream()
            .filter(block -> block.type().toLowerCase().equals(blockType) || 
                           block.type().toLowerCase().contains(blockType))
            .limit(count)
            .toList();
        
        if (targetBlocks.isEmpty()) {
            LogUtil.error("MiningActionHandler: No blocks of type found nearby: " + blockType);
            actionState.setIdle();
            return false;
        }
        
        // Mine each block
        boolean success = true;
        int mined = 0;
        for (var blockData : targetBlocks) {
            if (breakBlock(blockData.position())) {
                mined++;
                actionState.updateActionData("mined", mined);
            } else {
                success = false;
            }
        }
        
        // If all blocks mined, set to idle
        if (mined >= count) {
            actionState.setIdle();
        }
        
        return success;
    }
    
    private boolean mineAtCoordinates(String[] parsed) {
        // Format: mine at <x> <y> <z>
        if (parsed.length < 5) {
            LogUtil.error("MiningActionHandler: 'mine at <x> <y> <z>' requires 3 coordinates");
            return false;
        }
        
        try {
            int x = Integer.parseInt(parsed[2]);
            int y = Integer.parseInt(parsed[3]);
            int z = Integer.parseInt(parsed[4]);
            
            BlockPos targetPos = new BlockPos(x, y, z);
            
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            var actionData = new java.util.HashMap<String, Object>();
            actionData.put("position", targetPos);
            actionState.setAction(ActionStateManager.ActionType.MINING, actionData);
            
            boolean success = breakBlock(targetPos);
            
            // Set to idle after mining
            if (success) {
                actionState.setIdle();
            }
            
            return success;
            
        } catch (NumberFormatException e) {
            LogUtil.error("MiningActionHandler: Invalid coordinates");
            return false;
        }
    }
    
    private boolean breakBlock(BlockPos pos) {
        try {
            ServerWorld world = (ServerWorld) npcEntity.getWorld();
            BlockState blockState = world.getBlockState(pos);
            
            if (blockState.isAir()) {
                LogUtil.error("MiningActionHandler: Block at " + pos + " is air");
                return false;
            }
            
            // Use /setblock command to break the block (replace with air)
            String command = String.format("setblock %d %d %d air", pos.getX(), pos.getY(), pos.getZ());
            boolean success = MinecraftCommandUtil.executeCommand(npcEntity, command);
            
            if (success) {
                // Drop block as item (simulate mining)
                Block block = blockState.getBlock();
                Identifier blockId = Registries.BLOCK.getId(block);
                String itemId = blockId.toString();
                
                // Give the mined item to the NPC
                String giveCommand = String.format("give %s %s 1", npcEntity.getName().getString(), itemId);
                MinecraftCommandUtil.executeCommand(npcEntity, giveCommand);
            }
            
            return success;
            
        } catch (Exception e) {
            LogUtil.error("MiningActionHandler: Error breaking block at " + pos, e);
            return false;
        }
    }
    
    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 2) return false;
        
        String actionType = parsed[0].toLowerCase();
        if (!"mine".equals(actionType)) return false;
        
        String operation = parsed[1].toLowerCase();
        
        return switch (operation) {
            case "at" -> parsed.length >= 5; // mine at x y z
            default -> parsed.length >= 2; // mine block_type [count]
        };
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "mine <block_type> [count]",
            "mine at <x> <y> <z>"
        );
    }
}

