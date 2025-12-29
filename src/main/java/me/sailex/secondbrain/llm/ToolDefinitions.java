package me.sailex.secondbrain.llm;

import me.sailex.altoclef.commandsystem.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool definitions for LLM tool calling.
 * Commands are defined as tools, while messages use structured output.
 */
public class ToolDefinitions {

    private ToolDefinitions() {}

    /**
     * Creates the execute_command tool definition.
     * This tool allows the LLM to execute Minecraft commands.
     */
    public static Map<String, Object> getExecuteCommandTool() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "execute_command");
        function.put("description", "Execute a Minecraft command. Use this when you want to perform an action in the game. " +
                "You can execute one command at a time. Use 'idle' to do nothing.");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> commandParam = new HashMap<>();
        commandParam.put("type", "string");
        commandParam.put("description", "The AltoClef command to execute. Must be exactly one command from the available commands list. " +
                "Do not invent new commands. Use only commands that are provided in the system prompt.");
        properties.put("command", commandParam);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("command"));
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }

    /**
     * Returns the list of tools for tool calling.
     * Currently only includes execute_command.
     */
    public static List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(getExecuteCommandTool());
        return tools;
    }

    /**
     * Creates a message schema for structured output.
     * Used for chat messages (simple data, not actions).
     */
    public static Map<String, Object> getMessageSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "message", Map.of(
                    "type", "string",
                    "description", "An optional in-character chat message (under 250 characters). Use empty string \"\" if the NPC should not speak."
                )
            ),
            "required", List.of("message")
        );
    }
}

