package me.prskid1000.craftagent.client;

import me.prskid1000.craftagent.client.networking.ClientNetworkManager;
import net.fabricmc.api.ClientModInitializer;

public class CraftAgentClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientNetworkManager networkManager = new ClientNetworkManager();
        networkManager.registerPacketReceiver();
    }
}
