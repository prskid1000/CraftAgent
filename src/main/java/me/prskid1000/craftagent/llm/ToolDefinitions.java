package me.prskid1000.craftagent.llm;

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
        commandParam.put("description", "The Minecraft command to execute. Must be exactly one command from the available commands list. " +
                "Do not invent new commands. Use only vanilla Minecraft commands that are provided in the system prompt (e.g., 'give', 'tp', 'effect', 'summon', 'setblock', 'fill', etc.).");
        properties.put("command", commandParam);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("command"));
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }

    /**
     * Creates the manageMemory tool definition.
     * Allows LLM to add, update, or remove contacts and locations in memory.
     * Merged from addOrUpdateInfo and removeInfo tools.
     */
    public static Map<String, Object> getManageMemoryTool() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "manageMemory");
        function.put("description", "Manage information in memory (contacts or locations). " +
                "Use 'add' or 'update' to add new contacts from nearbyEntities or update existing ones (relationship, enmity, friendship), " +
                "or to save important locations at your current position. " +
                "Use 'remove' to forget about someone or a location (e.g., they're no longer relevant, " +
                "you want to free up memory space, or you've had a falling out).");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> actionParam = new HashMap<>();
        actionParam.put("type", "string");
        actionParam.put("description", "Action to perform: 'add' (or 'update' - same as add), or 'remove'");
        actionParam.put("enum", List.of("add", "update", "remove"));
        properties.put("action", actionParam);
        
        Map<String, Object> infoTypeParam = new HashMap<>();
        infoTypeParam.put("type", "string");
        infoTypeParam.put("description", "Type of information: 'contact' or 'location'");
        infoTypeParam.put("enum", List.of("contact", "location"));
        properties.put("infoType", infoTypeParam);
        
        Map<String, Object> nameParam = new HashMap<>();
        nameParam.put("type", "string");
        nameParam.put("description", "Name: For contacts, use the name from nearbyEntities. For locations, use a descriptive name (e.g., 'My Home', 'Diamond Mine')");
        properties.put("name", nameParam);
        
        // Contact-specific fields (only for add/update)
        Map<String, Object> relationshipParam = new HashMap<>();
        relationshipParam.put("type", "string");
        relationshipParam.put("description", "For contacts (add/update only): Relationship type (optional, default: 'neutral'). Values: 'friend', 'enemy', 'neutral', 'teammate', 'stranger', 'acquaintance', 'close_ally', 'rival'");
        properties.put("relationship", relationshipParam);
        
        Map<String, Object> notesParam = new HashMap<>();
        notesParam.put("type", "string");
        notesParam.put("description", "For contacts (add/update only): Optional notes about this contact (e.g., 'Met at spawn', 'Helped me mine')");
        properties.put("notes", notesParam);
        
        Map<String, Object> enmityLevelParam = new HashMap<>();
        enmityLevelParam.put("type", "number");
        enmityLevelParam.put("description", "For contacts (add/update only): Enmity level (optional, default: 0.0). Range: 0.0 to 1.0. Use to set absolute value or update existing");
        properties.put("enmityLevel", enmityLevelParam);
        
        Map<String, Object> friendshipLevelParam = new HashMap<>();
        friendshipLevelParam.put("type", "number");
        friendshipLevelParam.put("description", "For contacts (add/update only): Friendship level (optional, default: 0.0). Range: 0.0 to 1.0. Use to set absolute value or update existing");
        properties.put("friendshipLevel", friendshipLevelParam);
        
        // Location-specific fields (only for add/update)
        Map<String, Object> descriptionParam = new HashMap<>();
        descriptionParam.put("type", "string");
        descriptionParam.put("description", "For locations (add/update only): Description of what this location is or why it's important (required for locations)");
        properties.put("description", descriptionParam);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("action", "infoType", "name"));
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }

    /**
     * Creates the sendMessage tool definition.
     * Allows LLM to send messages to other NPCs or players.
     */
    public static Map<String, Object> getSendMessageTool() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "sendMessage");
        function.put("description", "Send a message to another NPC or player. " +
                "Messages are stored in a mail system and can be read by the recipient later. " +
                "Use this for asynchronous communication (not immediate chat). " +
                "The recipient must be a known contact or visible in nearbyEntities.");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> recipientNameParam = new HashMap<>();
        recipientNameParam.put("type", "string");
        recipientNameParam.put("description", "Name of the recipient (NPC or player) from contacts or nearbyEntities");
        properties.put("recipientName", recipientNameParam);
        
        Map<String, Object> subjectParam = new HashMap<>();
        subjectParam.put("type", "string");
        subjectParam.put("description", "Subject/title of the message");
        properties.put("subject", subjectParam);
        
        Map<String, Object> contentParam = new HashMap<>();
        contentParam.put("type", "string");
        contentParam.put("description", "Content of the message");
        properties.put("content", contentParam);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("recipientName", "subject", "content"));
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }

    /**
     * Creates the manageBook tool definition.
     * Allows LLM to add, update, or remove pages in the shared book.
     * Merged from addOrUpdatePageToBook and removePageFromBook tools.
     */
    public static Map<String, Object> getManageBookTool() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", "manageBook");
        function.put("description", "Manage pages in the shared book. " +
                "The shared book is accessible to all NPCs and contains common information " +
                "(e.g., community rules, shared locations, warnings, announcements). " +
                "Use 'add' or 'update' to add/update a page, or 'remove' to delete a page when information is outdated. " +
                "This is NOT for chatting - use sendMessage for that.");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> actionParam = new HashMap<>();
        actionParam.put("type", "string");
        actionParam.put("description", "Action to perform: 'add' (or 'update' - same as add), or 'remove'");
        actionParam.put("enum", List.of("add", "update", "remove"));
        properties.put("action", actionParam);
        
        Map<String, Object> pageTitleParam = new HashMap<>();
        pageTitleParam.put("type", "string");
        pageTitleParam.put("description", "Title of the page (e.g., 'Community Rules', 'Dangerous Areas', 'Trading Post Location')");
        properties.put("pageTitle", pageTitleParam);
        
        Map<String, Object> contentParam = new HashMap<>();
        contentParam.put("type", "string");
        contentParam.put("description", "Content of the page (required for add/update). This will replace existing content if the page already exists.");
        properties.put("content", contentParam);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("action", "pageTitle"));
        
        function.put("parameters", parameters);
        tool.put("function", function);
        
        return tool;
    }

    /**
     * Returns the list of tools for tool calling.
     * Includes execute_command, memory management, mail, and sharebook tools.
     * Simplified: merged memory tools and book tools, removed readMessage (messages auto-included in context).
     */
    public static List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(getExecuteCommandTool());
        tools.add(getManageMemoryTool());
        tools.add(getSendMessageTool());
        tools.add(getManageBookTool());
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

