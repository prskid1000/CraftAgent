package me.prskid1000.craftagent.util;

import me.prskid1000.craftagent.util.command.CommandMapperRegistry;
import me.prskid1000.craftagent.util.command.CommandParser;

import java.util.*;

/**
 * Facade for command mapping system.
 * Delegates to CommandMapperRegistry which uses specialized parsers for different command types.
 * This provides a clean API while maintaining extensibility through the parser system.
 */
public class CommandMapper {
    
    private static final CommandMapperRegistry registry = new CommandMapperRegistry();
    
    /**
     * Maps a custom command to actual Minecraft command or tool action.
     * Delegates to the CommandMapperRegistry which uses specialized parsers.
     * Supports parameters like "walk forward 5", "get wood 64", "mine stone", etc.
     * Returns the mapped command string, or null if not found.
     */
    public static String mapCommand(String customCommand) {
        return registry.mapCommand(customCommand);
    }
    
    /**
     * Unescapes parameter values from the pipe-delimited format.
     */
    public static String unescapeParam(String value) {
        if (value == null) return "";
        return value.replace("\\|", "|").replace("\\:", ":");
    }
    
    /**
     * Parses a tool action string with parameters.
     * Format: "tool:action|key1:value1|key2:value2"
     * Returns a map of parameters.
     */
    public static Map<String, String> parseToolAction(String toolAction) {
        Map<String, String> params = new HashMap<>();
        if (toolAction == null || !toolAction.contains("|")) {
            return params;
        }
        
        String[] parts = toolAction.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int colonIndex = part.indexOf(":");
            if (colonIndex > 0) {
                String key = part.substring(0, colonIndex);
                String value = unescapeParam(part.substring(colonIndex + 1));
                params.put(key, value);
            }
        }
        
        return params;
    }
    
    /**
     * Gets all available custom commands for display in UI and prompts.
     * Shows examples with parameters.
     */
    public static Map<String, List<String>> getAllCustomCommands() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        
        List<String> movement = Arrays.asList(
            "walk forward [steps]", "walk backward [steps]", "walk left [steps]", "walk right [steps]",
            "walk up [steps]", "walk down [steps]",
            "move forward [steps]", "move back [steps]", "move left [steps]", "move right [steps]"
        );
        
        List<String> mining = Arrays.asList(
            "mine [block/direction]", "mine front", "mine above", "mine below",
            "get <item> [amount]", "get wood 64", "get stone 32", "get iron 16", "get diamond 8"
        );
        
        List<String> crafting = Arrays.asList(
            "craft <item> [amount]", "craft pickaxe", "craft iron pickaxe", "craft sword",
            "craft axe", "craft shovel", "craft planks 64", "craft sticks 32",
            "craft bread 16", "craft cooked beef 32"
        );
        
        List<String> combat = Arrays.asList(
            "kill [mob]", "kill nearest mob", "kill zombie", "kill creeper", "kill skeleton", "clear mobs",
            "spawn <mob>", "spawn cow", "spawn pig", "spawn chicken"
        );
        
        List<String> survival = Arrays.asList(
            "heal", "regenerate", "feed", "get food [amount]", "set day", "set night", "clear weather"
        );
        
        List<String> building = Arrays.asList(
            "place <block> [direction]", "place wood", "place stone", "place wood front", "place stone below"
        );
        
        List<String> farming = Arrays.asList(
            "get wheat [amount]", "get carrot [amount]", "get potato [amount]",
            "spawn cow", "spawn pig", "spawn chicken"
        );
        
        List<String> fishing = Arrays.asList(
            "get fish [amount]", "get salmon [amount]", "get cod [amount]"
        );
        
        List<String> memory = Arrays.asList(
            "save location <name> [description:...]", "remember location <name> [description:...]",
            "forget location <name>",
            "add contact <name> [relationship:...] [notes:...]", 
            "update contact <name> [relationship:...] [notes:...]",
            "remove contact <name>"
        );
        
        List<String> communication = Arrays.asList(
            "send mail <recipient> <subject> [content:...]", 
            "send message <recipient> <subject> [content:...]"
        );
        
        List<String> book = Arrays.asList(
            "add book page <title> [content:...]", 
            "update book page <title> [content:...]",
            "remove book page <title>"
        );
        
        result.put("Movement", movement);
        result.put("Mining", mining);
        result.put("Crafting", crafting);
        result.put("Combat", combat);
        result.put("Survival", survival);
        result.put("Building", building);
        result.put("Farming", farming);
        result.put("Fishing", fishing);
        result.put("Memory", memory);
        result.put("Communication", communication);
        result.put("Book", book);
        
        return result;
    }
    
    /**
     * Gets the mapping for a custom command (what it maps to).
     * Uses the registry to get the actual mapping.
     */
    public static String getMapping(String customCommand) {
        String mapped = mapCommand(customCommand);
        if (mapped == null) {
            return "Unknown command";
        }
        
        if (mapped.contains(":")) {
            return "Tool: " + mapped;
        } else {
            return "Minecraft: " + mapped;
        }
    }
    
    /**
     * Gets formatted list of all custom commands for system prompt.
     * Shows command format with parameters and one simple example per command type.
     */
    public static String getFormattedCommandList() {
        Map<String, List<String>> commands = getAllCustomCommands();
        StringBuilder sb = new StringBuilder();
        
        // Track which command types we've shown examples for
        Set<String> exampleShown = new HashSet<>();
        
        for (Map.Entry<String, List<String>> entry : commands.entrySet()) {
            sb.append(entry.getKey()).append(":\n");
            for (String cmd : entry.getValue()) {
                // Show command format
                sb.append("  - ").append(cmd).append("\n");
                
                // Add one example per command type (only once per type)
                String cmdType = getCommandType(cmd);
                if ((cmd.contains("[") || cmd.contains("<")) && !exampleShown.contains(cmdType)) {
                    String example = getExampleForCommandType(cmdType);
                    if (example != null) {
                        sb.append("    Example: ").append(example).append("\n");
                        exampleShown.add(cmdType);
                    }
                }
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Gets the command type for grouping examples.
     */
    private static String getCommandType(String cmd) {
        if (cmd.startsWith("walk") || cmd.startsWith("move")) return "movement";
        if (cmd.startsWith("get")) return "get";
        if (cmd.startsWith("mine")) return "mine";
        if (cmd.startsWith("craft")) return "craft";
        if (cmd.startsWith("place")) return "place";
        if (cmd.startsWith("kill")) return "kill";
        if (cmd.startsWith("spawn")) return "spawn";
        if (cmd.startsWith("save") || cmd.startsWith("remember")) return "save_location";
        if (cmd.startsWith("send")) return "send";
        if (cmd.startsWith("add book") || cmd.startsWith("update book")) return "book";
        return "other";
    }
    
    /**
     * Gets a simple example for a command type (without showing internal transformation).
     */
    private static String getExampleForCommandType(String cmdType) {
        return switch (cmdType) {
            case "movement" -> "'walk forward 5'";
            case "get" -> "'get wood 64'";
            case "mine" -> "'mine front'";
            case "craft" -> "'craft pickaxe'";
            case "place" -> "'place stone front'";
            case "kill" -> "'kill zombie'";
            case "spawn" -> "'spawn cow'";
            case "save_location" -> "'save location MyBase description:My home'";
            case "send" -> "'send mail Bob Hello content:How are you?'";
            case "book" -> "'add book page Rules content:No griefing'";
            default -> null;
        };
    }
    
    /**
     * Executes a custom command by mapping it and executing the result.
     * Returns true if executed successfully, false otherwise.
     */
    public static boolean executeCustomCommand(String customCommand, 
                                               java.util.function.Function<String, Boolean> minecraftExecutor,
                                               java.util.function.Function<String, Boolean> toolExecutor) {
        String mapped = mapCommand(customCommand);
        if (mapped == null) {
            return false;
        }
        
        // Check if it's a tool action (contains : or |)
        if (mapped.contains(":") || mapped.contains("|")) {
            return toolExecutor.apply(mapped);
        } else {
            // It's a Minecraft command
            return minecraftExecutor.apply(mapped);
        }
    }
    
    /**
     * Gets a map of vanilla Minecraft commands to the custom commands that map to them.
     * Returns a map where:
     * - Key: vanilla command name (e.g., "give", "tp", "effect")
     * - Value: list of custom commands that map to this vanilla command
     */
    public static Map<String, List<String>> getVanillaCommandMappings() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        Map<String, List<String>> customCommands = getAllCustomCommands();
        
        // Iterate through all custom commands and find what they map to
        for (Map.Entry<String, List<String>> category : customCommands.entrySet()) {
            for (String customCmd : category.getValue()) {
                // Try multiple variations to find the mapping
                String mapped = null;
                
                // 1. Try the base command without parameters
                String baseCustomCmd = customCmd.split("\\[|\\<")[0].trim();
                mapped = mapCommand(baseCustomCmd);
                
                // 2. If that doesn't work or returns the same, try with example values
                if (mapped == null || mapped.equals(baseCustomCmd) || mapped.equals(customCmd)) {
                    // Build example command by replacing placeholders
                    String exampleCmd = customCmd
                        .replaceAll("\\[.*?\\]", "")  // Remove [optional] params
                        .replaceAll("<item>", "wood")  // Replace <item> with example
                        .replaceAll("<block>", "stone")
                        .replaceAll("<mob>", "cow")
                        .replaceAll("<name>", "Test")
                        .replaceAll("<recipient>", "Bob")
                        .replaceAll("<subject>", "Hello")
                        .replaceAll("<title>", "Page")
                        .replaceAll("\\[steps\\]", "5")  // Replace [steps] with example
                        .replaceAll("\\[amount\\]", "64")
                        .replaceAll("\\[direction\\]", "front")
                        .replaceAll("\\[block/direction\\]", "front")
                        .trim();
                    mapped = mapCommand(exampleCmd);
                }
                
                // 3. Check if it's a Minecraft command (not a tool action)
                if (mapped != null && !mapped.contains(":") && !mapped.equals(customCmd)) {
                    String vanillaCmd = extractBaseCommand(mapped);
                    if (vanillaCmd != null && !vanillaCmd.isEmpty()) {
                        result.computeIfAbsent(vanillaCmd, k -> new ArrayList<>()).add(customCmd);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Extracts the base Minecraft command name from a full command string.
     * Example: "give @s minecraft:diamond 64" -> "give"
     */
    private static String extractBaseCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }
        String[] parts = command.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }
}
