package me.prskid1000.craftagent.util.command;

import java.util.*;

/**
 * Parser for tool actions (memory, messaging, book management).
 * Supports parameter extraction from commands.
 */
public class ToolCommandParser implements CommandParser {
    
    @Override
    public boolean canParse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return false;
        String base = parts.get(0).toLowerCase();
        return base.equals("save") || base.equals("remember") || base.equals("forget") ||
               base.equals("add") || base.equals("update") || base.equals("remove") ||
               base.equals("send");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return null;
        
        String baseCommand = parts.get(0).toLowerCase();
        String normalized = command.toLowerCase();
        
        // Handle "save location <name> [description:...]"
        if (baseCommand.equals("save") || baseCommand.equals("remember")) {
            if (parts.size() > 1 && parts.get(1).equalsIgnoreCase("location")) {
                String name = parts.size() > 2 ? parts.get(2) : "Location";
                String description = ParameterParser.extractNamedParameter(normalized, "description");
                return String.format("manageMemory:add:location|name:%s|description:%s",
                    ParameterParser.escapeParam(name), ParameterParser.escapeParam(description != null ? description : ""));
            }
        }
        
        // Handle "forget location <name>"
        if (baseCommand.equals("forget")) {
            if (parts.size() > 1 && parts.get(1).equalsIgnoreCase("location")) {
                String name = parts.size() > 2 ? parts.get(2) : null;
                if (name == null) return null;
                return String.format("manageMemory:remove:location|name:%s", ParameterParser.escapeParam(name));
            }
        }
        
        // Handle "add contact <name> [relationship:...] [notes:...]"
        // Handle "update contact <name> [relationship:...] [notes:...]"
        if (baseCommand.equals("add") || baseCommand.equals("update")) {
            if (parts.size() > 1 && parts.get(1).equalsIgnoreCase("contact")) {
                String name = parts.size() > 2 ? parts.get(2) : null;
                String relationship = ParameterParser.extractNamedParameter(normalized, "relationship");
                String notes = ParameterParser.extractNamedParameter(normalized, "notes");
                if (name == null) return null;
                return String.format("manageMemory:%s:contact|name:%s|relationship:%s|notes:%s",
                    baseCommand, ParameterParser.escapeParam(name),
                    ParameterParser.escapeParam(relationship != null ? relationship : "neutral"),
                    ParameterParser.escapeParam(notes != null ? notes : ""));
            }
        }
        
        // Handle "remove contact <name>"
        if (baseCommand.equals("remove")) {
            if (parts.size() > 1 && parts.get(1).equalsIgnoreCase("contact")) {
                String name = parts.size() > 2 ? parts.get(2) : null;
                if (name == null) return null;
                return String.format("manageMemory:remove:contact|name:%s", ParameterParser.escapeParam(name));
            }
        }
        
        // Handle "send mail <recipient> <subject> [content:...]"
        // Handle "send message <recipient> <subject> [content:...]"
        if (baseCommand.equals("send")) {
            if (parts.size() > 1 && (parts.get(1).equalsIgnoreCase("mail") || parts.get(1).equalsIgnoreCase("message"))) {
                String recipient = parts.size() > 2 ? parts.get(2) : null;
                String subject = parts.size() > 3 ? parts.get(3) : null;
                String content = ParameterParser.extractNamedParameter(normalized, "content");
                if (recipient == null || subject == null) return null;
                return String.format("sendMessage|recipient:%s|subject:%s|content:%s",
                    ParameterParser.escapeParam(recipient), ParameterParser.escapeParam(subject),
                    ParameterParser.escapeParam(content != null ? content : ""));
            }
        }
        
        // Handle "add book page <title> [content:...]"
        // Handle "update book page <title> [content:...]"
        if (baseCommand.equals("add") || baseCommand.equals("update")) {
            if (parts.size() >= 3 && parts.get(1).equalsIgnoreCase("book") && parts.get(2).equalsIgnoreCase("page")) {
                String title = parts.size() > 3 ? parts.get(3) : null;
                String content = ParameterParser.extractNamedParameter(normalized, "content");
                if (title == null) return null;
                return String.format("manageBook:%s|title:%s|content:%s",
                    baseCommand, ParameterParser.escapeParam(title),
                    ParameterParser.escapeParam(content != null ? content : ""));
            }
        }
        
        // Handle "remove book page <title>"
        if (baseCommand.equals("remove")) {
            if (parts.size() >= 3 && parts.get(1).equalsIgnoreCase("book") && parts.get(2).equalsIgnoreCase("page")) {
                String title = parts.size() > 3 ? parts.get(3) : null;
                if (title == null) return null;
                return String.format("manageBook:remove|title:%s", ParameterParser.escapeParam(title));
            }
        }
        
        return null;
    }
    
    @Override
    public String getCategory() {
        return "Tools";
    }
    
    // Use generic ParameterParser utilities instead of local methods
}

