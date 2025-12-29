package me.prskid1000.craftagent.client.networking;

import me.prskid1000.craftagent.client.gui.screen.CraftAgentScreen;
import me.prskid1000.craftagent.networking.NetworkHandler;
import me.prskid1000.craftagent.networking.packet.ConfigPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class ClientNetworkManager {

    private final MinecraftClient client;

    public ClientNetworkManager() {
        this.client = MinecraftClient.getInstance();
    }

    public void registerPacketReceiver() {
        NetworkHandler.CHANNEL.registerClientbound(ConfigPacket.class, (configPacket, access) -> {
            Screen npcConfigScreen = new CraftAgentScreen(configPacket.npcConfigs(), configPacket.baseConfig(), this);
            client.setScreen(npcConfigScreen);
        });
    }

    public <R extends Record> void sendPacket(R packet) {
        NetworkHandler.CHANNEL.clientHandle().send(packet);
    }
}
