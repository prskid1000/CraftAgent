package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.context.ActionStateManager;
import me.prskid1000.craftagent.context.ContextProvider;
import me.prskid1000.craftagent.util.LogUtil;
import me.prskid1000.craftagent.util.MinecraftCommandUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;

/**
 * Handles farming actions for NPCs.
 * Supports planting seeds and harvesting crops.
 * 
 * Formats:
 * - "farm plant <crop_type> at <x> <y> <z>" - Plant crop at coordinates
 * - "farm harvest at <x> <y> <z>" - Harvest crop at coordinates
 * - "farm harvest" - Harvest nearby mature crops
 */
public class FarmingActionHandler implements ActionSyntaxProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final ContextProvider contextProvider;
    private final BaseConfig baseConfig;
    
    public FarmingActionHandler(ServerPlayerEntity npcEntity, ContextProvider contextProvider, BaseConfig baseConfig) {
        this.npcEntity = npcEntity;
        this.contextProvider = contextProvider;
        this.baseConfig = baseConfig;
    }
    
    public boolean handleAction(String originalAction, String[] parsed) {
        if (parsed == null || parsed.length < 2) {
            LogUtil.error("FarmingActionHandler: Invalid action format: " + originalAction);
            return false;
        }
        
        String actionType = parsed[0].toLowerCase();
        if (!"farm".equals(actionType)) {
            return false;
        }
        
        String operation = parsed[1].toLowerCase();
        
        return switch (operation) {
            case "plant" -> plantCrop(parsed);
            case "harvest" -> harvestCrop(parsed);
            default -> {
                LogUtil.error("FarmingActionHandler: Unknown farm operation: " + operation);
                yield false;
            }
        };
    }
    
    private boolean plantCrop(String[] parsed) {
        // Format: farm plant <crop_type> at <x> <y> <z>
        if (parsed.length < 6) {
            LogUtil.error("FarmingActionHandler: 'farm plant <crop_type> at <x> <y> <z>' requires crop type and coordinates");
            return false;
        }
        
        // Find "at" keyword
        int atIndex = -1;
        for (int i = 2; i < parsed.length; i++) {
            if ("at".equals(parsed[i].toLowerCase())) {
                atIndex = i;
                break;
            }
        }
        
        if (atIndex == -1 || atIndex + 3 >= parsed.length) {
            LogUtil.error("FarmingActionHandler: Invalid format. Expected: farm plant <crop_type> at <x> <y> <z>");
            return false;
        }
        
        // Extract crop type
        StringBuilder cropTypeBuilder = new StringBuilder();
        for (int i = 2; i < atIndex; i++) {
            if (cropTypeBuilder.length() > 0) cropTypeBuilder.append("_");
            cropTypeBuilder.append(parsed[i]);
        }
        String cropType = cropTypeBuilder.toString().toLowerCase();
        
        try {
            int x = Integer.parseInt(parsed[atIndex + 1]);
            int y = Integer.parseInt(parsed[atIndex + 2]);
            int z = Integer.parseInt(parsed[atIndex + 3]);
            
            BlockPos targetPos = new BlockPos(x, y, z);
            
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            var actionData = new java.util.HashMap<String, Object>();
            actionData.put("operation", "plant");
            actionData.put("cropType", cropType);
            actionData.put("position", targetPos);
            actionState.setAction(ActionStateManager.ActionType.FARMING, actionData);
            
            boolean success = plantCropAt(targetPos, cropType);
            
            if (success) {
                actionState.setIdle();
            }
            
            return success;
            
        } catch (NumberFormatException e) {
            LogUtil.error("FarmingActionHandler: Invalid coordinates");
            return false;
        }
    }
    
    private boolean plantCropAt(BlockPos pos, String cropType) {
        try {
            ServerWorld world = (ServerWorld) npcEntity.getWorld();
            BlockState currentState = world.getBlockState(pos);
            
            // Check if position is air or farmland
            if (!currentState.isAir() && !(currentState.getBlock() instanceof FarmlandBlock)) {
                LogUtil.error("FarmingActionHandler: Position " + pos + " is not suitable for planting");
                return false;
            }
            
            // Check if NPC has seeds for this crop type
            String seedType = getSeedTypeForCrop(cropType);
            if (seedType != null && !hasSeedInInventory(seedType)) {
                LogUtil.error("FarmingActionHandler: NPC doesn't have seeds for crop: " + cropType);
                return false;
            }
            
            // Use /setblock to place crop
            String blockId = cropType.contains(":") ? cropType : "minecraft:" + cropType;
            String command = String.format("setblock %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), blockId);
            boolean success = MinecraftCommandUtil.executeCommand(npcEntity, command);
            
            // Consume seed from inventory if planting was successful
            if (success && seedType != null) {
                removeSeedFromInventory(seedType);
            }
            
            return success;
            
        } catch (Exception e) {
            LogUtil.error("FarmingActionHandler: Error planting crop at " + pos, e);
            return false;
        }
    }
    
    private String getSeedTypeForCrop(String cropType) {
        // Map crop types to their seed items
        return switch (cropType.toLowerCase()) {
            case "wheat" -> "wheat_seeds";
            case "carrots" -> "carrot";
            case "potatoes" -> "potato";
            case "beetroots" -> "beetroot_seeds";
            case "melon_stem", "attached_melon_stem" -> "melon_seeds";
            case "pumpkin_stem", "attached_pumpkin_stem" -> "pumpkin_seeds";
            default -> null; // Some crops don't require seeds (like nether wart)
        };
    }
    
    private boolean hasSeedInInventory(String seedType) {
        var inventory = npcEntity.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                String itemName = itemId.getPath().toLowerCase();
                if (itemName.equals(seedType) || itemName.contains(seedType)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void removeSeedFromInventory(String seedType) {
        var inventory = npcEntity.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                String itemName = itemId.getPath().toLowerCase();
                if (itemName.equals(seedType) || itemName.contains(seedType)) {
                    stack.decrement(1);
                    if (stack.isEmpty()) {
                        inventory.setStack(i, ItemStack.EMPTY);
                    }
                    return;
                }
            }
        }
    }
    
    private boolean harvestCrop(String[] parsed) {
        // Format: farm harvest at <x> <y> <z> OR farm harvest
        if (parsed.length >= 3 && "at".equals(parsed[2].toLowerCase())) {
            // Harvest at specific coordinates
            if (parsed.length < 6) {
                LogUtil.error("FarmingActionHandler: 'farm harvest at <x> <y> <z>' requires coordinates");
                return false;
            }
            
            try {
                int x = Integer.parseInt(parsed[3]);
                int y = Integer.parseInt(parsed[4]);
                int z = Integer.parseInt(parsed[5]);
                
                BlockPos targetPos = new BlockPos(x, y, z);
                
                // Set action state
                var actionState = contextProvider.getActionStateManager();
                var actionData = new java.util.HashMap<String, Object>();
                actionData.put("operation", "harvest");
                actionData.put("position", targetPos);
                actionState.setAction(ActionStateManager.ActionType.FARMING, actionData);
                
                boolean success = harvestCropAt(targetPos);
                
                if (success) {
                    actionState.setIdle();
                }
                
                return success;
                
            } catch (NumberFormatException e) {
                LogUtil.error("FarmingActionHandler: Invalid coordinates");
                return false;
            }
        } else {
            // Harvest nearby mature crops
            // Set action state
            var actionState = contextProvider.getActionStateManager();
            var actionData = new java.util.HashMap<String, Object>();
            actionData.put("operation", "harvest");
            actionState.setAction(ActionStateManager.ActionType.FARMING, actionData);
            
            boolean success = harvestNearbyCrops();
            
            if (success) {
                actionState.setIdle();
            }
            
            return success;
        }
    }
    
    private boolean harvestCropAt(BlockPos pos) {
        try {
            ServerWorld world = (ServerWorld) npcEntity.getWorld();
            BlockState blockState = world.getBlockState(pos);
            
            if (!(blockState.getBlock() instanceof CropBlock)) {
                LogUtil.error("FarmingActionHandler: Block at " + pos + " is not a crop");
                return false;
            }
            
            CropBlock crop = (CropBlock) blockState.getBlock();
            
            // Check if crop is mature
            if (!crop.isMature(blockState)) {
                LogUtil.error("FarmingActionHandler: Crop at " + pos + " is not mature");
                return false;
            }
            
            // Break the block to harvest
            String command = String.format("setblock %d %d %d air", pos.getX(), pos.getY(), pos.getZ());
            boolean success = MinecraftCommandUtil.executeCommand(npcEntity, command);
            
            if (success) {
                // Give crop drops
                Identifier cropId = Registries.BLOCK.getId(crop);
                String itemId = cropId.toString();
                String giveCommand = String.format("give %s %s 1", npcEntity.getName().getString(), itemId);
                MinecraftCommandUtil.executeCommand(npcEntity, giveCommand);
            }
            
            return success;
            
        } catch (Exception e) {
            LogUtil.error("FarmingActionHandler: Error harvesting crop at " + pos, e);
            return false;
        }
    }
    
    private boolean harvestNearbyCrops() {
        // Find nearby mature crops and harvest them
        var nearbyBlocks = contextProvider.getChunkManager().getNearbyBlocks();
        boolean success = false;
        
        for (var blockData : nearbyBlocks) {
            BlockPos pos = blockData.position();
            if (harvestCropAt(pos)) {
                success = true;
                break; // Harvest one at a time
            }
        }
        
        return success;
    }
    
    public boolean isValidAction(String action, String[] parsed) {
        if (parsed == null || parsed.length < 2) return false;
        
        String actionType = parsed[0].toLowerCase();
        if (!"farm".equals(actionType)) return false;
        
        String operation = parsed[1].toLowerCase();
        
        return switch (operation) {
            case "plant" -> parsed.length >= 6; // farm plant crop_type at x y z
            case "harvest" -> parsed.length >= 2; // farm harvest [at x y z]
            default -> false;
        };
    }
    
    @Override
    public List<String> getActionSyntax() {
        return getStaticActionSyntax();
    }
    
    public static List<String> getStaticActionSyntax() {
        return Arrays.asList(
            "farm plant <crop_type> at <x> <y> <z>",
            "farm harvest at <x> <y> <z>",
            "farm harvest"
        );
    }
}

