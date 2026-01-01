package me.prskid1000.craftagent.llm;

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
     * Creates the addOrUpdateInfo tool definition.
     * Allows LLM to add or update contacts and locations in memory.
     */
    public static Map<String, Object> getAddOrUpdateInfoTool() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "addOrUpdateInfo");
        function.put("description", "Add or update information in memory (contacts or locations). " +
                "For contacts: Use to add new contacts from nearbyEntities or update existing ones (relationship, enmity, friendship). " +
                "For locations: Use to save important locations at your current position. " +
                "If the contact/location already exists, it will be updated with the new values.");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> infoTypeParam = new HashMap<>();
        infoTypeParam.put("type", "string");
        infoTypeParam.put("description", "Type of information: 'contact' or 'location'");
        infoTypeParam.put("enum", List.of("contact", "location"));
        properties.put("infoType", infoTypeParam);
        
        Map<String, Object> nameParam = new HashMap<>();
        nameParam.put("type", "string");
        nameParam.put("description", "Name: For contacts, use the name from nearbyEntities. For locations, use a descriptive name (e.g., 'My Home', 'Diamond Mine')");
        properties.put("name", nameParam);
        
        // Contact-specific fields
        Map<String, Object> relationshipParam = new HashMap<>();
        relationshipParam.put("type", "string");
        relationshipParam.put("description", "For contacts: Relationship type (optional, default: 'neutral'). Values: 'friend', 'enemy', 'neutral', 'teammate', 'stranger', 'acquaintance', 'close_ally', 'rival'");
        properties.put("relationship", relationshipParam);
        
        Map<String, Object> notesParam = new HashMap<>();
        notesParam.put("type", "string");
        notesParam.put("description", "For contacts: Optional notes about this contact (e.g., 'Met at spawn', 'Helped me mine')");
        properties.put("notes", notesParam);
        
        Map<String, Object> enmityLevelParam = new HashMap<>();
        enmityLevelParam.put("type", "number");
        enmityLevelParam.put("description", "For contacts: Enmity level (optional, default: 0.0). Range: 0.0 to 1.0. Use to set absolute value or update existing");
        properties.put("enmityLevel", enmityLevelParam);
        
        Map<String, Object> friendshipLevelParam = new HashMap<>();
        friendshipLevelParam.put("type", "number");
        friendshipLevelParam.put("description", "For contacts: Friendship level (optional, default: 0.0). Range: 0.0 to 1.0. Use to set absolute value or update existing");
        properties.put("friendshipLevel", friendshipLevelParam);
        
        // Location-specific fields
        Map<String, Object> descriptionParam = new HashMap<>();
        descriptionParam.put("type", "string");
        descriptionParam.put("description", "For locations: Description of what this location is or why it's important (required for locations)");
        properties.put("description", descriptionParam);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("infoType", "name"));
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }

    /**
     * Creates the removeInfo tool definition.
     * Allows LLM to remove contacts and locations from memory.
     */
    public static Map<String, Object> getRemoveInfoTool() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "removeInfo");
        function.put("description", "Remove information from memory (contacts or locations). " +
                "Use this when you want to forget about someone or a location (e.g., they're no longer relevant, " +
                "you want to free up memory space, or you've had a falling out).");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> infoTypeParam = new HashMap<>();
        infoTypeParam.put("type", "string");
        infoTypeParam.put("description", "Type of information to remove: 'contact' or 'location'");
        infoTypeParam.put("enum", List.of("contact", "location"));
        properties.put("infoType", infoTypeParam);
        
        Map<String, Object> nameParam = new HashMap<>();
        nameParam.put("type", "string");
        nameParam.put("description", "Name of the contact or location to remove");
        properties.put("name", nameParam);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("infoType", "name"));
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }

    /**
     * Returns the list of tools for tool calling.
     * Includes execute_command and memory management tools.
     */
    public static List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(getExecuteCommandTool());
        tools.add(getAddOrUpdateInfoTool());
        tools.add(getRemoveInfoTool());
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

