package me.prskid1000.craftagent.action;

import java.util.List;

/**
 * Interface for action handlers to provide their command syntax.
 * This allows automatic generation of action documentation.
 */
public interface ActionSyntaxProvider {
    
    /**
     * Returns a list of action syntax strings that this handler supports.
     * Format: "command <arg1> '<arg2>'"
     * 
     * @return List of action syntax strings
     */
    List<String> getActionSyntax();
}

