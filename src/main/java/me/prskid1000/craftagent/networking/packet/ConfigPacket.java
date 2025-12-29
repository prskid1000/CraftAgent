package me.prskid1000.craftagent.networking.packet;

import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.config.NPCConfig;

import java.util.List;

public record ConfigPacket(BaseConfig baseConfig, List<NPCConfig> npcConfigs) {

    public static final StructEndec<ConfigPacket> ENDEC = StructEndecBuilder.of(
            BaseConfig.ENDEC.fieldOf("baseConfig", ConfigPacket::baseConfig),
            NPCConfig.ENDEC.listOf().fieldOf("npcConfigs", ConfigPacket::npcConfigs),
            ConfigPacket::new
    );

    /**
     * Removes llm secret from the npcConfigs.
     * Currently no secrets to hide.
     */
    public void hideSecret() {
        // No secrets to hide
    }

    @Override
    public String toString() {
        return "ConfigPacket{" +
                "baseConfig=" + baseConfig +
                ",npcConfigs=" + npcConfigs +
                '}';
    }

}
