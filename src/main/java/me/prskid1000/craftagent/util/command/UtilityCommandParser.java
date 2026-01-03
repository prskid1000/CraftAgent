package me.prskid1000.craftagent.util.command;

import java.util.*;

/**
 * Parser for utility and general Minecraft commands.
 * Supports: teleport, gamemode, experience, enchant, clear inventory, fill, clone,
 * say, tellraw, title, playsound, list, locate, attribute, damage, schedule, scoreboard, tag, team, etc.
 */
public class UtilityCommandParser implements CommandParser {
    
    @Override
    public boolean canParse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return false;
        String base = parts.get(0).toLowerCase();
        
        // Commands this parser handles
        return base.equals("teleport") || base.equals("tp") || 
               base.equals("gamemode") || base.equals("gm") ||
               base.equals("experience") || base.equals("xp") ||
               base.equals("enchant") ||
               base.equals("clear") ||
               base.equals("fill") ||
               base.equals("clone") ||
               base.equals("say") ||
               base.equals("tellraw") ||
               base.equals("title") ||
               base.equals("playsound") ||
               base.equals("list") ||
               base.equals("locate") ||
               base.equals("attribute") ||
               base.equals("damage") ||
               base.equals("schedule") ||
               base.equals("scoreboard") ||
               base.equals("tag") ||
               base.equals("team") ||
               base.equals("me") ||
               base.equals("msg") ||
               base.equals("tell") ||
               base.equals("w") ||
               base.equals("teammsg") ||
               base.equals("tm") ||
               base.equals("particle") ||
               base.equals("loot") ||
               base.equals("ride") ||
               base.equals("spreadplayers") ||
               base.equals("spawnpoint") ||
               base.equals("worldborder") ||
               base.equals("bossbar") ||
               base.equals("advancement") ||
               base.equals("gamerule") ||
               base.equals("forceload") ||
               base.equals("help") ||
               base.equals("trigger") ||
               base.equals("difficulty") ||
               base.equals("function") ||
               base.equals("datapack") ||
               base.equals("data") ||
               base.equals("fillbiome") ||
               base.equals("ban") ||
               base.equals("ban-ip") ||
               base.equals("banlist") ||
               base.equals("deop") ||
               base.equals("op") ||
               base.equals("kick") ||
               base.equals("pardon") ||
               base.equals("pardon-ip") ||
               base.equals("whitelist") ||
               base.equals("defaultgamemode") ||
               base.equals("setworldspawn") ||
               base.equals("publish") ||
               base.equals("reload") ||
               base.equals("debug") ||
               base.equals("execute");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return null;
        
        String baseCommand = parts.get(0).toLowerCase();
        
        // Teleport commands: "teleport <x> <y> <z>" or "teleport <target>"
        if (baseCommand.equals("teleport") || baseCommand.equals("tp")) {
            if (parts.size() >= 4) {
                // Try to parse coordinates
                try {
                    String x = parts.get(1);
                    String y = parts.get(2);
                    String z = parts.get(3);
                    return String.format("tp @s %s %s %s", x, y, z);
                } catch (Exception e) {
                    // If not coordinates, treat as target
                    if (parts.size() >= 2) {
                        return String.format("tp @s %s", parts.get(1));
                    }
                }
            } else if (parts.size() >= 2) {
                // Single target
                return String.format("tp @s %s", parts.get(1));
            }
            return null;
        }
        
        // Gamemode: "gamemode <mode>" or "gm <mode>"
        if (baseCommand.equals("gamemode") || baseCommand.equals("gm")) {
            if (parts.size() >= 2) {
                String mode = parts.get(1).toLowerCase();
                // Map common aliases
                if (mode.equals("survival") || mode.equals("0") || mode.equals("s")) {
                    return "gamemode survival @s";
                } else if (mode.equals("creative") || mode.equals("1") || mode.equals("c")) {
                    return "gamemode creative @s";
                } else if (mode.equals("adventure") || mode.equals("2") || mode.equals("a")) {
                    return "gamemode adventure @s";
                } else if (mode.equals("spectator") || mode.equals("3") || mode.equals("sp")) {
                    return "gamemode spectator @s";
                } else {
                    return String.format("gamemode %s @s", mode);
                }
            }
            return null;
        }
        
        // Experience: "experience <add|set> <amount> [levels]" or "xp <add|set> <amount> [levels]"
        if (baseCommand.equals("experience") || baseCommand.equals("xp")) {
            if (parts.size() >= 3) {
                String action = parts.get(1).toLowerCase(); // add or set
                String amount = parts.get(2);
                if (parts.size() >= 4 && parts.get(3).equalsIgnoreCase("levels")) {
                    return String.format("xp %s %sL @s", action, amount);
                } else {
                    return String.format("xp %s %s @s", action, amount);
                }
            } else if (parts.size() >= 2) {
                // Simplified: just amount, default to add
                String amount = parts.get(1);
                return String.format("xp add %s @s", amount);
            }
            return null;
        }
        
        // Enchant: "enchant <enchantment> [level]"
        if (baseCommand.equals("enchant")) {
            if (parts.size() >= 2) {
                String enchantment = parts.get(1);
                String level = parts.size() >= 3 ? parts.get(2) : "1";
                return String.format("enchant @s %s %s", enchantment, level);
            }
            return null;
        }
        
        // Clear inventory: "clear [item] [maxCount]"
        // Syntax: /clear [player] [item] [data] [maxCount]
        if (baseCommand.equals("clear")) {
            if (parts.size() >= 2) {
                String item = parts.get(1);
                String maxCount = parts.size() >= 3 ? parts.get(2) : "";
                if (!maxCount.isEmpty()) {
                    return String.format("clear @s %s 0 %s", item, maxCount);
                } else {
                    return String.format("clear @s %s", item);
                }
            } else {
                // Clear all inventory
                return "clear @s";
            }
        }
        
        // Fill: "fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [mode]"
        // Syntax: /fill <from> <to> <block> [replace|destroy|keep|hollow|outline]
        if (baseCommand.equals("fill")) {
            if (parts.size() >= 8) {
                String mode = parts.size() >= 9 ? parts.get(8) : "replace";
                return String.format("fill %s %s %s %s %s %s %s %s",
                    parts.get(1), parts.get(2), parts.get(3),
                    parts.get(4), parts.get(5), parts.get(6),
                    parts.get(7), mode);
            }
            return null;
        }
        
        // Clone: "clone <x1> <y1> <z1> <x2> <y2> <z2> <x> <y> <z> [mode]"
        // Syntax: /clone <begin> <end> <destination> [maskMode] [cloneMode]
        if (baseCommand.equals("clone")) {
            if (parts.size() >= 10) {
                String mode = parts.size() >= 11 ? parts.get(10) : "replace";
                return String.format("clone %s %s %s %s %s %s %s %s %s %s",
                    parts.get(1), parts.get(2), parts.get(3),
                    parts.get(4), parts.get(5), parts.get(6),
                    parts.get(7), parts.get(8), parts.get(9), mode);
            }
            return null;
        }
        
        // Say: "say <message>"
        if (baseCommand.equals("say")) {
            if (parts.size() >= 2) {
                String message = String.join(" ", parts.subList(1, parts.size()));
                return String.format("say %s", message);
            }
            return null;
        }
        
        // Tellraw: "tellraw <target> <json>"
        // Syntax: /tellraw <target> <json>
        // Note: JSON should be properly formatted, but we'll pass it through as-is
        if (baseCommand.equals("tellraw")) {
            if (parts.size() >= 3) {
                String target = parts.get(1);
                // Join all remaining parts as JSON (user should provide valid JSON)
                String json = String.join(" ", parts.subList(2, parts.size()));
                // If JSON doesn't start with {, wrap it in a text component
                if (!json.trim().startsWith("{")) {
                    json = String.format("{\"text\":\"%s\"}", json);
                }
                return String.format("tellraw %s %s", target, json);
            }
            return null;
        }
        
        // Title: "title <target> <title|subtitle|actionbar|clear|reset> [text]"
        // Syntax: /title <target> <title|subtitle|actionbar|clear|reset> [<text>]
        if (baseCommand.equals("title")) {
            if (parts.size() >= 3) {
                String target = parts.get(1);
                String type = parts.get(2).toLowerCase();
                
                // clear and reset don't need text
                if (type.equals("clear") || type.equals("reset")) {
                    return String.format("title %s %s", target, type);
                } else if (parts.size() >= 4) {
                    // title, subtitle, or actionbar need text
                    String text = String.join(" ", parts.subList(3, parts.size()));
                    return String.format("title %s %s {\"text\":\"%s\"}", target, type, text);
                }
            }
            return null;
        }
        
        // Playsound: "playsound <sound> <source> [target] [x] [y] [z] [volume] [pitch]"
        if (baseCommand.equals("playsound")) {
            if (parts.size() >= 3) {
                String sound = parts.get(1);
                String source = parts.get(2);
                String target = parts.size() >= 4 ? parts.get(3) : "@s";
                if (parts.size() >= 7) {
                    return String.format("playsound %s %s %s %s %s %s",
                        sound, source, target, parts.get(4), parts.get(5), parts.get(6));
                } else {
                    return String.format("playsound %s %s %s", sound, source, target);
                }
            }
            return null;
        }
        
        // List: "list" (simple command)
        if (baseCommand.equals("list")) {
            return "list";
        }
        
        // Locate: "locate <structure|biome>"
        if (baseCommand.equals("locate")) {
            if (parts.size() >= 2) {
                String type = parts.get(1);
                return String.format("locate %s", type);
            }
            return null;
        }
        
        // Attribute: "attribute <target> <attribute> <operation> ..."
        // Syntax: /attribute <target> <attribute> (get [<scale>]|base get [<scale>]|base set <value>|base reset|modifier add <id> <value> <operation>|modifier remove <id>|modifier value get <id> [<scale>])
        // Simplified for common use cases:
        // - attribute <target> <attribute> get [scale]
        // - attribute <target> <attribute> base get [scale]
        // - attribute <target> <attribute> base set <value>
        // - attribute <target> <attribute> base reset
        // - attribute <target> <attribute> modifier add <id> <value> <operation>
        // - attribute <target> <attribute> modifier remove <id>
        if (baseCommand.equals("attribute")) {
            if (parts.size() >= 4) {
                String target = parts.get(1);
                String attribute = parts.get(2);
                String operation = parts.get(3).toLowerCase();
                
                if (operation.equals("get")) {
                    // attribute <target> <attribute> get [scale]
                    String scale = parts.size() >= 5 ? parts.get(4) : "";
                    return scale.isEmpty() ? String.format("attribute %s %s get", target, attribute) 
                                          : String.format("attribute %s %s get %s", target, attribute, scale);
                } else if (operation.equals("base")) {
                    if (parts.size() >= 5) {
                        String baseOp = parts.get(4).toLowerCase();
                        if (baseOp.equals("get")) {
                            // attribute <target> <attribute> base get [scale]
                            String scale = parts.size() >= 6 ? parts.get(5) : "";
                            return scale.isEmpty() ? String.format("attribute %s %s base get", target, attribute)
                                                  : String.format("attribute %s %s base get %s", target, attribute, scale);
                        } else if (baseOp.equals("set") && parts.size() >= 6) {
                            // attribute <target> <attribute> base set <value>
                            return String.format("attribute %s %s base set %s", target, attribute, parts.get(5));
                        } else if (baseOp.equals("reset")) {
                            // attribute <target> <attribute> base reset
                            return String.format("attribute %s %s base reset", target, attribute);
                        }
                    }
                } else if (operation.equals("modifier")) {
                    if (parts.size() >= 5) {
                        String modOp = parts.get(4).toLowerCase();
                        if (modOp.equals("add") && parts.size() >= 8) {
                            // attribute <target> <attribute> modifier add <id> <value> <operation>
                            return String.format("attribute %s %s modifier add %s %s %s", 
                                target, attribute, parts.get(5), parts.get(6), parts.get(7));
                        } else if (modOp.equals("remove") && parts.size() >= 6) {
                            // attribute <target> <attribute> modifier remove <id>
                            return String.format("attribute %s %s modifier remove %s", target, attribute, parts.get(5));
                        } else if (modOp.equals("value") && parts.size() >= 7 && parts.get(5).equalsIgnoreCase("get")) {
                            // attribute <target> <attribute> modifier value get <id> [scale]
                            String scale = parts.size() >= 8 ? parts.get(7) : "";
                            return scale.isEmpty() ? String.format("attribute %s %s modifier value get %s", target, attribute, parts.get(6))
                                                  : String.format("attribute %s %s modifier value get %s %s", target, attribute, parts.get(6), scale);
                        }
                    }
                }
            }
            return null;
        }
        
        // Damage: "damage <target> <amount> [source]"
        if (baseCommand.equals("damage")) {
            if (parts.size() >= 3) {
                String target = parts.get(1);
                String amount = parts.get(2);
                if (parts.size() >= 4) {
                    return String.format("damage %s %s %s", target, amount, parts.get(3));
                } else {
                    return String.format("damage %s %s", target, amount);
                }
            }
            return null;
        }
        
        // Schedule: "schedule function <name> <time> [append|replace]"
        // Syntax: /schedule function <name> <time> [append|replace]
        if (baseCommand.equals("schedule")) {
            if (parts.size() >= 4) {
                // Format: schedule function <name> <time> [append|replace]
                String functionName = parts.get(2);
                String time = parts.get(3);
                String mode = parts.size() >= 5 ? parts.get(4) : "replace";
                return String.format("schedule function %s %s %s", functionName, time, mode);
            } else if (parts.size() >= 3) {
                // Simplified: schedule <name> <time>
                return String.format("schedule function %s %s", parts.get(1), parts.get(2));
            }
            return null;
        }
        
        // Scoreboard: "scoreboard <objective|players> <action> ..."
        if (baseCommand.equals("scoreboard")) {
            if (parts.size() >= 3) {
                String subcommand = parts.get(1).toLowerCase();
                if (subcommand.equals("objectives")) {
                    if (parts.size() >= 4) {
                        String action = parts.get(2);
                        String name = parts.get(3);
                        if (parts.size() >= 5) {
                            return String.format("scoreboard objectives %s %s %s", action, name, parts.get(4));
                        }
                        return String.format("scoreboard objectives %s %s", action, name);
                    }
                } else if (subcommand.equals("players")) {
                    if (parts.size() >= 5) {
                        return String.format("scoreboard players %s %s %s %s",
                            parts.get(2), parts.get(3), parts.get(4),
                            parts.size() >= 6 ? parts.get(5) : "");
                    }
                }
            }
            return null;
        }
        
        // Tag: "tag <target> <add|remove|list> <name>"
        if (baseCommand.equals("tag")) {
            if (parts.size() >= 3) {
                String target = parts.get(1);
                String action = parts.get(2).toLowerCase();
                if (action.equals("list")) {
                    return String.format("tag %s list", target);
                } else if (parts.size() >= 4) {
                    String name = parts.get(3);
                    return String.format("tag %s %s %s", target, action, name);
                }
            }
            return null;
        }
        
        // Team: "team <add|remove|join|leave|list> ..."
        if (baseCommand.equals("team")) {
            if (parts.size() >= 2) {
                String action = parts.get(1).toLowerCase();
                if (action.equals("list")) {
                    return "team list" + (parts.size() >= 3 ? " " + parts.get(2) : "");
                } else if (action.equals("add") && parts.size() >= 3) {
                    return String.format("team add %s", parts.get(2));
                } else if (action.equals("remove") && parts.size() >= 3) {
                    return String.format("team remove %s", parts.get(2));
                } else if (action.equals("join") && parts.size() >= 3) {
                    return String.format("team join %s %s", parts.get(2), parts.size() >= 4 ? parts.get(3) : "@s");
                } else if (action.equals("leave") && parts.size() >= 2) {
                    return String.format("team leave %s", parts.size() >= 3 ? parts.get(2) : "@s");
                }
            }
            return null;
        }
        
        // Me: "me <action>"
        if (baseCommand.equals("me")) {
            if (parts.size() >= 2) {
                String action = String.join(" ", parts.subList(1, parts.size()));
                return String.format("me %s", action);
            }
            return null;
        }
        
        // Msg/Tell/W: "msg <target> <message>" or "tell <target> <message>" or "w <target> <message>"
        if (baseCommand.equals("msg") || baseCommand.equals("tell") || baseCommand.equals("w")) {
            if (parts.size() >= 3) {
                String target = parts.get(1);
                String message = String.join(" ", parts.subList(2, parts.size()));
                return String.format("msg %s %s", target, message);
            }
            return null;
        }
        
        // Teammsg/Tm: "teammsg <message>" or "tm <message>"
        if (baseCommand.equals("teammsg") || baseCommand.equals("tm")) {
            if (parts.size() >= 2) {
                String message = String.join(" ", parts.subList(1, parts.size()));
                return String.format("teammsg %s", message);
            }
            return null;
        }
        
        // Particle: "particle <type> <x> <y> <z> [dx] [dy] [dz] [speed] [count]"
        if (baseCommand.equals("particle")) {
            if (parts.size() >= 5) {
                String type = parts.get(1);
                String x = parts.get(2);
                String y = parts.get(3);
                String z = parts.get(4);
                if (parts.size() >= 8) {
                    return String.format("particle %s %s %s %s %s %s %s %s",
                        type, x, y, z, parts.get(5), parts.get(6), parts.get(7),
                        parts.size() >= 9 ? parts.get(8) : "1");
                } else {
                    return String.format("particle %s %s %s %s", type, x, y, z);
                }
            }
            return null;
        }
        
        // Loot: "loot <spawn|replace|give> <target> <source> ..."
        // Syntax: /loot <spawn|replace|give> <target> <source> ...
        if (baseCommand.equals("loot")) {
            if (parts.size() >= 4) {
                // Format: loot <spawn|replace|give> <target> <source> <sourceType> ...
                return String.format("loot %s %s %s %s",
                    parts.get(1), parts.get(2), parts.get(3),
                    parts.size() >= 5 ? parts.get(4) : "");
            }
            return null;
        }
        
        // Ride: "ride <target> mount <vehicle>" or "ride <target> dismount"
        if (baseCommand.equals("ride")) {
            if (parts.size() >= 3) {
                String target = parts.get(1);
                String action = parts.get(2).toLowerCase();
                if (action.equals("dismount")) {
                    return String.format("ride %s dismount", target);
                } else if (parts.size() >= 4 && action.equals("mount")) {
                    return String.format("ride %s mount %s", target, parts.get(3));
                }
            }
            return null;
        }
        
        // Spreadplayers: "spreadplayers <x> <z> <spreadDistance> <maxRange> <targets>"
        if (baseCommand.equals("spreadplayers")) {
            if (parts.size() >= 6) {
                return String.format("spreadplayers %s %s %s %s %s",
                    parts.get(1), parts.get(2), parts.get(3), parts.get(4), parts.get(5));
            }
            return null;
        }
        
        // Spawnpoint: "spawnpoint [target] [x] [y] [z]"
        if (baseCommand.equals("spawnpoint")) {
            if (parts.size() >= 5) {
                return String.format("spawnpoint %s %s %s %s",
                    parts.get(1), parts.get(2), parts.get(3), parts.get(4));
            } else if (parts.size() >= 2) {
                return String.format("spawnpoint %s", parts.get(1));
            } else {
                return "spawnpoint @s";
            }
        }
        
        // Worldborder: "worldborder <get|set|add|center> ..."
        if (baseCommand.equals("worldborder")) {
            if (parts.size() >= 2) {
                String action = parts.get(1).toLowerCase();
                if (action.equals("get")) {
                    return "worldborder get";
                } else if (action.equals("set") && parts.size() >= 3) {
                    return String.format("worldborder set %s", parts.get(2));
                } else if (action.equals("add") && parts.size() >= 3) {
                    return String.format("worldborder add %s", parts.get(2));
                } else if (action.equals("center") && parts.size() >= 4) {
                    return String.format("worldborder center %s %s", parts.get(2), parts.get(3));
                }
            }
            return "worldborder get";
        }
        
        // Bossbar: "bossbar <add|remove|set|get|list> <id> [name]"
        // Syntax: /bossbar <add|remove|set|get|list> <id> [<name>]
        if (baseCommand.equals("bossbar")) {
            if (parts.size() >= 2) {
                String action = parts.get(1).toLowerCase();
                if (action.equals("list")) {
                    return "bossbar list";
                } else if (action.equals("add") && parts.size() >= 3) {
                    String name = parts.size() >= 4 ? parts.get(3) : "\"Bossbar\"";
                    return String.format("bossbar add %s %s", parts.get(2), name);
                } else if (action.equals("remove") && parts.size() >= 3) {
                    return String.format("bossbar remove %s", parts.get(2));
                } else if (action.equals("set") && parts.size() >= 4) {
                    return String.format("bossbar set %s %s %s", parts.get(2), parts.get(3),
                        parts.size() >= 5 ? parts.get(4) : "");
                } else if (action.equals("get") && parts.size() >= 3) {
                    return String.format("bossbar get %s", parts.get(2));
                }
            }
            return null;
        }
        
        // Advancement: "advancement <grant|revoke> <target> <only|from|through|until|everything> [advancement]"
        // Syntax: /advancement <grant|revoke> <player> <only|from|through|until|everything> [<advancement>]
        // Note: "everything" doesn't need an advancement parameter
        if (baseCommand.equals("advancement")) {
            if (parts.size() >= 4) {
                String scope = parts.get(3).toLowerCase();
                if (scope.equals("everything")) {
                    // everything doesn't need advancement parameter
                    return String.format("advancement %s %s %s",
                        parts.get(1), parts.get(2), scope);
                } else if (parts.size() >= 5) {
                    // other scopes need advancement parameter
                    return String.format("advancement %s %s %s %s",
                        parts.get(1), parts.get(2), parts.get(3), parts.get(4));
                }
            }
            return null;
        }
        
        // Gamerule: "gamerule <rule> [value]"
        if (baseCommand.equals("gamerule")) {
            if (parts.size() >= 2) {
                String rule = parts.get(1);
                if (parts.size() >= 3) {
                    return String.format("gamerule %s %s", rule, parts.get(2));
                } else {
                    return String.format("gamerule %s", rule);
                }
            }
            return null;
        }
        
        // Forceload: "forceload <add|remove|query> <chunkX> <chunkZ>"
        // Syntax: /forceload <add|remove|query> [<pos>]
        if (baseCommand.equals("forceload")) {
            if (parts.size() >= 2) {
                String action = parts.get(1).toLowerCase();
                if (action.equals("query")) {
                    return "forceload query";
                } else if (parts.size() >= 4) {
                    // Format: forceload add <chunkX> <chunkZ> [<fromChunkX> <fromChunkZ>]
                    if (parts.size() >= 6) {
                        return String.format("forceload %s %s %s %s %s", 
                            action, parts.get(2), parts.get(3), parts.get(4), parts.get(5));
                    } else {
                        return String.format("forceload %s %s %s", action, parts.get(2), parts.get(3));
                    }
                }
            }
            return null;
        }
        
        // Help: "help [command]"
        if (baseCommand.equals("help")) {
            if (parts.size() >= 2) {
                return String.format("help %s", parts.get(1));
            }
            return "help";
        }
        
        // Trigger: "trigger <objective> [add|set] [value]"
        if (baseCommand.equals("trigger")) {
            if (parts.size() >= 2) {
                String objective = parts.get(1);
                if (parts.size() >= 4) {
                    return String.format("trigger %s %s %s", objective, parts.get(2), parts.get(3));
                } else if (parts.size() >= 3) {
                    return String.format("trigger %s %s", objective, parts.get(2));
                } else {
                    return String.format("trigger %s", objective);
                }
            }
            return null;
        }
        
        // Difficulty: "difficulty <peaceful|easy|normal|hard>"
        if (baseCommand.equals("difficulty")) {
            if (parts.size() >= 2) {
                return String.format("difficulty %s", parts.get(1));
            }
            return null;
        }
        
        // Function: "function <name>"
        if (baseCommand.equals("function")) {
            if (parts.size() >= 2) {
                return String.format("function %s", parts.get(1));
            }
            return null;
        }
        
        // Datapack: "datapack <enable|disable|list> [name]"
        if (baseCommand.equals("datapack")) {
            if (parts.size() >= 2) {
                String action = parts.get(1).toLowerCase();
                if (action.equals("list")) {
                    return "datapack list" + (parts.size() >= 3 ? " " + parts.get(2) : "");
                } else if (parts.size() >= 3) {
                    return String.format("datapack %s %s", action, parts.get(2));
                }
            }
            return null;
        }
        
        // Data: "data <get|merge|modify|remove> <target> [path] [value]"
        // Syntax: /data <get|merge|modify|remove> <target> [<path>] [<value>]
        // For modify: data modify <target> <path> <operation> <value>
        if (baseCommand.equals("data")) {
            if (parts.size() >= 3) {
                String action = parts.get(1).toLowerCase();
                String target = parts.get(2);
                
                if (action.equals("modify") && parts.size() >= 6) {
                    // Modify needs: data modify <target> <path> <operation> <value>
                    return String.format("data modify %s %s %s %s", target, parts.get(3), parts.get(4), parts.get(5));
                } else if (parts.size() >= 4) {
                    // Has path parameter
                    return String.format("data %s %s %s", action, target, parts.get(3));
                } else {
                    // No path, just action and target
                    return String.format("data %s %s", action, target);
                }
            }
            return null;
        }
        
        // Fillbiome: "fillbiome <x1> <y1> <z1> <x2> <y2> <z2> <biome>"
        if (baseCommand.equals("fillbiome")) {
            if (parts.size() >= 8) {
                return String.format("fillbiome %s %s %s %s %s %s %s",
                    parts.get(1), parts.get(2), parts.get(3),
                    parts.get(4), parts.get(5), parts.get(6),
                    parts.get(7));
            }
            return null;
        }
        
        // Admin commands - pass through with parameters
        // Ban: "ban <player> [reason]"
        if (baseCommand.equals("ban")) {
            if (parts.size() >= 2) {
                String player = parts.get(1);
                String reason = parts.size() >= 3 ? String.join(" ", parts.subList(2, parts.size())) : "";
                return reason.isEmpty() ? String.format("ban %s", player) : String.format("ban %s %s", player, reason);
            }
            return null;
        }
        
        // Ban-ip: "ban-ip <address> [reason]"
        if (baseCommand.equals("ban-ip")) {
            if (parts.size() >= 2) {
                String address = parts.get(1);
                String reason = parts.size() >= 3 ? String.join(" ", parts.subList(2, parts.size())) : "";
                return reason.isEmpty() ? String.format("ban-ip %s", address) : String.format("ban-ip %s %s", address, reason);
            }
            return null;
        }
        
        // Banlist: "banlist [ips|players]"
        if (baseCommand.equals("banlist")) {
            if (parts.size() >= 2) {
                return String.format("banlist %s", parts.get(1));
            }
            return "banlist";
        }
        
        // Deop: "deop <player>"
        if (baseCommand.equals("deop")) {
            if (parts.size() >= 2) {
                return String.format("deop %s", parts.get(1));
            }
            return null;
        }
        
        // Op: "op <player>"
        if (baseCommand.equals("op")) {
            if (parts.size() >= 2) {
                return String.format("op %s", parts.get(1));
            }
            return null;
        }
        
        // Kick: "kick <player> [reason]"
        if (baseCommand.equals("kick")) {
            if (parts.size() >= 2) {
                String player = parts.get(1);
                String reason = parts.size() >= 3 ? String.join(" ", parts.subList(2, parts.size())) : "";
                return reason.isEmpty() ? String.format("kick %s", player) : String.format("kick %s %s", player, reason);
            }
            return null;
        }
        
        // Pardon: "pardon <player>"
        if (baseCommand.equals("pardon")) {
            if (parts.size() >= 2) {
                return String.format("pardon %s", parts.get(1));
            }
            return null;
        }
        
        // Pardon-ip: "pardon-ip <address>"
        if (baseCommand.equals("pardon-ip")) {
            if (parts.size() >= 2) {
                return String.format("pardon-ip %s", parts.get(1));
            }
            return null;
        }
        
        // Whitelist: "whitelist <add|remove|list|on|off|reload> [player]"
        if (baseCommand.equals("whitelist")) {
            if (parts.size() >= 2) {
                String action = parts.get(1).toLowerCase();
                if (action.equals("list") || action.equals("on") || action.equals("off") || action.equals("reload")) {
                    return String.format("whitelist %s", action);
                } else if (parts.size() >= 3) {
                    return String.format("whitelist %s %s", action, parts.get(2));
                }
            }
            return null;
        }
        
        // Defaultgamemode: "defaultgamemode <mode>"
        if (baseCommand.equals("defaultgamemode")) {
            if (parts.size() >= 2) {
                return String.format("defaultgamemode %s", parts.get(1));
            }
            return null;
        }
        
        // Setworldspawn: "setworldspawn [x] [y] [z]"
        if (baseCommand.equals("setworldspawn")) {
            if (parts.size() >= 4) {
                return String.format("setworldspawn %s %s %s", parts.get(1), parts.get(2), parts.get(3));
            } else {
                return "setworldspawn";
            }
        }
        
        // Publish: "publish"
        if (baseCommand.equals("publish")) {
            return "publish";
        }
        
        // Reload: "reload"
        if (baseCommand.equals("reload")) {
            return "reload";
        }
        
        // Debug: "debug <start|stop|function>"
        if (baseCommand.equals("debug")) {
            if (parts.size() >= 2) {
                return String.format("debug %s", parts.get(1));
            }
            return null;
        }
        
        // Execute: "execute <subcommand> ..." (complex command, pass through)
        if (baseCommand.equals("execute")) {
            if (parts.size() >= 2) {
                return String.format("execute %s", String.join(" ", parts.subList(1, parts.size())));
            }
            return null;
        }
        
        return null;
    }
    
    @Override
    public String getCategory() {
        return "Utility";
    }
}

