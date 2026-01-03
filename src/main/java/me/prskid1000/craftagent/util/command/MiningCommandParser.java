package me.prskid1000.craftagent.util.command;

import java.util.*;

/**
 * Parser for mining commands using generic utilities.
 * Supports: mine [direction/block]
 */
public class MiningCommandParser implements CommandParser {
    
    private static final Map<String, String> DIRECTION_MAP = Map.ofEntries(
        Map.entry("block", "~ ~-1 ~"),
        Map.entry("below", "~ ~-1 ~"),
        Map.entry("down", "~ ~-1 ~"),
        Map.entry("front", "~1 ~ ~"),
        Map.entry("forward", "~1 ~ ~"),
        Map.entry("above", "~ ~1 ~"),
        Map.entry("up", "~ ~1 ~"),
        Map.entry("left", "~ ~ ~-1"),
        Map.entry("right", "~ ~ ~1"),
        Map.entry("back", "~-1 ~ ~"),
        Map.entry("backward", "~-1 ~ ~")
    );
    
    private static final String DEFAULT_POSITION = "~ ~-1 ~";
    
    @Override
    public boolean canParse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        return !parts.isEmpty() && parts.get(0).equalsIgnoreCase("mine");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        
        if (parts.size() < 2) {
            return String.format("setblock %s minecraft:air", DEFAULT_POSITION);
        }
        
        String direction = parts.get(1).toLowerCase();
        String position = DIRECTION_MAP.get(direction);
        
        if (position == null) {
            // Try as block type to mine
            String blockType = ResourceMapper.mapBlock(direction);
            if (blockType != null) {
                return String.format("setblock %s %s", DEFAULT_POSITION, blockType);
            }
            position = DEFAULT_POSITION;
        }
        
        return String.format("setblock %s minecraft:air", position);
    }
    
    @Override
    public String getCategory() {
        return "Mining";
    }
}
