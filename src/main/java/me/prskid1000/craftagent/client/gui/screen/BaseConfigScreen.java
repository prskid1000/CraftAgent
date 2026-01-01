package me.prskid1000.craftagent.client.gui.screen;

import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import me.prskid1000.craftagent.client.networking.ClientNetworkManager;
import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.networking.packet.UpdateBaseConfigPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static me.prskid1000.craftagent.CraftAgent.MOD_ID;

public class BaseConfigScreen extends ConfigScreen<BaseConfig> {

    private static final Identifier ID = Identifier.of(MOD_ID, "baseconfig");

    public BaseConfigScreen(
        ClientNetworkManager networkManager,
        BaseConfig baseConfig,
        boolean isEdit
    ) {
        super(networkManager, baseConfig, isEdit, ID);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        FlowLayout panel = rootComponent.childById(FlowLayout.class, "panel");

        panel.childById(LabelComponent.class, "llmTimeout-label").text(Text.of(BaseConfig.LLM_TIMEOUT_KEY));
        panel.childById(DiscreteSliderComponent.class, "llmTimeout")
                .setFromDiscreteValue(config.getLlmTimeout())
                .onChanged()
                .subscribe(value -> config.setLlmTimeout((int) Math.round(value)));

        panel.childById(LabelComponent.class, "chunkRadius-label").text(Text.of(BaseConfig.CONTEXT_CHUNK_RADIUS_KEY));
        panel.childById(DiscreteSliderComponent.class, "chunkRadius")
                .setFromDiscreteValue(config.getContextChunkRadius())
                .onChanged()
                .subscribe(value -> config.setContextChunkRadius((int) Math.round(value)));

        panel.childById(LabelComponent.class, "verticalScanRange-label").text(Text.of(BaseConfig.CONTEXT_VERTICAL_RANGE_KEY));
        panel.childById(DiscreteSliderComponent.class, "verticalScanRange")
                .setFromDiscreteValue(config.getContextVerticalScanRange())
                .onChanged()
                .subscribe(value -> config.setContextVerticalScanRange((int) Math.round(value)));

        panel.childById(LabelComponent.class, "cacheExpiryTime-label").text(Text.of(BaseConfig.CHUNK_EXPIRY_TIME_KEY));
        panel.childById(DiscreteSliderComponent.class, "cacheExpiryTime")
                .setFromDiscreteValue(config.getChunkExpiryTime())
                .onChanged()
                .subscribe(value -> config.setChunkExpiryTime((int) Math.round(value)));

        panel.childById(LabelComponent.class, "conversationHistoryLength-label").text(Text.of(BaseConfig.CONVERSATION_HISTORY_LENGTH_KEY));
        panel.childById(DiscreteSliderComponent.class, "conversationHistoryLength")
                .setFromDiscreteValue(config.getConversationHistoryLength())
                .onChanged()
                .subscribe(value -> config.setConversationHistoryLength((int) Math.round(value)));

        panel.childById(LabelComponent.class, "maxLocations-label").text(Text.of(BaseConfig.MAX_LOCATIONS_KEY));
        panel.childById(DiscreteSliderComponent.class, "maxLocations")
                .setFromDiscreteValue(config.getMaxLocations())
                .onChanged()
                .subscribe(value -> config.setMaxLocations((int) Math.round(value)));

        panel.childById(LabelComponent.class, "maxContacts-label").text(Text.of(BaseConfig.MAX_CONTACTS_KEY));
        panel.childById(DiscreteSliderComponent.class, "maxContacts")
                .setFromDiscreteValue(config.getMaxContacts())
                .onChanged()
                .subscribe(value -> config.setMaxContacts((int) Math.round(value)));

        panel.childById(LabelComponent.class, "maxNearbyBlocks-label").text(Text.of(BaseConfig.MAX_NEARBY_BLOCKS_KEY));
        panel.childById(DiscreteSliderComponent.class, "maxNearbyBlocks")
                .setFromDiscreteValue(config.getMaxNearbyBlocks())
                .onChanged()
                .subscribe(value -> config.setMaxNearbyBlocks((int) Math.round(value)));

        panel.childById(LabelComponent.class, "maxNearbyEntities-label").text(Text.of(BaseConfig.MAX_NEARBY_ENTITIES_KEY));
        panel.childById(DiscreteSliderComponent.class, "maxNearbyEntities")
                .setFromDiscreteValue(config.getMaxNearbyEntities())
                .onChanged()
                .subscribe(value -> config.setMaxNearbyEntities((int) Math.round(value)));

        panel.childById(LabelComponent.class, "verbose-label").text(Text.of(BaseConfig.VERBOSE_KEY));
        panel.childById(CheckboxComponent.class, "verbose")
                .checked(config.isVerbose())
                .onChanged(listener -> config.setVerbose(!config.isVerbose()));

        onPressSaveButton(panel, button -> {
            networkManager.sendPacket(new UpdateBaseConfigPacket(config));
            close();
        });
    }
}
