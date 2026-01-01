package me.prskid1000.craftagent.context;

import java.util.*;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.model.context.*;
import me.prskid1000.craftagent.memory.MemoryManager;
import me.prskid1000.craftagent.util.MCDataUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Generates the context for the LLM requests based on the NPCs world environment.
 */
public class ContextProvider {

	private final ServerPlayerEntity npcEntity;
	private final ChunkManager chunkManager;
	private final int maxNearbyEntities;
	private WorldContext cachedContext;
	public MemoryManager memoryManager;

	public ContextProvider(ServerPlayerEntity npcEntity, BaseConfig config) {
		this.npcEntity = npcEntity;
		this.maxNearbyEntities = config.getMaxNearbyEntities();
		this.chunkManager = new ChunkManager(npcEntity, config);
		buildContext();
	}

	/**
	 * Builds a context of the NPC entity world environment.
	 */
	public WorldContext buildContext() {
		synchronized (this) {
			java.util.Map<String, Object> memoryData = null;
			if (memoryManager != null) {
				memoryData = buildMemoryData();
			}
			
			WorldContext context = new WorldContext(
					getNpcState(),
					getInventoryState(),
					chunkManager.getNearbyBlocks(),
					getNearbyEntities(),
					memoryData
			);
//			chunkManager.getNearbyBlocks().forEach(blockData -> LogUtil.debugInChat(blockData.toString()));
			this.cachedContext = context;
			return context;
		}
	}
	
	private java.util.Map<String, Object> buildMemoryData() {
		java.util.Map<String, Object> memory = new java.util.HashMap<>();
		
		// Add locations
		java.util.List<java.util.Map<String, Object>> locations = new java.util.ArrayList<>();
		memoryManager.getLocations().forEach(loc -> {
			java.util.Map<String, Object> locMap = new java.util.HashMap<>();
			locMap.put("name", loc.getName());
			locMap.put("x", loc.getX());
			locMap.put("y", loc.getY());
			locMap.put("z", loc.getZ());
			locMap.put("description", loc.getDescription());
			locMap.put("lastVisited", loc.getTimestamp());
			locations.add(locMap);
		});
		memory.put("locations", locations);
		
		// Add contacts
		java.util.List<java.util.Map<String, Object>> contacts = new java.util.ArrayList<>();
		memoryManager.getContacts().forEach(contact -> {
			java.util.Map<String, Object> contactMap = new java.util.HashMap<>();
			contactMap.put("name", contact.getContactName());
			contactMap.put("type", contact.getContactType());
			contactMap.put("relationship", contact.getRelationship());
			contactMap.put("notes", contact.getNotes());
			contactMap.put("lastSeen", contact.getLastSeen());
			contacts.add(contactMap);
		});
		memory.put("contacts", contacts);
		
		return memory;
	}

	private StateData getNpcState() {
		return new StateData(
				npcEntity.getBlockPos(),
				npcEntity.getHealth(),
				npcEntity.getHungerManager().getFoodLevel(),
				MCDataUtil.getBiome(npcEntity));
	}

	private InventoryData getInventoryState() {
		PlayerInventory inventory = npcEntity.getInventory();
		return new InventoryData(
				// armour
				getItemsInRange(inventory, 36, 39),
				// main inventory
				getItemsInRange(inventory, 9, 35),
				// hotbar
				getItemsInRange(inventory, 0, 8),
				// off-hand
				getItemsInRange(inventory, 40, 40)
		);
	}

	private List<ItemData> getItemsInRange(PlayerInventory inventory, int start, int end) {
		List<ItemData> items = new ArrayList<>();
		for (int i = start; i <= end; i++) {
			ItemStack stack = inventory.getStack(i);
			addItemData(stack, items, i);
		}
		return items;
	}

	private void addItemData(ItemStack stack, List<ItemData> items, int slot) {
		if (!stack.isEmpty()) {
			items.add(new ItemData(getBlockName(stack), stack.getCount(), slot));
		}
	}

	private String getBlockName(ItemStack stack) {
		String translationKey = stack.getItem().getTranslationKey();
		return translationKey.substring(translationKey.lastIndexOf(".") + 1);
	}

	private List<EntityData> getNearbyEntities() {
		List<EntityData> nearbyEntities = new ArrayList<>();
		List<Entity> entities = MCDataUtil.getNearbyEntities(npcEntity);
		
		// Limit to maxNearbyEntities (prioritize players, then by distance)
		List<Entity> sortedEntities = new ArrayList<>(entities);
		sortedEntities.sort((a, b) -> {
			// Prioritize players
			if (a.isPlayer() && !b.isPlayer()) return -1;
			if (!a.isPlayer() && b.isPlayer()) return 1;
			// Then by distance
			double distA = npcEntity.getBlockPos().getSquaredDistance(a.getBlockPos());
			double distB = npcEntity.getBlockPos().getSquaredDistance(b.getBlockPos());
			return Double.compare(distA, distB);
		});
		
		sortedEntities.stream()
			.limit(maxNearbyEntities)
			.forEach(entity ->
				nearbyEntities.add(new EntityData(entity.getId(), entity.getName().getString(), entity.isPlayer()))
			);
		return nearbyEntities;
	}

	public ChunkManager getChunkManager() {
		return chunkManager;
	}

	public WorldContext getCachedContext() {
		return cachedContext;
	}
}
