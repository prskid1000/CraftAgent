package me.prskid1000.craftagent;

import me.prskid1000.craftagent.auth.PlayerAuthorizer;
import me.prskid1000.craftagent.commands.CommandManager;
import me.prskid1000.craftagent.common.NPCFactory;
import lombok.Getter;
import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.ConfigProvider;
import me.prskid1000.craftagent.database.SqliteClient;
import me.prskid1000.craftagent.database.repositories.RepositoryFactory;
import me.prskid1000.craftagent.database.resources.ResourceProvider;
import me.prskid1000.craftagent.listener.EventListenerRegisterer;
import me.prskid1000.craftagent.networking.NetworkHandler;
import me.prskid1000.craftagent.util.LogUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Main class for the CraftAgent mod.
 */
@Getter
public class CraftAgent implements ModInitializer {

	public static final String MOD_ID = "craftagent";
	private boolean isFirstPlayerJoins = true;

	@Override
	public void onInitialize() {
        ConfigProvider configProvider = new ConfigProvider();

        SqliteClient sqlite = new SqliteClient();
        RepositoryFactory repositoryFactory = new RepositoryFactory(sqlite);

        ResourceProvider resourceProvider = new ResourceProvider(
            repositoryFactory.getConversationRepository(),
            repositoryFactory.getLocationMemoryRepository(),
            repositoryFactory.getContactRepository(),
            repositoryFactory.getMessageRepository(),
            repositoryFactory.getSharebookRepository()
        );

        NPCFactory npcFactory = new NPCFactory(
            configProvider,
            repositoryFactory.getLocationMemoryRepository(),
            repositoryFactory.getContactRepository(),
            repositoryFactory.getMessageRepository(),
            repositoryFactory.getSharebookRepository()
        );
        NPCService npcService = new NPCService(npcFactory, configProvider, resourceProvider);

        PlayerAuthorizer authorizer = new PlayerAuthorizer();

        NetworkHandler networkManager = new NetworkHandler(configProvider, npcService, authorizer);
        networkManager.registerPacketReceiver();

        EventListenerRegisterer eventListenerRegisterer = new EventListenerRegisterer(npcService);
        eventListenerRegisterer.register();

        CommandManager commandManager = new CommandManager(npcService, configProvider, networkManager);
        commandManager.registerAll();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LogUtil.initialize(server, configProvider);
            repositoryFactory.initRepositories();
            resourceProvider.loadResources(configProvider.getUuidsOfNpcs());
            npcService.init(server);
        });

        onStop(npcService, configProvider, sqlite, resourceProvider);
    }

	private void onStop(
        NPCService npcService,
        ConfigProvider configProvider,
        SqliteClient sqlite,
        ResourceProvider resourceProvider
	) {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            npcService.shutdownNPCs(server);
            resourceProvider.saveResources();
            sqlite.closeConnection();
            configProvider.saveAll();
            isFirstPlayerJoins = true;
        });
	}
}
