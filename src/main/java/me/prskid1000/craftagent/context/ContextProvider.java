package me.prskid1000.craftagent.context;

import java.util.*;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.database.repositories.MessageRepository;
import me.prskid1000.craftagent.database.repositories.SharebookRepository;
import me.prskid1000.craftagent.model.context.*;
import me.prskid1000.craftagent.memory.MemoryManager;
import me.prskid1000.craftagent.util.MCDataUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

/**
 * Generates the context for the LLM requests based on the NPCs world environment.
 */
public class ContextProvider {

	private final ServerPlayerEntity npcEntity;
	private final ChunkManager chunkManager;
	private final int maxNearbyEntities;
	private final BaseConfig baseConfig;
	private WorldContext cachedContext;
	public MemoryManager memoryManager;
	private MessageRepository messageRepository;
	private SharebookRepository sharebookRepository;
	private UUID npcUuid;

	public ContextProvider(ServerPlayerEntity npcEntity, BaseConfig config) {
		this.npcEntity = npcEntity;
		this.maxNearbyEntities = config.getMaxNearbyEntities();
		this.baseConfig = config;
		this.chunkManager = new ChunkManager(npcEntity, config);
		buildContext();
	}
	
	public BaseConfig getBaseConfig() {
		return baseConfig;
	}

	public MessageRepository getMessageRepository() {
		return messageRepository;
	}

	public void setRepositories(MessageRepository messageRepository, SharebookRepository sharebookRepository, UUID npcUuid) {
		this.messageRepository = messageRepository;
		this.sharebookRepository = sharebookRepository;
		this.npcUuid = npcUuid;
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
			contactMap.put("enmityLevel", contact.getEnmityLevel());
			contactMap.put("friendshipLevel", contact.getFriendshipLevel());
			contacts.add(contactMap);
		});
		memory.put("contacts", contacts);
		
		// Add mail (messages) - only unread messages to avoid overwhelming context
		// Auto-mark messages as read when included in context
		if (messageRepository != null && npcUuid != null) {
			java.util.List<java.util.Map<String, Object>> messages = new java.util.ArrayList<>();
			messageRepository.selectByRecipient(npcUuid, 10, true).forEach(msg -> {
				// Mark message as read when included in context
				if (!msg.getRead()) {
					messageRepository.markAsRead(msg.getId());
				}
				
				java.util.Map<String, Object> msgMap = new java.util.HashMap<>();
				msgMap.put("id", msg.getId());
				msgMap.put("senderName", msg.getSenderName());
				msgMap.put("senderType", msg.getSenderType());
				msgMap.put("subject", msg.getSubject());
				msgMap.put("content", msg.getContent());
				msgMap.put("timestamp", msg.getTimestamp());
				msgMap.put("read", true); // Always mark as read in context
				messages.add(msgMap);
			});
			memory.put("mail", messages);
		}
		
		// Add sharebook (shared information accessible to all NPCs)
		if (sharebookRepository != null) {
			java.util.List<java.util.Map<String, Object>> sharebookPages = new java.util.ArrayList<>();
			sharebookRepository.selectAll().forEach(page -> {
				java.util.Map<String, Object> pageMap = new java.util.HashMap<>();
				pageMap.put("pageTitle", page.getPageTitle());
				pageMap.put("content", page.getContent());
				pageMap.put("authorName", page.getAuthorName());
				pageMap.put("timestamp", page.getTimestamp());
				sharebookPages.add(pageMap);
			});
			memory.put("sharebook", sharebookPages);
		}
		
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

	public ServerPlayerEntity getNpcEntity() {
		return npcEntity;
	}
}
