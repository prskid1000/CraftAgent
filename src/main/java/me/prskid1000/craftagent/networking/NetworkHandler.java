package me.prskid1000.craftagent.networking;

import io.wispforest.owo.network.OwoNetChannel;
import me.sailex.altoclef.multiversion.EntityVer;
import me.prskid1000.craftagent.auth.PlayerAuthorizer;
import me.prskid1000.craftagent.callback.STTCallback;
import me.prskid1000.craftagent.common.NPCService;
import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.config.ConfigProvider;
import me.prskid1000.craftagent.config.NPCConfig;
import me.prskid1000.craftagent.networking.packet.*;
import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.Identifier;

import java.util.UUID;

import static me.prskid1000.craftagent.CraftAgent.MOD_ID;


/**
 * Serverside NetworkHandler that sends and receives packets to/from the client
 */
public class NetworkHandler {

    public static final Identifier CONFIG_CHANNEL_ID = Identifier.of(MOD_ID, "config-channel");
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(CONFIG_CHANNEL_ID);
    private final ConfigProvider configProvider;
    private final NPCService npcService;
    private final PlayerAuthorizer authorizer;

    public NetworkHandler(ConfigProvider configProvider, NPCService npcService, PlayerAuthorizer authorizer) {
        this.configProvider = configProvider;
        this.npcService = npcService;
        this.authorizer = authorizer;
    }

    /**
     * Registers endecs for serialization and packet receivers serverside.
     */
    public void registerPacketReceiver() {
        registerEndecs();

        registerUpdateBaseConfig();
        registerUpdateNpcConfig();
        registerAddNpc();
        registerDeleteNpc();
        registerStartStopSTT();

        CHANNEL.registerClientboundDeferred(ConfigPacket.class);
    }

    /**
     * Sends config packet to client to update base and npc configs.
     *
     * @param packet       config packet that will be sent
     * @param targetClient client the packet will be sent to
     */
    public void sendPacket(ConfigPacket packet, PlayerEntity targetClient) {
        CHANNEL.serverHandle(targetClient).send(packet);
    }

    private void registerUpdateBaseConfig() {
        CHANNEL.registerServerbound(UpdateBaseConfigPacket.class, (configPacket, serverAccess) -> {
            if (authorizer.isAuthorized(serverAccess)) {
                configProvider.setBaseConfig(configPacket.baseConfig());
                LogUtil.info("Updated base config to: " + configPacket);
            }
        });
    }

    private void registerUpdateNpcConfig() {
        CHANNEL.registerServerbound(UpdateNpcConfigPacket.class, (configPacket, serverAccess) -> {
            if (authorizer.isAuthorized(serverAccess)) {
                NPCConfig updatedConfig = configPacket.npcConfig();
                configProvider.updateNpcConfig(updatedConfig);
                // Update system prompt for active NPC if it exists
                npcService.updateNpcSystemPrompt(updatedConfig.getUuid());
                LogUtil.info("Updated npc config to: " + configPacket);
            }
        });
    }

    private void registerAddNpc() {
        CHANNEL.registerServerbound(CreateNpcPacket.class, (createNpcPacket, serverAccess) -> {
            if (authorizer.isAuthorized(serverAccess)) {
                npcService.createNpc(createNpcPacket.npcConfig(), serverAccess.runtime(),
                        serverAccess.player().getBlockPos(), serverAccess.player());
            }
        });
    }

    private void registerDeleteNpc() {
        CHANNEL.registerServerbound(DeleteNpcPacket.class, (configPacket, serverAccess) -> {
            if (authorizer.isAuthorized(serverAccess)) {
                PlayerManager playerManager = EntityVer.getWorld(serverAccess.player()).getServer().getPlayerManager();
                UUID uuid = UUID.fromString(configPacket.uuid());

                if (configPacket.isDelete()) {
                    npcService.deleteNpc(uuid, playerManager);
                } else {
                    npcService.removeNpc(uuid, playerManager);
                }
            }
        });
    }

    private void registerStartStopSTT() {
        CHANNEL.registerServerbound(STTPacket.class, (sttPacket, serverAccess) -> {
            if (authorizer.isAuthorized(serverAccess) && authorizer.isLocalConnection(serverAccess)) {
                STTCallback.EVENT.invoker().onSTTAction(sttPacket.type());
                LogUtil.info("STT action: " + sttPacket);
            }
        });
    }

    private void registerEndecs() {
        CHANNEL.addEndecs(builder -> {
            builder.register(ConfigPacket.ENDEC, ConfigPacket.class);
            builder.register(BaseConfig.ENDEC, BaseConfig.class);
            builder.register(NPCConfig.ENDEC, NPCConfig.class);
            builder.register(CreateNpcPacket.ENDEC, CreateNpcPacket.class);
            builder.register(DeleteNpcPacket.ENDEC, DeleteNpcPacket.class);
            builder.register(UpdateNpcConfigPacket.ENDEC, UpdateNpcConfigPacket.class);
            builder.register(UpdateBaseConfigPacket.ENDEC, UpdateBaseConfigPacket.class);
            builder.register(STTPacket.ENDEC, STTPacket.class);
        });
    }
}
