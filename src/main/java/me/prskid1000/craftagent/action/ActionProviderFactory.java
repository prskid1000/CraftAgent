package me.prskid1000.craftagent.action;

import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.context.ContextProvider;
import me.prskid1000.craftagent.database.repositories.MessageRepository;
import me.prskid1000.craftagent.database.repositories.SharebookRepository;
import me.prskid1000.craftagent.memory.MemoryManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Factory class for creating ActionProvider with all action handlers.
 * Centralizes handler creation logic to avoid duplication.
 * 
 * To add a new handler:
 * 1. Create the handler class implementing ActionSyntaxProvider
 * 2. Add handler creation in createAllHandlers() method
 * 3. Add handler to ActionProvider constructor call
 * 4. Add handler.getStaticActionSyntax() to ActionProvider.getAllStaticActionSyntax() method
 *    (This ensures command suggestions work automatically)
 */
public class ActionProviderFactory {
    
    /**
     * Creates an ActionProvider with all action handlers for the given NPC.
     * 
     * @param npcEntity The NPC entity
     * @param contextProvider The context provider for the NPC
     * @param configUuid The NPC config UUID
     * @param npcName The NPC name
     * @param memoryManager The memory manager
     * @param messageRepository The message repository
     * @param sharebookRepository The sharebook repository
     * @param npcService The NPC service
     * @param baseConfig The base config
     * @return ActionProvider with all handlers initialized
     */
    public static ActionProvider create(
            ServerPlayerEntity npcEntity,
            ContextProvider contextProvider,
            UUID configUuid,
            String npcName,
            MemoryManager memoryManager,
            MessageRepository messageRepository,
            SharebookRepository sharebookRepository,
            NPCService npcService,
            BaseConfig baseConfig) {
        
        // Create all handlers
        HandlerSet handlers = createAllHandlers(
                npcEntity,
                contextProvider,
                configUuid,
                npcName,
                memoryManager,
                messageRepository,
                sharebookRepository,
                npcService,
                baseConfig
        );
        
        // Create and return ActionProvider with all handlers
        return new ActionProvider(
                handlers.memoryHandler,
                handlers.communicationHandler,
                handlers.navigationHandler,
                handlers.miningHandler,
                handlers.buildingHandler,
                handlers.craftingHandler,
                handlers.huntingHandler,
                handlers.farmingHandler,
                handlers.fishingHandler,
                handlers.combatHandler
        );
    }
    
    /**
     * Creates all action handlers for an NPC.
     * Add new handlers here when implementing new action types.
     */
    private static HandlerSet createAllHandlers(
            ServerPlayerEntity npcEntity,
            ContextProvider contextProvider,
            UUID configUuid,
            String npcName,
            MemoryManager memoryManager,
            MessageRepository messageRepository,
            SharebookRepository sharebookRepository,
            NPCService npcService,
            BaseConfig baseConfig) {
        
        // Memory handler (required - handles sharedbook/privatebook)
        MemoryActionHandler memoryHandler = new MemoryActionHandler(
                memoryManager,
                sharebookRepository,
                configUuid,
                npcName,
                baseConfig
        );
        
        // Communication handler (required - handles mail)
        CommunicationActionHandler communicationHandler = new CommunicationActionHandler(
                messageRepository,
                npcService,
                configUuid,
                npcName,
                baseConfig
        );
        
        // Navigation handler (optional - handles travel)
        NavigationActionHandler navigationHandler = new NavigationActionHandler(
                npcEntity,
                contextProvider,
                baseConfig
        );
        
        // Mining handler (optional - handles mine)
        MiningActionHandler miningHandler = new MiningActionHandler(
                npcEntity,
                contextProvider,
                baseConfig
        );
        
        // Building handler (optional - handles build/place)
        BuildingActionHandler buildingHandler = new BuildingActionHandler(
                npcEntity,
                contextProvider,
                baseConfig
        );
        
        // Crafting handler (optional - handles craft)
        CraftingActionHandler craftingHandler = new CraftingActionHandler(
                npcEntity,
                contextProvider,
                baseConfig
        );
        
        // Hunting handler (optional - handles hunt)
        HuntingActionHandler huntingHandler = new HuntingActionHandler(
                npcEntity,
                contextProvider,
                baseConfig
        );
        
        // Farming handler (optional - handles farm)
        FarmingActionHandler farmingHandler = new FarmingActionHandler(
                npcEntity,
                contextProvider,
                baseConfig
        );
        
        // Fishing handler (optional - handles fish)
        FishingActionHandler fishingHandler = new FishingActionHandler(
                npcEntity,
                contextProvider,
                baseConfig
        );
        
        // Combat handler (optional - handles attack/defend)
        CombatActionHandler combatHandler = new CombatActionHandler(
                npcEntity,
                contextProvider,
                baseConfig
        );
        
        return new HandlerSet(
                memoryHandler,
                communicationHandler,
                navigationHandler,
                miningHandler,
                buildingHandler,
                craftingHandler,
                huntingHandler,
                farmingHandler,
                fishingHandler,
                combatHandler
        );
    }
    
    /**
     * Internal class to hold all handlers.
     * Makes it easier to pass handlers around and add new ones.
     */
    private static class HandlerSet {
        final MemoryActionHandler memoryHandler;
        final CommunicationActionHandler communicationHandler;
        final NavigationActionHandler navigationHandler;
        final MiningActionHandler miningHandler;
        final BuildingActionHandler buildingHandler;
        final CraftingActionHandler craftingHandler;
        final HuntingActionHandler huntingHandler;
        final FarmingActionHandler farmingHandler;
        final FishingActionHandler fishingHandler;
        final CombatActionHandler combatHandler;
        
        HandlerSet(
                MemoryActionHandler memoryHandler,
                CommunicationActionHandler communicationHandler,
                NavigationActionHandler navigationHandler,
                MiningActionHandler miningHandler,
                BuildingActionHandler buildingHandler,
                CraftingActionHandler craftingHandler,
                HuntingActionHandler huntingHandler,
                FarmingActionHandler farmingHandler,
                FishingActionHandler fishingHandler,
                CombatActionHandler combatHandler) {
            this.memoryHandler = memoryHandler;
            this.communicationHandler = communicationHandler;
            this.navigationHandler = navigationHandler;
            this.miningHandler = miningHandler;
            this.buildingHandler = buildingHandler;
            this.craftingHandler = craftingHandler;
            this.huntingHandler = huntingHandler;
            this.farmingHandler = farmingHandler;
            this.fishingHandler = fishingHandler;
            this.combatHandler = combatHandler;
        }
    }
}

