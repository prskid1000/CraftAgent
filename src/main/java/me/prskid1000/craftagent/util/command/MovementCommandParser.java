package me.prskid1000.craftagent.util.command;

import java.util.*;

/**
 * Parser for movement commands using generic parameter extraction.
 * Supports: walk/move [direction] [steps] or walk/move [steps] [direction]
 */
public class MovementCommandParser implements CommandParser {
    
    private static final Map<String, String> DIRECTION_MAP = Map.ofEntries(
        Map.entry("forward", "~%d ~ ~"),
        Map.entry("backward", "~-%d ~ ~"),
        Map.entry("back", "~-%d ~ ~"),
        Map.entry("left", "~ ~ ~-%d"),
        Map.entry("right", "~ ~ ~%d"),
        Map.entry("up", "~ ~%d ~"),
        Map.entry("down", "~ ~-%d ~")
    );
    
    @Override
    public boolean canParse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return false;
        String base = parts.get(0).toLowerCase();
        return base.equals("walk") || base.equals("move");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.size() < 2) return null;
        
        // Extract direction and steps using generic utilities
        Integer steps = ParameterParser.findFirstNumber(parts, 1);
        String direction = ParameterParser.findFirstNonNumber(parts, 1);
        
        if (direction == null) return null;
        
        // If we found a number first, direction should be after it
        if (steps != null && parts.size() > 2) {
            String possibleDirection = parts.get(2).toLowerCase();
            if (DIRECTION_MAP.containsKey(possibleDirection)) {
                direction = possibleDirection;
            }
        }
        
        // If no number found, default to 1 step
        if (steps == null) {
            steps = 1;
        }
        
        direction = direction.toLowerCase();
        String template = DIRECTION_MAP.get(direction);
        if (template == null) return null;
        
        return String.format("tp @s " + template, steps);
    }
    
    @Override
    public String getCategory() {
        return "Movement";
    }
}

