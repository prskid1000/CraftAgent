package me.prskid1000.craftagent.util.command;

import java.util.List;

/**
 * Parser for crafting commands using generic utilities.
 * Supports: craft <item> [amount]
 */
public class CraftingCommandParser implements CommandParser {
    
    private static final int DEFAULT_AMOUNT = 1;
    
    @Override
    public boolean canParse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        return !parts.isEmpty() && parts.get(0).equalsIgnoreCase("craft");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.size() < 2) return null;
        
        // Use generic utilities to extract item and amount
        Integer amount = ParameterParser.findFirstNumber(parts, parts.size() - 1);
        String item = ParameterParser.reconstructName(parts, 1);
        
        // If last part is a number, item is everything before it
        if (amount != null && parts.size() > 2 && item != null) {
            // Remove the number from item name if it was included
            String lastPartStr = parts.get(parts.size() - 1);
            if (item.endsWith(" " + lastPartStr)) {
                item = item.substring(0, item.length() - lastPartStr.length() - 1);
            }
        }
        
        if (item == null || item.isEmpty()) return null;
        if (amount == null || amount <= 0) amount = DEFAULT_AMOUNT;
        
        // Use ResourceMapper for craft item mapping
        String minecraftItem = ResourceMapper.mapCraftItem(item);
        if (minecraftItem == null) return null;
        
        return String.format("give @s %s %d", minecraftItem, amount);
    }
    
    @Override
    public String getCategory() {
        return "Crafting";
    }
}

