package me.prskid1000.craftagent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.prskid1000.craftagent.model.context.ContextData;
import me.prskid1000.craftagent.model.context.WorldContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Formats prompts with structured input: text prompt + JSON context data.
 * Separates instructions from structured data for better LLM parsing.
 */
public class StructuredInputFormatter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private StructuredInputFormatter() {}

    /**
     * Creates a structured input message with text prompt and JSON context.
     * Format: Text instruction + JSON context object
     * 
     * @param prompt The text instruction/prompt
     * @param worldContext The structured world context data
     * @return Formatted message with text and JSON
     */
    public static String formatStructured(String prompt, WorldContext worldContext) {
        try {
            // Create JSON context object
            Map<String, Object> context = new HashMap<>();
            context.put("state", createStateMap(worldContext.state()));
            context.put("inventory", createInventoryMap(worldContext.inventory()));
            // Data is already limited by ChunkManager and ContextProvider based on config
            context.put("nearbyBlocks", worldContext.nearbyBlocks().stream()
                    .map(block -> Map.of(
                            "type", block.type(),
                            "position", Map.of(
                                    "x", block.position().getX(),
                                    "y", block.position().getY(),
                                    "z", block.position().getZ()
                            ),
                            "mineLevel", block.mineLevel(),
                            "toolNeeded", block.toolNeeded()
                    ))
                    .toList());
            context.put("nearbyEntities", worldContext.nearbyEntities().stream()
                    .map(entity -> Map.of(
                            "id", entity.id(),
                            "name", entity.name(),
                            "isPlayer", entity.isPlayer()
                    ))
                    .toList());
            
            // Add memory data if available
            if (worldContext.memoryData() != null) {
                context.put("memory", worldContext.memoryData());
            }
            
            // Add navigation data if available
            if (worldContext.navigation() != null) {
                Map<String, Object> navMap = new HashMap<>();
                navMap.put("state", worldContext.navigation().state());
                navMap.put("stateDescription", worldContext.navigation().stateDescription());
                navMap.put("timeInCurrentState", worldContext.navigation().timeInCurrentState());
                if (worldContext.navigation().destination() != null) {
                    navMap.put("destination", Map.of(
                            "x", worldContext.navigation().destination().getX(),
                            "y", worldContext.navigation().destination().getY(),
                            "z", worldContext.navigation().destination().getZ()
                    ));
                }
                context.put("navigation", navMap);
            }
            
            // Add line of sight data if available
            if (worldContext.lineOfSight() != null) {
                Map<String, Object> losMap = new HashMap<>();
                
                // Items in line of sight
                losMap.put("items", worldContext.lineOfSight().items().stream()
                        .map(item -> Map.of(
                                "type", item.type(),
                                "count", item.count(),
                                "distance", item.distance(),
                                "position", Map.of(
                                        "x", item.position().getX(),
                                        "y", item.position().getY(),
                                        "z", item.position().getZ()
                                )
                        ))
                        .toList());
                
                // Entities in line of sight
                losMap.put("entities", worldContext.lineOfSight().entities().stream()
                        .map(entity -> Map.of(
                                "id", entity.id(),
                                "name", entity.name(),
                                "isPlayer", entity.isPlayer()
                        ))
                        .toList());
                
                // Target block (where NPC is looking)
                if (worldContext.lineOfSight().targetBlock() != null) {
                    var targetBlock = worldContext.lineOfSight().targetBlock();
                    losMap.put("targetBlock", Map.of(
                            "type", targetBlock.type(),
                            "position", Map.of(
                                    "x", targetBlock.position().getX(),
                                    "y", targetBlock.position().getY(),
                                    "z", targetBlock.position().getZ()
                            ),
                            "mineLevel", targetBlock.mineLevel(),
                            "toolNeeded", targetBlock.toolNeeded()
                    ));
                }
                
                // Visible blocks
                losMap.put("visibleBlocks", worldContext.lineOfSight().visibleBlocks().stream()
                        .map(block -> Map.of(
                                "type", block.type(),
                                "position", Map.of(
                                        "x", block.position().getX(),
                                        "y", block.position().getY(),
                                        "z", block.position().getZ()
                                ),
                                "mineLevel", block.mineLevel(),
                                "toolNeeded", block.toolNeeded()
                        ))
                        .toList());
                
                context.put("lineOfSight", losMap);
            }
            
            // Add action state data if available
            if (worldContext.actionState() != null) {
                Map<String, Object> actionStateMap = new HashMap<>();
                actionStateMap.put("actionType", worldContext.actionState().actionType());
                actionStateMap.put("actionDescription", worldContext.actionState().actionDescription());
                actionStateMap.put("timeInCurrentAction", worldContext.actionState().timeInCurrentAction());
                if (worldContext.actionState().actionData() != null) {
                    actionStateMap.put("actionData", worldContext.actionState().actionData());
                }
                context.put("actionState", actionStateMap);
            }

            // Convert context to JSON string
            String contextJson = objectMapper.writeValueAsString(context);

            // Combine text prompt with JSON context in a structured format
            // Format: Text instruction followed by JSON context in a clear structure
            return String.format("""
                %s
                
                === CONTEXT DATA (JSON) ===
                %s
                === END CONTEXT ===
                """, prompt, contextJson);
        } catch (Exception e) {
            // If JSON serialization fails, throw exception - no fallback
            throw new RuntimeException("Failed to format structured input: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> createStateMap(ContextData.StateData state) {
        Map<String, Object> stateMap = new HashMap<>();
        stateMap.put("position", Map.of(
                "x", state.position().getX(),
                "y", state.position().getY(),
                "z", state.position().getZ()
        ));
        stateMap.put("health", state.health());
        stateMap.put("food", state.food());
        stateMap.put("biome", state.biome());
        return stateMap;
    }

    private static Map<String, Object> createInventoryMap(ContextData.InventoryData inventory) {
        Map<String, Object> invMap = new HashMap<>();
        invMap.put("hotbar", inventory.hotbar().stream()
                .map(item -> Map.of("type", item.type(), "count", item.count(), "slot", item.slot()))
                .toList());
        invMap.put("mainInventory", inventory.mainInventory().stream()
                .map(item -> Map.of("type", item.type(), "count", item.count(), "slot", item.slot()))
                .toList());
        invMap.put("armor", inventory.armor().stream()
                .map(item -> Map.of("type", item.type(), "count", item.count(), "slot", item.slot()))
                .toList());
        invMap.put("offHand", inventory.offHand().stream()
                .map(item -> Map.of("type", item.type(), "count", item.count(), "slot", item.slot()))
                .toList());
        return invMap;
    }
}

