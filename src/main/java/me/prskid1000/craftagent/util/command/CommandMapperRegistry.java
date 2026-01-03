package me.prskid1000.craftagent.util.command;

import java.util.*;

/**
 * Registry that manages all command parsers and routes commands to the appropriate parser.
 */
public class CommandMapperRegistry {
    
    private final List<CommandParser> parsers;
    private final Map<String, String> exactMatches; // For simple commands without parameters
    
    public CommandMapperRegistry() {
        this.parsers = new ArrayList<>();
        this.exactMatches = new HashMap<>();
        
        // Register all parsers in order of specificity (most specific first)
        registerParser(new ToolCommandParser());
        registerParser(new MovementCommandParser());
        registerParser(new ItemCommandParser());
        registerParser(new MiningCommandParser());
        registerParser(new CraftingCommandParser());
        registerParser(new CombatCommandParser());
        registerParser(new BuildingCommandParser());
        registerParser(new SurvivalCommandParser());
        
        // Register exact matches for simple commands
        registerExactMatches();
    }
    
    /**
     * Registers a command parser.
     */
    public void registerParser(CommandParser parser) {
        parsers.add(parser);
    }
    
    /**
     * Maps a custom command to actual Minecraft command or tool action.
     * Tries parsers first, then exact matches, then returns as-is.
     */
    public String mapCommand(String customCommand) {
        if (customCommand == null || customCommand.trim().isEmpty()) {
            return null;
        }
        
        String normalized = customCommand.toLowerCase().trim();
        
        // Try exact matches first (for simple commands)
        String exactMatch = exactMatches.get(normalized);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // Try each parser in order
        for (CommandParser parser : parsers) {
            if (parser.canParse(normalized)) {
                String result = parser.parse(normalized);
                if (result != null) {
                    return result;
                }
            }
        }
        
        // If not found, return as-is (might be a direct Minecraft command)
        return customCommand;
    }
    
    /**
     * Gets all parsers for a given category.
     */
    public List<CommandParser> getParsersByCategory(String category) {
        List<CommandParser> result = new ArrayList<>();
        for (CommandParser parser : parsers) {
            if (parser.getCategory().equals(category)) {
                result.add(parser);
            }
        }
        return result;
    }
    
    /**
     * Gets all registered parsers.
     */
    public List<CommandParser> getAllParsers() {
        return new ArrayList<>(parsers);
    }
    
    /**
     * Registers exact match commands (simple commands without parameters).
     */
    private void registerExactMatches() {
        // Simple survival commands
        exactMatches.put("heal", "effect give @s minecraft:instant_health 1 1");
        exactMatches.put("regenerate", "effect give @s minecraft:regeneration 30 1");
        exactMatches.put("feed", "effect give @s minecraft:saturation 1 10");
        exactMatches.put("get food", "give @s minecraft:cooked_beef 16");
        exactMatches.put("set day", "time set day");
        exactMatches.put("set night", "time set night");
        exactMatches.put("clear weather", "weather clear");
        
        // Simple tool commands (without parameters - will be handled by parser if parameters present)
        exactMatches.put("save location", "manageMemory:add:location");
        exactMatches.put("remember location", "manageMemory:add:location");
        exactMatches.put("forget location", "manageMemory:remove:location");
        exactMatches.put("add contact", "manageMemory:add:contact");
        exactMatches.put("update contact", "manageMemory:update:contact");
        exactMatches.put("remove contact", "manageMemory:remove:contact");
        exactMatches.put("send mail", "sendMessage");
        exactMatches.put("send message", "sendMessage");
        exactMatches.put("add book page", "manageBook:add");
        exactMatches.put("update book page", "manageBook:update");
        exactMatches.put("remove book page", "manageBook:remove");
    }
}

