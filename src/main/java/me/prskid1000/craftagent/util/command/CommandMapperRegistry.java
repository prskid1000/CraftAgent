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
        registerParser(new UtilityCommandParser());
        
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
        
        // Simple utility commands (exact matches)
        exactMatches.put("list", "list");
        exactMatches.put("stopsound", "stopsound @s");
        exactMatches.put("spectate", "spectate @s");
        exactMatches.put("recipe", "recipe give @s *");
        exactMatches.put("seed", "seed");
        exactMatches.put("version", "version");
        exactMatches.put("help", "help");
        exactMatches.put("spawnpoint", "spawnpoint @s");
        exactMatches.put("worldborder", "worldborder get");
        
        // Add direct aliases for vanilla commands (so they show as mapped)
        // These allow NPCs to use vanilla command names directly
        // Note: For commands that need parameters, these are just placeholders
        // The parser will handle the actual parameter parsing
        exactMatches.put("teleport", "teleport @s");
        exactMatches.put("tp", "teleport @s");
        exactMatches.put("say", "say");
        exactMatches.put("me", "me");
        exactMatches.put("tell", "tell");
        exactMatches.put("msg", "msg");
        exactMatches.put("w", "msg");
        exactMatches.put("tellraw", "tellraw");
        exactMatches.put("title", "title");
        exactMatches.put("playsound", "playsound");
        exactMatches.put("locate", "locate");
        exactMatches.put("attribute", "attribute");
        exactMatches.put("damage", "damage");
        exactMatches.put("schedule", "schedule");
        exactMatches.put("scoreboard", "scoreboard");
        exactMatches.put("tag", "tag");
        exactMatches.put("team", "team");
        exactMatches.put("teammsg", "teammsg");
        exactMatches.put("tm", "teammsg");
        exactMatches.put("particle", "particle");
        exactMatches.put("loot", "loot");
        exactMatches.put("ride", "ride");
        exactMatches.put("spreadplayers", "spreadplayers");
        exactMatches.put("bossbar", "bossbar");
        exactMatches.put("advancement", "advancement");
        exactMatches.put("gamerule", "gamerule");
        exactMatches.put("forceload", "forceload");
        exactMatches.put("gamemode", "gamemode");
        exactMatches.put("gm", "gamemode");
        exactMatches.put("experience", "experience");
        exactMatches.put("xp", "experience");
        exactMatches.put("enchant", "enchant");
        exactMatches.put("clear", "clear");
        exactMatches.put("fill", "fill");
        exactMatches.put("clone", "clone");
        exactMatches.put("trigger", "trigger");
        exactMatches.put("difficulty", "difficulty");
        exactMatches.put("function", "function");
        exactMatches.put("datapack", "datapack");
        exactMatches.put("data", "data");
        exactMatches.put("fillbiome", "fillbiome");
        
        // Admin commands (pass-through, may not be useful for NPCs but added for completeness)
        exactMatches.put("ban", "ban");
        exactMatches.put("ban-ip", "ban-ip");
        exactMatches.put("banlist", "banlist");
        exactMatches.put("deop", "deop");
        exactMatches.put("op", "op");
        exactMatches.put("kick", "kick");
        exactMatches.put("pardon", "pardon");
        exactMatches.put("pardon-ip", "pardon-ip");
        exactMatches.put("whitelist", "whitelist");
        exactMatches.put("defaultgamemode", "defaultgamemode");
        exactMatches.put("setworldspawn", "setworldspawn");
        exactMatches.put("publish", "publish");
        exactMatches.put("reload", "reload");
        exactMatches.put("debug", "debug");
        
        // Mod-specific or unknown commands (pass-through)
        exactMatches.put("carpet", "carpet");
        exactMatches.put("counter", "counter");
        exactMatches.put("dialog", "dialog");
        exactMatches.put("distance", "distance");
        exactMatches.put("draw", "draw");
        exactMatches.put("execute", "execute");
        exactMatches.put("info", "info");
        exactMatches.put("item", "item");
        exactMatches.put("jfr", "jfr");
        exactMatches.put("log", "log");
        exactMatches.put("place", "place");
        exactMatches.put("player", "player");
        exactMatches.put("perf", "perf");
        exactMatches.put("perimeterinfo", "perimeterinfo");
        exactMatches.put("profile", "profile");
        exactMatches.put("random", "random");
        exactMatches.put("return", "return");
        exactMatches.put("rotate", "rotate");
        exactMatches.put("script", "script");
        exactMatches.put("spawn", "spawn");
        exactMatches.put("test", "test");
        exactMatches.put("tick", "tick");
        exactMatches.put("track", "track");
        exactMatches.put("waypoint", "waypoint");
    }
}

