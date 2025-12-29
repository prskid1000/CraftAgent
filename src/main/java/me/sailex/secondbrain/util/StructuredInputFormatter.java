package me.sailex.secondbrain.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.sailex.secondbrain.model.context.WorldContext;

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
            // Fallback to old format if JSON serialization fails
            return PromptFormatter.format(prompt, worldContext);
        }
    }

    private static Map<String, Object> createStateMap(me.sailex.secondbrain.model.context.StateData state) {
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

    private static Map<String, Object> createInventoryMap(me.sailex.secondbrain.model.context.InventoryData inventory) {
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

