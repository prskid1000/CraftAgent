package me.prskid1000.craftagent.util.command;

import java.util.*;

/**
 * Parser for building/placing commands using generic utilities.
 * Supports: place <block> [direction]
 */
public class BuildingCommandParser implements CommandParser {
    
    private static final Map<String, String> DIRECTION_MAP = Map.ofEntries(
        Map.entry("front", "~1 ~ ~"),
        Map.entry("forward", "~1 ~ ~"),
        Map.entry("below", "~ ~-1 ~"),
        Map.entry("down", "~ ~-1 ~"),
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
        return !parts.isEmpty() && parts.get(0).equalsIgnoreCase("place");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.size() < 2) return null;
        
        // Use generic utilities to extract block and direction
        String lastPart = parts.size() > 2 ? parts.get(parts.size() - 1).toLowerCase() : null;
        boolean lastPartIsDirection = lastPart != null && DIRECTION_MAP.containsKey(lastPart);
        String direction = lastPartIsDirection ? lastPart : "below";
        
        // Reconstruct block name (everything except first and last if last is direction)
        String block = ParameterParser.reconstructName(parts, 1);
        
        // Remove direction from block name if it was included
        if (lastPartIsDirection && block != null && block.endsWith(" " + lastPart)) {
            block = block.substring(0, block.length() - lastPart.length() - 1);
        }
        
        if (block == null || block.isEmpty()) return null;
        
        String minecraftBlock = ResourceMapper.mapBlock(block);
        if (minecraftBlock == null) return null;
        
        String position = DIRECTION_MAP.getOrDefault(direction, DEFAULT_POSITION);
        return String.format("setblock %s %s", position, minecraftBlock);
    }
    
    @Override
    public String getCategory() {
        return "Building";
    }
}

