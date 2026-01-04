package me.prskid1000.craftagent.context;

import java.util.*;

import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.database.repositories.MessageRepository;
import me.prskid1000.craftagent.database.repositories.SharebookRepository;
import me.prskid1000.craftagent.model.context.ContextData;
import me.prskid1000.craftagent.model.context.WorldContext;
import me.prskid1000.craftagent.memory.MemoryManager;
import me.prskid1000.craftagent.util.LogUtil;
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

	public SharebookRepository getSharebookRepository() {
		return sharebookRepository;
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
			try {
				Map<String, Object> memoryData = null;
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
				this.cachedContext = context;
				return context;
			} catch (Exception e) {
				LogUtil.error("Error building NPC context", e);
				throw new RuntimeException(e);
			}
		}
	}
	
	private java.util.Map<String, Object> buildMemoryData() {
		java.util.Map<String, Object> memory = new java.util.HashMap<>();
		
		// Add private book pages (private memory for this NPC)
		java.util.List<java.util.Map<String, Object>> privatePages = new java.util.ArrayList<>();
		memoryManager.getPages().forEach(page -> {
			java.util.Map<String, Object> pageMap = new java.util.HashMap<>();
			pageMap.put("pageTitle", page.getPageTitle());
			pageMap.put("content", page.getContent());
			pageMap.put("timestamp", page.getTimestamp());
			privatePages.add(pageMap);
		});
		memory.put("privateBook", privatePages);
		
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
				pageMap.put("authorUuid", page.getAuthorUuid());
				pageMap.put("timestamp", page.getTimestamp());
				sharebookPages.add(pageMap);
			});
			memory.put("sharebook", sharebookPages);
		}
		
		return memory;
	}

	private ContextData.StateData getNpcState() {
		return new ContextData.StateData(
				npcEntity.getBlockPos(),
				npcEntity.getHealth(),
				npcEntity.getHungerManager().getFoodLevel(),
				MCDataUtil.getBiome(npcEntity));
	}

	private ContextData.InventoryData getInventoryState() {
		PlayerInventory inventory = npcEntity.getInventory();
		return new ContextData.InventoryData(
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

	private List<ContextData.ItemData> getItemsInRange(PlayerInventory inventory, int start, int end) {
		List<ContextData.ItemData> items = new ArrayList<>();
		for (int i = start; i <= end; i++) {
			ItemStack stack = inventory.getStack(i);
			addItemData(stack, items, i);
		}
		return items;
	}

	private void addItemData(ItemStack stack, List<ContextData.ItemData> items, int slot) {
		if (!stack.isEmpty()) {
			items.add(new ContextData.ItemData(getBlockName(stack), stack.getCount(), slot));
		}
	}

	private String getBlockName(ItemStack stack) {
		String translationKey = stack.getItem().getTranslationKey();
		return translationKey.substring(translationKey.lastIndexOf(".") + 1);
	}

	private List<ContextData.EntityData> getNearbyEntities() {
		List<ContextData.EntityData> nearbyEntities = new ArrayList<>();
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
				nearbyEntities.add(new ContextData.EntityData(entity.getId(), entity.getName().getString(), entity.isPlayer()))
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
