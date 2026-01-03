package me.prskid1000.craftagent.util.command;

/**
 * Base interface for command parsers.
 * Each parser handles a specific category of commands.
 */
public interface CommandParser {
    /**
     * Checks if this parser can handle the given command.
     * @param command The normalized command string (lowercase, trimmed)
     * @return true if this parser can handle the command
     */
    boolean canParse(String command);
    
    /**
     * Parses the command and returns the mapped Minecraft command or tool action.
     * @param command The normalized command string
     * @return The mapped command string, or null if parsing fails
     */
    String parse(String command);
    
    /**
     * Gets the command category name for this parser.
     * @return Category name (e.g., "Movement", "Mining", "Crafting")
     */
    String getCategory();
}

