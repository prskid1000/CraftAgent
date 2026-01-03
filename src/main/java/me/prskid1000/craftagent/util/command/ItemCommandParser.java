package me.prskid1000.craftagent.util.command;

import java.util.List;

/**
 * Parser for item-related commands using generic utilities.
 * Supports: get <item> [amount] or get [amount] <item>
 */
public class ItemCommandParser implements CommandParser {
    
    private static final int DEFAULT_AMOUNT = 32;
    
    @Override
    public boolean canParse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        return !parts.isEmpty() && parts.get(0).equalsIgnoreCase("get");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.size() < 2) return null;
        
        // Use generic utilities to extract item and amount
        Integer amount = ParameterParser.findFirstNumber(parts, 1);
        String item = ParameterParser.reconstructName(parts, 1);
        
        // If first param is a number, item comes after
        if (amount != null && parts.size() > 2) {
            item = ParameterParser.reconstructName(parts, 2);
        }
        
        if (item == null) return null;
        if (amount == null || amount <= 0) amount = DEFAULT_AMOUNT;
        
        // Use ResourceMapper for item name mapping
        String minecraftItem = ResourceMapper.mapItem(item);
        if (minecraftItem == null) return null;
        
        return String.format("give @s %s %d", minecraftItem, amount);
    }
    
    @Override
    public String getCategory() {
        return "Items";
    }
}

