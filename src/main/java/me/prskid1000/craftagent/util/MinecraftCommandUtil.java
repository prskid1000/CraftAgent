package me.prskid1000.craftagent.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for discovering and executing vanilla Minecraft commands using Brigadier.
 * All command operations use Brigadier's CommandDispatcher exclusively.
 * 
 * This class provides methods to:
 * - Discover all registered commands via Brigadier's command tree
 * - Execute commands through Brigadier's dispatcher
 * - Validate and get suggestions using Brigadier's parser
 */
public class MinecraftCommandUtil {

    private MinecraftCommandUtil() {
        // Utility class - no instantiation
    }

    /**
     * Gets all registered command names from Brigadier's command dispatcher.
     * Uses Brigadier's CommandDispatcher to traverse the command tree.
     * 
     * Approximate command counts:
     * - Vanilla Minecraft 1.21.8: ~115+ base commands
     * - With Fabric API: Additional commands may be added
     * - With other mods: Count increases based on installed mods
     * 
     * @param server The Minecraft server instance
     * @return A set of all registered command names (literal commands)
     */
    public static Set<String> getAllCommandNames(MinecraftServer server) {
        if (server == null) {
            return Collections.emptySet();
        }
        
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        Set<String> commandNames = new HashSet<>();
        
        // Iterate through all root nodes (literal commands)
        for (CommandNode<ServerCommandSource> node : dispatcher.getRoot().getChildren()) {
            if (node instanceof LiteralCommandNode) {
                commandNames.add(node.getName());
                // Also collect subcommands
                collectSubcommands(node, commandNames, "");
            }
        }
        
        return commandNames;
    }

    /**
     * Recursively collects command names including subcommands.
     * 
     * @param node The command node to traverse
     * @param commandNames The set to add command names to
     * @param prefix The current command prefix (for nested commands)
     */
    private static void collectSubcommands(CommandNode<ServerCommandSource> node, Set<String> commandNames, String prefix) {
        String currentPath = prefix.isEmpty() ? node.getName() : prefix + " " + node.getName();
        
        for (CommandNode<ServerCommandSource> child : node.getChildren()) {
            if (child instanceof LiteralCommandNode) {
                commandNames.add(currentPath + " " + child.getName());
                collectSubcommands(child, commandNames, currentPath);
            }
        }
    }

    /**
     * Gets all registered commands with their descriptions and usage information using Brigadier.
     * Uses Brigadier's getSmartUsage() method to get command usage strings.
     * 
     * @param server The Minecraft server instance
     * @return A map of command names to their usage strings
     */
    public static Map<String, String> getAllCommandsWithUsage(MinecraftServer server) {
        if (server == null) {
            return Collections.emptyMap();
        }
        
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        Map<String, String> commands = new HashMap<>();
        
        // Create a dummy command source for getting command usage
        ServerCommandSource dummySource = server.getCommandSource()
                .withLevel(4); // OP level 4 to see all commands
        
        for (CommandNode<ServerCommandSource> node : dispatcher.getRoot().getChildren()) {
            if (node instanceof LiteralCommandNode) {
                String commandName = node.getName();
                try {
                    // Get usage string for the command (includes parameters)
                    // getSmartUsage returns a map of possible command paths
                    Map<CommandNode<ServerCommandSource>, String> usageMap = dispatcher.getSmartUsage(node, dummySource);
                    if (!usageMap.isEmpty()) {
                        // Get the first (most common) usage path
                        String usage = usageMap.values().iterator().next();
                        // Remove the command name prefix if it's duplicated
                        if (usage.startsWith(commandName + " ")) {
                            usage = usage.substring(commandName.length() + 1);
                        }
                        commands.put(commandName, usage);
                    } else {
                        // No usage found, just use empty string (will show command name only)
                        commands.put(commandName, "");
                    }
                } catch (Exception e) {
                    // If we can't get usage, just add empty string (will show command name only)
                    commands.put(commandName, "");
                }
            }
        }
        
        return commands;
    }

    /**
     * Gets a formatted list of all available commands as a string.
     * Useful for displaying to LLMs or logging.
     * 
     * @param server The Minecraft server instance
     * @return A formatted string listing all commands
     */
    public static String getFormattedCommandList(MinecraftServer server) {
        Set<String> commands = getAllCommandNames(server);
        if (commands.isEmpty()) {
            return "No commands available";
        }
        
        return commands.stream()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * Gets a formatted list of commands with their usage/parameters for LLM system prompts.
     * This provides the LLM with full command syntax including parameters.
     * 
     * Format example:
     * "• give <targets> <item> [<count>]"
     * "• tp <targets> <pos>"
     * "• effect give <targets> <effect> [<seconds>] [<amplifier>]"
     * 
     * @param server The Minecraft server instance
     * @return A formatted string with commands and their usage information (parameters)
     */
    public static String getFormattedCommandsWithUsage(MinecraftServer server) {
        Map<String, String> commandsWithUsage = getAllCommandsWithUsage(server);
        if (commandsWithUsage.isEmpty()) {
            return "No commands available";
        }
        
        return commandsWithUsage.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String command = entry.getKey();
                    String usage = entry.getValue();
                    // Format as: "• command <param1> [param2]"
                    if (usage == null || usage.trim().isEmpty()) {
                        return "• " + command;
                    } else {
                        return "• " + command + " " + usage;
                    }
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * Gets the MOST COMPLETE command set using Brigadier's getSmartUsage().
     * This method uses Brigadier to discover all possible command paths, including those that might
     * not be directly accessible as literal nodes. This is the superset of all commands.
     * 
     * @param server The Minecraft server instance
     * @return A set containing all possible command paths (most complete set via Brigadier)
     */
    public static Set<String> getAllCommandsComplete(MinecraftServer server) {
        if (server == null) {
            return Collections.emptySet();
        }
        
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        Set<String> allCommands = new HashSet<>();
        
        // Create command source with highest permissions to see all commands
        ServerCommandSource source = server.getCommandSource()
                .withLevel(4);
        
        // Get all root commands
        for (CommandNode<ServerCommandSource> node : dispatcher.getRoot().getChildren()) {
            if (node instanceof LiteralCommandNode) {
                String commandName = node.getName();
                allCommands.add(commandName);
                
                // Use getSmartUsage to discover all possible command paths
                try {
                    Map<CommandNode<ServerCommandSource>, String> usage = dispatcher.getSmartUsage(node, source);
                    for (String usageString : usage.values()) {
                        // Parse usage strings to extract command paths
                        // Usage format: "command <arg1> [arg2] ..."
                        String baseCommand = usageString.split(" ")[0];
                        if (!baseCommand.startsWith("<") && !baseCommand.startsWith("[")) {
                            allCommands.add(baseCommand);
                        }
                        // Add full usage as a command pattern
                        allCommands.add(usageString);
                    }
                } catch (Exception e) {
                    // If getSmartUsage fails, fall back to basic collection
                    collectSubcommands(node, allCommands, "");
                }
                
                // Also collect subcommands recursively
                collectSubcommands(node, allCommands, "");
            }
        }
        
        return allCommands;
    }

    /**
     * Executes a vanilla Minecraft command using Brigadier's dispatcher.
     * The command is executed through Brigadier's command system.
     * 
     * @param player The player entity to execute the command as
     * @param command The command string to execute (without leading slash)
     * @return true if the command was executed successfully, false otherwise
     */
    public static boolean executeCommand(ServerPlayerEntity player, String command) {
        if (player == null || command == null || command.trim().isEmpty()) {
            return false;
        }
        
        try {
            MinecraftServer server = player.getServer();
            if (server == null) {
                LogUtil.error("Cannot execute command: server is null");
                return false;
            }
            
            // Remove leading slash if present
            String commandToExecute = command.startsWith("/") ? command.substring(1) : command;
            
            // Create command source with OP level 4 (highest permission)
            ServerCommandSource source = player.getCommandSource()
                    .withLevel(4);
            
            // Execute the command on the server thread
            server.getCommandManager().executeWithPrefix(source, commandToExecute);
            
            return true;
        } catch (Exception e) {
            LogUtil.error("Error executing Minecraft command: " + command, e);
            return false;
        }
    }

    /**
     * Executes a vanilla Minecraft command using Brigadier's dispatcher with callbacks.
     * The command is executed through Brigadier's command system.
     * 
     * @param player The player entity to execute the command as
     * @param command The command string to execute (without leading slash)
     * @param onSuccess Callback to run on successful execution
     * @param onError Callback to run on error (receives the exception)
     * @return true if the command was executed successfully, false otherwise
     */
    public static boolean executeCommand(ServerPlayerEntity player, String command, 
                                        Runnable onSuccess, java.util.function.Consumer<Exception> onError) {
        if (player == null || command == null || command.trim().isEmpty()) {
            if (onError != null) {
                onError.accept(new IllegalArgumentException("Player or command is null/empty"));
            }
            return false;
        }
        
        try {
            MinecraftServer server = player.getServer();
            if (server == null) {
                Exception e = new IllegalStateException("Server is null");
                if (onError != null) {
                    onError.accept(e);
                }
                LogUtil.error("Cannot execute command: server is null");
                return false;
            }
            
            // Remove leading slash if present
            String commandToExecute = command.startsWith("/") ? command.substring(1) : command;
            
            // Create command source with OP level 4 (highest permission)
            ServerCommandSource source = player.getCommandSource()
                    .withLevel(4);
            
            // Execute the command on the server thread
            // executeWithPrefix returns void, so we assume success if no exception is thrown
            try {
                server.getCommandManager().executeWithPrefix(source, commandToExecute);
                if (onSuccess != null) {
                    onSuccess.run();
                }
                return true;
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e);
                }
                throw e; // Re-throw to be caught by outer catch
            }
        } catch (Exception e) {
            if (onError != null) {
                onError.accept(e);
            }
            LogUtil.error("Error executing Minecraft command: " + command, e);
            return false;
        }
    }

    /**
     * Checks if a command exists using Brigadier's parser.
     * Uses Brigadier's parse() method to validate command existence.
     * 
     * @param server The Minecraft server instance
     * @param commandName The command name to check (without leading slash)
     * @return true if the command exists, false otherwise
     */
    public static boolean commandExists(MinecraftServer server, String commandName) {
        if (server == null || commandName == null || commandName.trim().isEmpty()) {
            return false;
        }
        
        String cmd = commandName.startsWith("/") ? commandName.substring(1) : commandName;
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        
        try {
            // Try to parse the command to see if it exists
            dispatcher.parse(cmd, server.getCommandSource());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets suggestions for a partial command using Brigadier's completion system.
     * Uses Brigadier's getCompletionSuggestions() method for autocomplete.
     * 
     * @param server The Minecraft server instance
     * @param partialCommand The partial command string
     * @param source The command source to get suggestions for
     * @return A list of command suggestions from Brigadier
     */
    public static List<String> getCommandSuggestions(MinecraftServer server, String partialCommand, ServerCommandSource source) {
        if (server == null || partialCommand == null || partialCommand.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String cmd = partialCommand.startsWith("/") ? partialCommand.substring(1) : partialCommand;
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        
        try {
            com.mojang.brigadier.ParseResults<ServerCommandSource> parseResults = dispatcher.parse(cmd, source);
            return dispatcher.getCompletionSuggestions(parseResults).join().getList().stream()
                    .map(suggestion -> suggestion.getText())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LogUtil.error("Error getting command suggestions for: " + partialCommand, e);
            return Collections.emptyList();
        }
    }
}

