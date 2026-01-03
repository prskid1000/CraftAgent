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
     * Shows parameter examples.
     */
    public static String getFormattedCommandList() {
        Map<String, List<String>> commands = getAllCustomCommands();
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String, List<String>> entry : commands.entrySet()) {
            sb.append(entry.getKey()).append(":\n");
            for (String cmd : entry.getValue()) {
                // Show example mappings for parameterized commands
                if (cmd.contains("[") || cmd.contains("<")) {
                    sb.append("  - ").append(cmd).append("\n");
                    // Add example based on command type
                    if (cmd.startsWith("walk") || cmd.startsWith("move")) {
                        sb.append("    Example: 'walk forward 5' → 'tp @s ~5 ~ ~'\n");
                    } else if (cmd.startsWith("get")) {
                        sb.append("    Example: 'get wood 64' → 'give @s minecraft:oak_log 64'\n");
                    } else if (cmd.startsWith("mine")) {
                        sb.append("    Example: 'mine front' → 'setblock ~1 ~ ~ minecraft:air'\n");
                    } else if (cmd.startsWith("craft")) {
                        sb.append("    Example: 'craft pickaxe' → 'give @s minecraft:wooden_pickaxe'\n");
                    } else if (cmd.startsWith("place")) {
                        sb.append("    Example: 'place wood front' → 'setblock ~1 ~ ~ minecraft:oak_planks'\n");
                    } else if (cmd.startsWith("kill")) {
                        sb.append("    Example: 'kill zombie' → 'kill @e[type=minecraft:zombie,limit=1,sort=nearest]'\n");
                    } else if (cmd.startsWith("spawn")) {
                        sb.append("    Example: 'spawn cow' → 'summon minecraft:cow ~ ~ ~'\n");
                    } else if (cmd.startsWith("save") || cmd.startsWith("remember")) {
                        sb.append("    Example: 'save location MyBase description:My home' → 'manageMemory:add:location|name:MyBase|description:My home'\n");
                    } else if (cmd.startsWith("send")) {
                        sb.append("    Example: 'send mail Bob Hello content:How are you?' → 'sendMessage|recipient:Bob|subject:Hello|content:How are you?'\n");
                    } else if (cmd.startsWith("add book") || cmd.startsWith("update book")) {
                        sb.append("    Example: 'add book page Rules content:No griefing' → 'manageBook:add|title:Rules|content:No griefing'\n");
                    }
                } else {
                    String mapping = getMapping(cmd);
                    sb.append("  - ").append(cmd).append(" → ").append(mapping).append("\n");
                }
            }
            sb.append("\n");
        }
        
        return sb.toString();
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
}
