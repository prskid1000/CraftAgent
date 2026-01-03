package me.prskid1000.craftagent.util.command;

import java.util.*;

/**
 * Parser for survival commands using generic utilities.
 * Supports: heal, feed, time, weather commands.
 */
public class SurvivalCommandParser implements CommandParser {
    
    private static final Map<String, String> SIMPLE_COMMANDS = Map.ofEntries(
        Map.entry("heal", "effect give @s minecraft:instant_health 1 1"),
        Map.entry("regenerate", "effect give @s minecraft:regeneration 30 1"),
        Map.entry("feed", "effect give @s minecraft:saturation 1 10")
    );
    
    private static final Map<String, String> TIME_COMMANDS = Map.ofEntries(
        Map.entry("day", "time set day"),
        Map.entry("night", "time set night"),
        Map.entry("noon", "time set noon"),
        Map.entry("midnight", "time set midnight")
    );
    
    @Override
    public boolean canParse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return false;
        String base = parts.get(0).toLowerCase();
        return SIMPLE_COMMANDS.containsKey(base) || base.equals("set") || base.equals("clear");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return null;
        
        String baseCommand = parts.get(0).toLowerCase();
        
        // Simple commands
        String simpleCmd = SIMPLE_COMMANDS.get(baseCommand);
        if (simpleCmd != null) {
            return simpleCmd;
        }
        
        // "set" command with time parameter
        if (baseCommand.equals("set") && parts.size() > 1) {
            String time = parts.get(1).toLowerCase();
            return TIME_COMMANDS.get(time);
        }
        
        // "clear weather" command
        if (baseCommand.equals("clear") && parts.size() > 1 && parts.get(1).equalsIgnoreCase("weather")) {
            return "weather clear";
        }
        
        return null;
    }
    
    @Override
    public String getCategory() {
        return "Survival";
    }
}

