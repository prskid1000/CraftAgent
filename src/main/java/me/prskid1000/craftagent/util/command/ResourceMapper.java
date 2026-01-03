package me.prskid1000.craftagent.util.command;

import java.util.Map;
import java.util.HashMap;

/**
 * Centralized resource name mappings (items, blocks, mobs).
 * Makes it easy to add new mappings without modifying parser code.
 */
public class ResourceMapper {
    
    private static final Map<String, String> ITEM_MAP = new HashMap<>();
    private static final Map<String, String> BLOCK_MAP = new HashMap<>();
    private static final Map<String, String> MOB_MAP = new HashMap<>();
    private static final Map<String, String> CRAFT_ITEM_MAP = new HashMap<>();
    
    static {
        // Item mappings
        ITEM_MAP.put("wood", "minecraft:oak_log");
        ITEM_MAP.put("log", "minecraft:oak_log");
        ITEM_MAP.put("oak_log", "minecraft:oak_log");
        ITEM_MAP.put("stone", "minecraft:stone");
        ITEM_MAP.put("cobblestone", "minecraft:cobblestone");
        ITEM_MAP.put("cobble", "minecraft:cobblestone");
        ITEM_MAP.put("iron", "minecraft:iron_ore");
        ITEM_MAP.put("iron_ore", "minecraft:iron_ore");
        ITEM_MAP.put("coal", "minecraft:coal");
        ITEM_MAP.put("diamond", "minecraft:diamond");
        ITEM_MAP.put("gold", "minecraft:gold_ore");
        ITEM_MAP.put("gold_ore", "minecraft:gold_ore");
        ITEM_MAP.put("wheat", "minecraft:wheat");
        ITEM_MAP.put("carrot", "minecraft:carrot");
        ITEM_MAP.put("potato", "minecraft:potato");
        ITEM_MAP.put("beef", "minecraft:cooked_beef");
        ITEM_MAP.put("cooked_beef", "minecraft:cooked_beef");
        ITEM_MAP.put("bread", "minecraft:bread");
        ITEM_MAP.put("apple", "minecraft:apple");
        ITEM_MAP.put("fish", "minecraft:cod");
        ITEM_MAP.put("cod", "minecraft:cod");
        ITEM_MAP.put("salmon", "minecraft:salmon");
        ITEM_MAP.put("food", "minecraft:cooked_beef");
        
        // Block mappings
        BLOCK_MAP.put("wood", "minecraft:oak_planks");
        BLOCK_MAP.put("planks", "minecraft:oak_planks");
        BLOCK_MAP.put("oak_planks", "minecraft:oak_planks");
        BLOCK_MAP.put("stone", "minecraft:stone");
        BLOCK_MAP.put("cobblestone", "minecraft:cobblestone");
        BLOCK_MAP.put("cobble", "minecraft:cobblestone");
        BLOCK_MAP.put("dirt", "minecraft:dirt");
        BLOCK_MAP.put("grass", "minecraft:grass_block");
        BLOCK_MAP.put("sand", "minecraft:sand");
        BLOCK_MAP.put("gravel", "minecraft:gravel");
        BLOCK_MAP.put("glass", "minecraft:glass");
        BLOCK_MAP.put("brick", "minecraft:bricks");
        BLOCK_MAP.put("bricks", "minecraft:bricks");
        
        // Mob mappings
        MOB_MAP.put("zombie", "minecraft:zombie");
        MOB_MAP.put("skeleton", "minecraft:skeleton");
        MOB_MAP.put("creeper", "minecraft:creeper");
        MOB_MAP.put("spider", "minecraft:spider");
        MOB_MAP.put("cow", "minecraft:cow");
        MOB_MAP.put("pig", "minecraft:pig");
        MOB_MAP.put("chicken", "minecraft:chicken");
        MOB_MAP.put("sheep", "minecraft:sheep");
        MOB_MAP.put("villager", "minecraft:villager");
        
        // Craft item mappings
        CRAFT_ITEM_MAP.put("pickaxe", "minecraft:wooden_pickaxe");
        CRAFT_ITEM_MAP.put("wooden_pickaxe", "minecraft:wooden_pickaxe");
        CRAFT_ITEM_MAP.put("iron_pickaxe", "minecraft:iron_pickaxe");
        CRAFT_ITEM_MAP.put("diamond_pickaxe", "minecraft:diamond_pickaxe");
        CRAFT_ITEM_MAP.put("sword", "minecraft:wooden_sword");
        CRAFT_ITEM_MAP.put("wooden_sword", "minecraft:wooden_sword");
        CRAFT_ITEM_MAP.put("iron_sword", "minecraft:iron_sword");
        CRAFT_ITEM_MAP.put("diamond_sword", "minecraft:diamond_sword");
        CRAFT_ITEM_MAP.put("axe", "minecraft:wooden_axe");
        CRAFT_ITEM_MAP.put("wooden_axe", "minecraft:wooden_axe");
        CRAFT_ITEM_MAP.put("iron_axe", "minecraft:iron_axe");
        CRAFT_ITEM_MAP.put("shovel", "minecraft:wooden_shovel");
        CRAFT_ITEM_MAP.put("wooden_shovel", "minecraft:wooden_shovel");
        CRAFT_ITEM_MAP.put("planks", "minecraft:oak_planks");
        CRAFT_ITEM_MAP.put("oak_planks", "minecraft:oak_planks");
        CRAFT_ITEM_MAP.put("sticks", "minecraft:stick");
        CRAFT_ITEM_MAP.put("stick", "minecraft:stick");
        CRAFT_ITEM_MAP.put("bread", "minecraft:bread");
        CRAFT_ITEM_MAP.put("cooked_beef", "minecraft:cooked_beef");
        CRAFT_ITEM_MAP.put("torch", "minecraft:torch");
        CRAFT_ITEM_MAP.put("torches", "minecraft:torch");
        CRAFT_ITEM_MAP.put("chest", "minecraft:chest");
        CRAFT_ITEM_MAP.put("furnace", "minecraft:furnace");
    }
    
    /**
     * Maps an item name to Minecraft item ID.
     * Returns null if not found, allowing caller to handle fallback.
     */
    public static String mapItem(String itemName) {
        if (itemName == null) return null;
        String lower = itemName.toLowerCase();
        String mapped = ITEM_MAP.get(lower);
        if (mapped != null) return mapped;
        
        // If already has minecraft: prefix, return as-is
        if (itemName.startsWith("minecraft:")) {
            return itemName;
        }
        
        // Try with minecraft: prefix
        return "minecraft:" + itemName.toLowerCase();
    }
    
    /**
     * Maps a block name to Minecraft block ID.
     */
    public static String mapBlock(String blockName) {
        if (blockName == null) return null;
        String lower = blockName.toLowerCase();
        String mapped = BLOCK_MAP.get(lower);
        if (mapped != null) return mapped;
        
        if (blockName.startsWith("minecraft:")) {
            return blockName;
        }
        
        return "minecraft:" + blockName.toLowerCase();
    }
    
    /**
     * Maps a mob name to Minecraft entity ID.
     */
    public static String mapMob(String mobName) {
        if (mobName == null) return null;
        String lower = mobName.toLowerCase();
        String mapped = MOB_MAP.get(lower);
        if (mapped != null) return mapped;
        
        if (mobName.startsWith("minecraft:")) {
            return mobName;
        }
        
        return "minecraft:" + mobName.toLowerCase();
    }
    
    /**
     * Maps a craft item name to Minecraft item ID.
     */
    public static String mapCraftItem(String itemName) {
        if (itemName == null) return null;
        String lower = itemName.toLowerCase();
        String mapped = CRAFT_ITEM_MAP.get(lower);
        if (mapped != null) return mapped;
        
        if (itemName.startsWith("minecraft:")) {
            return itemName;
        }
        
        return null; // Craft items are more specific, return null if not found
    }
}

