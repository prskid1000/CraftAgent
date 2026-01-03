package me.prskid1000.craftagent.util.command;

import java.util.List;
import java.util.Set;

/**
 * Parser for combat commands using generic utilities.
 * Supports: kill [mob], spawn <mob>
 */
public class CombatCommandParser implements CommandParser {
    
    private static final Set<String> GENERIC_MOB_TERMS = Set.of("nearest", "mob");
    private static final String DEFAULT_KILL_COMMAND = "kill @e[type=!player,limit=1,sort=nearest]";
    private static final String CLEAR_MOBS_COMMAND = "kill @e[type=#minecraft:hostile_entities,distance=..20]";
    
    @Override
    public boolean canParse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return false;
        String base = parts.get(0).toLowerCase();
        return base.equals("kill") || base.equals("spawn") || base.equals("clear");
    }
    
    @Override
    public String parse(String command) {
        List<String> parts = ParameterParser.parseCommandParts(command);
        if (parts.isEmpty()) return null;
        
        String baseCommand = parts.get(0).toLowerCase();
        
        if (baseCommand.equals("kill")) {
            if (parts.size() < 2) {
                return DEFAULT_KILL_COMMAND;
            }
            
            String mob = parts.get(1).toLowerCase();
            if (GENERIC_MOB_TERMS.contains(mob)) {
                return DEFAULT_KILL_COMMAND;
            }
            
            String minecraftMob = ResourceMapper.mapMob(mob);
            if (minecraftMob != null) {
                return String.format("kill @e[type=%s,limit=1,sort=nearest]", minecraftMob);
            }
            
            return DEFAULT_KILL_COMMAND;
        }
        
        if (baseCommand.equals("spawn")) {
            if (parts.size() < 2) return null;
            
            String mob = parts.get(1);
            String minecraftMob = ResourceMapper.mapMob(mob);
            if (minecraftMob == null) return null;
            
            return String.format("summon %s ~ ~ ~", minecraftMob);
        }
        
        if (baseCommand.equals("clear") && parts.size() > 1 && parts.get(1).equalsIgnoreCase("mobs")) {
            return CLEAR_MOBS_COMMAND;
        }
        
        return null;
    }
    
    @Override
    public String getCategory() {
        return "Combat";
    }
}

