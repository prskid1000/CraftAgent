package me.prskid1000.craftagent.action;

/**
 * Generic utility for parsing action commands with quote-aware argument extraction.
 * Handles both single and double quotes, allowing arguments with spaces.
 * 
 * Example: "mail send 'Alice' 'Hello world'" -> ["mail", "send", "Alice", "Hello world"]
 */
public class ActionParser {
    
    /**
     * Parses a string into arguments, respecting quoted strings (both single and double quotes).
     * Strips surrounding quotes from each argument.
     * 
     * @param input The command string to parse
     * @return Array of parsed arguments with quotes stripped
     */
    public static String[] parseQuotedArguments(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new String[0];
        }
        
        java.util.List<String> args = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quoteChar = 0;
        boolean inQuotes = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (!inQuotes && (c == '\'' || c == '"')) {
                // Start of quoted string
                inQuotes = true;
                quoteChar = c;
                // Don't append the opening quote
            } else if (inQuotes && c == quoteChar) {
                // End of quoted string
                inQuotes = false;
                quoteChar = 0;
                // Don't append the closing quote
            } else if (!inQuotes && Character.isWhitespace(c)) {
                // Whitespace outside quotes - end of argument
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                // Regular character
                current.append(c);
            }
        }
        
        // Add last argument if any
        if (current.length() > 0) {
            args.add(current.toString());
        }
        
        return args.toArray(new String[0]);
    }
    
    /**
     * Strips surrounding quotes (single or double) from a string if present.
     * 
     * @param str The string to strip quotes from
     * @return String with quotes removed, or original string if not quoted
     */
    public static String stripQuotes(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        if ((str.startsWith("'") && str.endsWith("'")) || 
            (str.startsWith("\"") && str.endsWith("\""))) {
            return str.substring(1, str.length() - 1);
        }
        
        return str;
    }
    
    /**
     * Checks if a string is wrapped in quotes (single or double).
     * 
     * @param str The string to check
     * @return true if the string is wrapped in quotes
     */
    public static boolean isQuoted(String str) {
        if (str == null || str.length() < 2) {
            return false;
        }
        
        return (str.startsWith("'") && str.endsWith("'")) || 
               (str.startsWith("\"") && str.endsWith("\""));
    }
    
    /**
     * Checks if a specific argument (by index) was quoted in the original string.
     * 
     * @param original The original command string
     * @param parsedArgs The parsed arguments array
     * @param argIndex The index of the argument to check (0-based)
     * @return true if the argument was quoted in the original string
     */
    public static boolean wasArgumentQuoted(String original, String[] parsedArgs, int argIndex) {
        if (original == null || parsedArgs == null || argIndex < 0 || argIndex >= parsedArgs.length) {
            return false;
        }
        
        // Find the position of this argument in the original string
        int currentPos = 0;
        for (int i = 0; i < argIndex; i++) {
            // Find the end of previous argument
            String arg = parsedArgs[i];
            int argPos = original.indexOf(arg, currentPos);
            if (argPos == -1) {
                return false;
            }
            // Move past this argument and any whitespace
            currentPos = argPos + arg.length();
            // Skip whitespace
            while (currentPos < original.length() && Character.isWhitespace(original.charAt(currentPos))) {
                currentPos++;
            }
        }
        
        // Now check if the argument at currentPos starts with a quote
        while (currentPos < original.length() && Character.isWhitespace(original.charAt(currentPos))) {
            currentPos++;
        }
        
        if (currentPos >= original.length()) {
            return false;
        }
        
        char firstChar = original.charAt(currentPos);
        return firstChar == '\'' || firstChar == '"';
    }
}

