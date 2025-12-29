package me.prskid1000.craftagent.client;

import me.prskid1000.craftagent.client.gui.hud.STTHudElement;
import me.prskid1000.craftagent.client.keybind.STTKeybind;
import me.prskid1000.craftagent.client.networking.ClientNetworkManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class CraftAgentClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientNetworkManager networkManager = new ClientNetworkManager();
        networkManager.registerPacketReceiver();

        STTHudElement sttHudElement = new STTHudElement();

        STTKeybind keybind = new STTKeybind(networkManager, sttHudElement);
        keybind.register();

        HudRenderCallback.EVENT.register(sttHudElement);
    }
}
