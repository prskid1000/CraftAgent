package me.prskid1000.craftagent.model.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the context of the minecraft world around the NPC
 */
public record WorldContext(
	StateData state,
	InventoryData inventory,
	List<BlockData> nearbyBlocks,
	List<EntityData> nearbyEntities,
	Map<String, Object> memoryData
) {
	public WorldContext(StateData state, InventoryData inventory, List<BlockData> nearbyBlocks, List<EntityData> nearbyEntities) {
		this(state, inventory, nearbyBlocks, nearbyEntities, null);
	}
	public Optional<BlockData> findBlockByType(String type) {
		return nearbyBlocks.stream()
				.filter(block -> block.type().equals(type))
				.findFirst();
	}

	public Optional<ItemData> findItemByType(String type) {
		return inventory.getAllItems().stream()
				.filter(item -> item.type().equals(type))
				.findFirst();
	}
}
