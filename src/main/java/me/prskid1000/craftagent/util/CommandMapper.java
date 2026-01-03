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
        
        List<String> utility = Arrays.asList(
            "teleport <x> <y> <z>", "teleport <target>", "tp <x> <y> <z>", "tp <target>",
            "gamemode <survival|creative|adventure|spectator>", "gm <mode>",
            "experience add <amount> [levels]", "xp add <amount> [levels]", "xp set <amount> [levels]",
            "enchant <enchantment> [level]",
            "clear [item] [maxCount]",
            "fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [replace|destroy|keep|hollow|outline]",
            "clone <x1> <y1> <z1> <x2> <y2> <z2> <x> <y> <z> [replace|masked|filtered]",
            "say <message>",
            "tellraw <target> <json>",
            "title <target> <title|subtitle|actionbar|clear|reset> [text]",
            "playsound <sound> <source> [target] [x] [y] [z] [volume] [pitch]",
            "list", "help [command]",
            "locate <structure|biome>",
            "attribute <target> <attribute> get [scale]",
            "attribute <target> <attribute> base get [scale]",
            "attribute <target> <attribute> base set <value>",
            "attribute <target> <attribute> base reset",
            "attribute <target> <attribute> modifier add <id> <value> <operation>",
            "attribute <target> <attribute> modifier remove <id>",
            "damage <target> <amount> [source]",
            "schedule function <name> <time> [append|replace]",
            "scoreboard objectives <add|remove|list> <name> [criteria]",
            "scoreboard players <add|set|remove> <target> <objective> [value]",
            "tag <target> <add|remove|list> [name]",
            "team add <name>", "team remove <name>", "team join <name> [target]", "team leave [target]", "team list",
            "me <action>",
            "msg <target> <message>", "tell <target> <message>", "w <target> <message>",
            "teammsg <message>", "tm <message>",
            "stopsound", "spectate", "recipe", "seed", "version",
            "particle <type> <x> <y> <z> [dx] [dy] [dz] [speed] [count]",
            "loot <spawn|replace|give> <target> <source> <sourceType>",
            "ride <target> <mount|dismount> [vehicle]",
            "spreadplayers <x> <z> <spreadDistance> <maxRange> <targets>",
            "spawnpoint [target] [x] [y] [z]",
            "worldborder get", "worldborder set <size>", "worldborder add <size>", "worldborder center <x> <z>",
            "bossbar add <id> [name]", "bossbar remove <id>", "bossbar set <id> <property> <value>", "bossbar list",
            "advancement <grant|revoke> <target> everything",
            "advancement <grant|revoke> <target> <only|from|through|until> <advancement>",
            "gamerule <rule> [value]",
            "forceload add <chunkX> <chunkZ>", "forceload remove <chunkX> <chunkZ>", "forceload query",
            "trigger <objective> [add|set] [value]",
            "difficulty <peaceful|easy|normal|hard>",
            "function <name>",
            "datapack <enable|disable|list> [name]",
            "data <get|merge|modify|remove> <target> [path] [value]",
            "fillbiome <x1> <y1> <z1> <x2> <y2> <z2> <biome>",
            "ban <player> [reason]", "ban-ip <address> [reason]", "banlist [ips|players]",
            "deop <player>", "op <player>", "kick <player> [reason]",
            "pardon <player>", "pardon-ip <address>",
            "whitelist <add|remove|list|on|off|reload> [player]",
            "defaultgamemode <mode>", "setworldspawn [x] [y] [z]", "publish", "reload",
            "debug <start|stop|function>", "execute <subcommand> ...",
            // Simple commands that map directly (no parameters or simple pass-through)
            "tell <target> <message>",
            // Mod-specific or unknown commands (pass-through - these are exact matches)
            // Note: craftagent is excluded - it's the mod's own command and shouldn't be shown to users/LLM
            "carpet", "counter", "dialog", "distance", "draw", "info", "item", "jfr", 
            "log", "perf", "perimeterinfo", "place", "player", "profile", "random", 
            "return", "rotate", "script", "spawn", "test", "tick", "track", "waypoint"
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
        result.put("Utility", utility);
        
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
     * Shows command format with all parameters (no examples).
     * Excludes craftagent command from display.
     */
    public static String getFormattedCommandList() {
        Map<String, List<String>> commands = getAllCustomCommands();
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String, List<String>> entry : commands.entrySet()) {
            sb.append(entry.getKey()).append(":\n");
            for (String cmd : entry.getValue()) {
                // Skip craftagent command - don't show it to LLM or users
                if (cmd.toLowerCase().trim().equals("craftagent")) {
                    continue;
                }
                
                // Show command format with all parameters
                sb.append("  - ").append(cmd).append("\n");
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
    
    /**
     * Gets a map of vanilla Minecraft commands to the custom commands that map to them.
     * Returns a map where:
     * - Key: vanilla command name (e.g., "give", "tp", "effect")
     * - Value: list of custom commands that map to this vanilla command
     */
    public static Map<String, List<String>> getVanillaCommandMappings() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        Map<String, List<String>> customCommands = getAllCustomCommands();
        
        // First pass: collect all custom command base names for direct name matching
        Set<String> customCommandBaseNames = new HashSet<>();
        for (Map.Entry<String, List<String>> category : customCommands.entrySet()) {
            for (String customCmd : category.getValue()) {
                if (customCmd.toLowerCase().trim().equals("craftagent")) {
                    continue;
                }
                String baseName = customCmd.split("\\[|\\<")[0].trim().toLowerCase();
                if (!baseName.isEmpty()) {
                    customCommandBaseNames.add(baseName);
                }
            }
        }
        
        // Iterate through all custom commands and find what they map to
        for (Map.Entry<String, List<String>> category : customCommands.entrySet()) {
            for (String customCmd : category.getValue()) {
                // Skip craftagent command - don't include it in mappings
                if (customCmd.toLowerCase().trim().equals("craftagent")) {
                    continue;
                }
                
                // Try multiple variations to find the mapping
                String mapped = null;
                
                // 1. Build example command by replacing placeholders with actual values
                // Replace required parameters first, then optional ones
                String exampleCmd = customCmd
                    .replaceAll("<item>", "wood")  // Replace <item> with example
                    .replaceAll("<block>", "stone")
                    .replaceAll("<mob>", "cow")
                    .replaceAll("<name>", "Test")
                    .replaceAll("<recipient>", "Bob")
                    .replaceAll("<subject>", "Hello")
                    .replaceAll("<title>", "Page")
                    // Coordinates
                    .replaceAll("<x>", "100").replaceAll("<y>", "64").replaceAll("<z>", "200")
                    .replaceAll("<x1>", "0").replaceAll("<y1>", "64").replaceAll("<z1>", "0")
                    .replaceAll("<x2>", "10").replaceAll("<y2>", "70").replaceAll("<z2>", "10")
                    // Common parameters
                    .replaceAll("<target>", "@p")
                    .replaceAll("<player>", "PlayerName")
                    .replaceAll("<address>", "192.168.1.1")
                    .replaceAll("<mode>", "creative")
                    .replaceAll("<survival\\|creative\\|adventure\\|spectator>", "creative")
                    .replaceAll("<amount>", "64")
                    .replaceAll("<enchantment>", "minecraft:sharpness")
                    .replaceAll("<level>", "5")
                    .replaceAll("<message>", "Hello")
                    .replaceAll("<sound>", "minecraft:entity.player.levelup")
                    .replaceAll("<source>", "master")
                    .replaceAll("<structure\\|biome>", "village")
                    .replaceAll("<attribute>", "minecraft:generic.max_health")
                    .replaceAll("<value>", "40")
                    .replaceAll("<objective>", "kills")
                    .replaceAll("<rule>", "keepInventory")
                    .replaceAll("<advancement>", "minecraft:story/mine_stone")
                    .replaceAll("<biome>", "minecraft:plains")
                    .replaceAll("<chunkX>", "0").replaceAll("<chunkZ>", "0")
                    .replaceAll("<size>", "1000")
                    .replaceAll("<id>", "mybossbar")
                    .replaceAll("<property>", "name")
                    .replaceAll("<name>", "MyFunction")
                    .replaceAll("<time>", "5s")
                    .replaceAll("<criteria>", "dummy")
                    .replaceAll("<sourceType>", "loot")
                    .replaceAll("<spreadDistance>", "10")
                    .replaceAll("<maxRange>", "50")
                    .replaceAll("<targets>", "@a")
                    .replaceAll("<operation>", "add_value")
                    // Optional parameters
                    .replaceAll("\\[steps\\]", "5")
                    .replaceAll("\\[amount\\]", "64")
                    .replaceAll("\\[direction\\]", "front")
                    .replaceAll("\\[block/direction\\]", "front")
                    .replaceAll("\\[mob\\]", "zombie")
                    .replaceAll("\\[.*?\\]", "")  // Remove any remaining [optional] params
                    .trim();
                
                // Try mapping with example values
                mapped = mapCommand(exampleCmd);
                
                // 2. If that doesn't work or returns unchanged, try the base command without parameters
                if (mapped == null || mapped.equals(exampleCmd) || mapped.equals(customCmd)) {
                    String baseCustomCmd = customCmd.split("\\[|\\<")[0].trim();
                    if (!baseCustomCmd.isEmpty() && !baseCustomCmd.equals(exampleCmd)) {
                        String baseMapped = mapCommand(baseCustomCmd);
                        if (baseMapped != null && !baseMapped.equals(baseCustomCmd) && !baseMapped.equals(customCmd)) {
                            mapped = baseMapped;
                        } else if (baseMapped != null && baseMapped.equals(baseCustomCmd)) {
                            // Command maps to itself (e.g., "list" -> "list"), this is valid
                            mapped = baseMapped;
                        }
                    }
                }
                
                // Special case: For simple commands with no parameters, try mapping the base command directly
                String baseCustomCmd = customCmd.split("\\[|\\<")[0].trim().toLowerCase();
                if (mapped == null && !baseCustomCmd.isEmpty()) {
                    String directMapped = mapCommand(baseCustomCmd);
                    if (directMapped != null) {
                        mapped = directMapped;
                    }
                }
                
                // 3. Check if it's a Minecraft command (not a tool action)
                // Tool actions contain ":" (like "manageMemory:add:location") or "|" (parameter delimiter)
                // Also allow commands that map to themselves (e.g., "list" -> "list")
                if (mapped != null && (!mapped.equals(customCmd) || baseCustomCmd.equals(mapped.toLowerCase()))) {
                    // Check if it's a tool action first
                    String lowerMapped = mapped.toLowerCase();
                    boolean isToolAction = mapped.contains("|") || 
                                         lowerMapped.contains("managememory:") ||
                                         lowerMapped.contains("sendmessage") ||
                                         lowerMapped.contains("managebook:");
                    
                    if (!isToolAction) {
                        String vanillaCmd = extractBaseCommand(mapped);
                        
                        // Also check if the custom command itself is a vanilla command name
                        // (e.g., "list" custom command maps to "list" vanilla command)
                        if (vanillaCmd == null || vanillaCmd.isEmpty()) {
                            // If we can't extract from mapped, try the custom command name itself
                            vanillaCmd = baseCustomCmd;
                        }
                        
                        // Special case: if custom command is the same as vanilla command name,
                        // it's a valid mapping (e.g., "list" -> "list")
                        // Also handle cases where mapped command starts with the vanilla command
                        // OR if mapped equals the base custom command (maps to itself)
                        if (vanillaCmd != null && !vanillaCmd.isEmpty()) {
                            // Check if base custom command matches vanilla command (case-insensitive)
                            // or if mapped command starts with vanilla command
                            // or if mapped equals the base custom command (maps to itself)
                            if (baseCustomCmd.equals(vanillaCmd.toLowerCase()) || 
                                mapped.toLowerCase().startsWith(vanillaCmd.toLowerCase() + " ") ||
                                mapped.toLowerCase().equals(vanillaCmd.toLowerCase()) ||
                                mapped.toLowerCase().equals(baseCustomCmd)) {
                                result.computeIfAbsent(vanillaCmd, k -> new ArrayList<>()).add(customCmd);
                            }
                        }
                    }
                }
            }
        }
        
        // Second pass: For any vanilla command that has a custom command with the same name,
        // mark it as mapped even if the custom command maps to a different vanilla command
        // This handles cases like: custom "tm" maps to "teammsg", but vanilla "tm" should still show as mapped
        // Note: customCommandBaseNames was already collected in the first pass above
        for (String customBaseName : customCommandBaseNames) {
            // If we haven't already mapped this command, add it as mapped to itself
            if (!result.containsKey(customBaseName)) {
                // Find the custom command(s) with this base name
                List<String> matchingCustomCommands = new ArrayList<>();
                for (Map.Entry<String, List<String>> category : customCommands.entrySet()) {
                    for (String customCmd : category.getValue()) {
                        if (customCmd.toLowerCase().trim().equals("craftagent")) {
                            continue;
                        }
                        String baseName = customCmd.split("\\[|\\<")[0].trim().toLowerCase();
                        if (baseName.equals(customBaseName)) {
                            matchingCustomCommands.add(customCmd);
                        }
                    }
                }
                if (!matchingCustomCommands.isEmpty()) {
                    result.put(customBaseName, matchingCustomCommands);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Extracts the base Minecraft command name from a full command string.
     * Example: "give @s minecraft:diamond 64" -> "give"
     * Example: "/give @s minecraft:diamond 64" -> "give"
     */
    private static String extractBaseCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }
        String trimmed = command.trim();
        // Strip leading slash if present
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        String[] parts = trimmed.split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }
}
