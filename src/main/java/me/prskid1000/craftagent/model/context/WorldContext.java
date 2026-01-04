package me.prskid1000.craftagent.model.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the context of the minecraft world around the NPC
 */
public record WorldContext(
	ContextData.StateData state,
	ContextData.InventoryData inventory,
	List<ContextData.BlockData> nearbyBlocks,
	List<ContextData.EntityData> nearbyEntities,
	Map<String, Object> memoryData,
	ContextData.NavigationData navigation,
	ContextData.LineOfSightData lineOfSight
) {
	public WorldContext(ContextData.StateData state, ContextData.InventoryData inventory, 
	                   List<ContextData.BlockData> nearbyBlocks, List<ContextData.EntityData> nearbyEntities) {
		this(state, inventory, nearbyBlocks, nearbyEntities, null, null, null);
	}
	
	public WorldContext(ContextData.StateData state, ContextData.InventoryData inventory, 
	                   List<ContextData.BlockData> nearbyBlocks, List<ContextData.EntityData> nearbyEntities,
	                   Map<String, Object> memoryData) {
		this(state, inventory, nearbyBlocks, nearbyEntities, memoryData, null, null);
	}
	
	public Optional<ContextData.BlockData> findBlockByType(String type) {
		return nearbyBlocks.stream()
				.filter(block -> block.type().equals(type))
				.findFirst();
	}

	public Optional<ContextData.ItemData> findItemByType(String type) {
		return inventory.getAllItems().stream()
				.filter(item -> item.type().equals(type))
				.findFirst();
	}
}
