package me.prskid1000.craftagent.util.command;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Generic parameter parser utility for extracting parameters from command strings.
 * Supports various parameter formats and patterns.
 */
public class ParameterParser {
    
    /**
     * Extracts a named parameter from command string.
     * Format: "command param1 param2 key:value key2:value2"
     * Returns the value for the given key, or null if not found.
     */
    public static String extractNamedParameter(String command, String key) {
        String keyPrefix = key + ":";
        int keyIndex = command.indexOf(keyPrefix);
        if (keyIndex < 0) return null;
        
        int valueStart = keyIndex + keyPrefix.length();
        int valueEnd = command.indexOf(" ", valueStart);
        if (valueEnd < 0) {
            valueEnd = command.length();
        }
        
        String value = command.substring(valueStart, valueEnd).trim();
        return value.isEmpty() ? null : value;
    }
    
    /**
     * Extracts all named parameters (key:value pairs) from a command string.
     */
    public static Map<String, String> extractAllNamedParameters(String command) {
        Map<String, String> params = new HashMap<>();
        Pattern pattern = Pattern.compile("(\\w+):([^\\s]+)");
        java.util.regex.Matcher matcher = pattern.matcher(command);
        
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }
        
        return params;
    }
    
    /**
     * Parses command into parts, handling quoted strings.
     */
    public static List<String> parseCommandParts(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : command.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts;
    }
    
    /**
     * Tries to parse an integer from a string, returns null if not a number.
     */
    public static Integer tryParseInt(String str) {
        if (str == null) return null;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Finds the first number in a list of strings.
     */
    public static Integer findFirstNumber(List<String> parts, int startIndex) {
        for (int i = startIndex; i < parts.size(); i++) {
            Integer num = tryParseInt(parts.get(i));
            if (num != null) {
                return num;
            }
        }
        return null;
    }
    
    /**
     * Finds the first non-number string in a list.
     */
    public static String findFirstNonNumber(List<String> parts, int startIndex) {
        for (int i = startIndex; i < parts.size(); i++) {
            String part = parts.get(i);
            if (tryParseInt(part) == null && !part.contains(":")) {
                return part;
            }
        }
        return null;
    }
    
    /**
     * Reconstructs a multi-word item/block name from parts.
     * Stops at numbers or named parameters.
     */
    public static String reconstructName(List<String> parts, int startIndex) {
        StringBuilder name = new StringBuilder();
        for (int i = startIndex; i < parts.size(); i++) {
            String part = parts.get(i);
            if (part.contains(":") || tryParseInt(part) != null) {
                break;
            }
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(part);
        }
        return name.toString();
    }
    
    /**
     * Escapes special characters in parameter values for pipe-delimited format.
     */
    public static String escapeParam(String value) {
        if (value == null) return "";
        return value.replace("|", "\\|").replace(":", "\\:");
    }
    
    /**
     * Unescapes parameter values from pipe-delimited format.
     */
    public static String unescapeParam(String value) {
        if (value == null) return "";
        return value.replace("\\|", "|").replace("\\:", ":");
    }
}

