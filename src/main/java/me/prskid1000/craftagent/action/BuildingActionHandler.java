package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.context.ActionStateManager;
import me.prskid1000.craftagent.context.ContextProvider;
import me.prskid1000.craftagent.util.LogUtil;
import me.prskid1000.craftagent.util.MinecraftCommandUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;

/**
 * Handles building/placing actions for NPCs.
 * Supports placing blocks from inventory.
 * 
 * Formats:
 * - "build <block_type> at <x> <y> <z>" - Place block at coordinates
 * - "place <block_type> at <x> <y> <z>" - Place block at coordinates (alias)
 */
public class BuildingActionHandler implements ActionSyntaxProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final ContextProvider contextProvider;
    private final BaseConfig baseConfig;
    
    public BuildingActionHandler(ServerPlayerEntity npcEntity, ContextProvider contextProvider, BaseConfig baseConfig) {
        this.npcEntity = npcEntity;
        this.contextProvider = contextProvider;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 2) {
            LogUtil.error("BuildingActionHandler: Invalid action format: " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        if (!"build".equals(actionType) && !"place".equals(actionType)) {
            return false;
        }
        
        return placeBlock(parsed);
    }
    
    private boolean placeBlock(String[] parsed) {
        // Format: build/place <block_type> at <x> <y> <z>
        if (parsed.length < 6) {
            LogUtil.error("BuildingActionHandler: 'build <block_type> at <x> <y> <z>' requires block type and coordinates");
            return false;
        }
        
        // Find "at" keyword
        int atIndex = -1;
        for (int i = 1; i < parsed.length; i++) {
            if ("at".equals(parsed[i].toLowerCase())) {
                atIndex = i;
                break;
            }
        }
        
        if (atIndex == -1 || atIndex + 3 >= parsed.length) {
            LogUtil.error("BuildingActionHandler: Invalid format. Expected: build <block_type> at <x> <y> <z>");
            return false;
        }
        
        // Extract block type (everything between action and "at")
        StringBuilder blockTypeBuilder = new StringBuilder();
        for (int i = 1; i < atIndex; i++) {
            if (blockTypeBuilder.length() > 0) blockTypeBuilder.append("_");
            blockTypeBuilder.append(parsed[i]);
        }
        String blockType = blockTypeBuilder.toString().toLowerCase();
        
        try {
            int x = Integer.parseInt(parsed[atIndex + 1]);
            int y = Integer.parseInt(parsed[atIndex + 2]);
            int z = Integer.parseInt(parsed[atIndex + 3]);
            
            BlockPos targetPos = new BlockPos(x, y, z);
            
            // Check if NPC has the block in inventory
            if (!hasBlockInInventory(blockType)) {
                LogUtil.error("BuildingActionHandler: NPC doesn't have block in inventory: " + blockType);
                return false;
            }
            
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            var actionData = new java.util.HashMap<String, Object>();
            actionData.put("blockType", blockType);
            actionData.put("position", targetPos);
            actionState.setAction(ActionStateManager.ActionType.BUILDING, actionData);
            
            // Place the block
            boolean success = placeBlockAt(targetPos, blockType);
            
            // Set to idle after building
            if (success) {
                actionState.setIdle();
            }
            
            return success;
            
        } catch (NumberFormatException e) {
            LogUtil.error("BuildingActionHandler: Invalid coordinates");
            return false;
        }
    }
    
    private boolean hasBlockInInventory(String blockType) {
        var inventory = npcEntity.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                String itemName = itemId.getPath().toLowerCase();
                if (itemName.equals(blockType) || itemName.contains(blockType)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean placeBlockAt(BlockPos pos, String blockType) {
        try {
            ServerWorld world = (ServerWorld) npcEntity.getWorld();
            
            // Check if position is air or replaceable
            if (!world.getBlockState(pos).isAir() && !world.getBlockState(pos).isReplaceable()) {
                LogUtil.error("BuildingActionHandler: Position " + pos + " is not empty");
                return false;
            }
            
            // Use /setblock command to place the block
            // Convert block_type to minecraft:block_type format
            String blockId = blockType.contains(":") ? blockType : "minecraft:" + blockType;
            String command = String.format("setblock %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), blockId);
            boolean success = MinecraftCommandUtil.executeCommand(npcEntity, command);
            
            if (success) {
                // Remove one block from inventory
                removeBlockFromInventory(blockType);
            }
            
            return success;
            
        } catch (Exception e) {
            LogUtil.error("BuildingActionHandler: Error placing block at " + pos, e);
            return false;
        }
    }
    
    private void removeBlockFromInventory(String blockType) {
        var inventory = npcEntity.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                String itemName = itemId.getPath().toLowerCase();
                if (itemName.equals(blockType) || itemName.contains(blockType)) {
                    stack.decrement(1);
                    if (stack.isEmpty()) {
                        inventory.setStack(i, ItemStack.EMPTY);
                    }
                    return;
                }
            }
        }
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 6) return false;
        
        String actionType = parsed[0].toLowerCase();
        if (!"build".equals(actionType) && !"place".equals(actionType)) return false;
        
        // Check for "at" keyword
        boolean hasAt = false;
        for (int i = 1; i < parsed.length; i++) {
            if ("at".equals(parsed[i].toLowerCase())) {
                hasAt = true;
                // Check if there are 3 more arguments after "at"
                if (i + 3 < parsed.length) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "build <block_type> at <x> <y> <z>",
            "place <block_type> at <x> <y> <z>"
        );
    }
}

