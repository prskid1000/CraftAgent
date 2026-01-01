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

    public BaseConfigScreen(ClientNetworkManager networkManager, BaseConfig baseConfig, boolean isEdit) {
        super(networkManager, baseConfig, isEdit, ID);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // IMPORTANT: operate on padded inner content container
        FlowLayout content = rootComponent.childById(FlowLayout.class, "content");

        bindSlider(content, "llmTimeout-label", "llmTimeout",
                BaseConfig.LLM_TIMEOUT_KEY, config.getLlmTimeout(), config::setLlmTimeout);

        bindSlider(content, "chunkRadius-label", "chunkRadius",
                BaseConfig.CONTEXT_CHUNK_RADIUS_KEY, config.getContextChunkRadius(), config::setContextChunkRadius);

        bindSlider(content, "verticalScanRange-label", "verticalScanRange",
                BaseConfig.CONTEXT_VERTICAL_RANGE_KEY, config.getContextVerticalScanRange(), config::setContextVerticalScanRange);

        bindSlider(content, "cacheExpiryTime-label", "cacheExpiryTime",
                BaseConfig.CHUNK_EXPIRY_TIME_KEY, config.getChunkExpiryTime(), config::setChunkExpiryTime);

        bindSlider(content, "conversationHistoryLength-label", "conversationHistoryLength",
                BaseConfig.CONVERSATION_HISTORY_LENGTH_KEY, config.getConversationHistoryLength(), config::setConversationHistoryLength);

        bindSlider(content, "maxLocations-label", "maxLocations",
                BaseConfig.MAX_LOCATIONS_KEY, config.getMaxLocations(), config::setMaxLocations);

        bindSlider(content, "maxContacts-label", "maxContacts",
                BaseConfig.MAX_CONTACTS_KEY, config.getMaxContacts(), config::setMaxContacts);

        bindSlider(content, "maxNearbyBlocks-label", "maxNearbyBlocks",
                BaseConfig.MAX_NEARBY_BLOCKS_KEY, config.getMaxNearbyBlocks(), config::setMaxNearbyBlocks);

        bindSlider(content, "maxNearbyEntities-label", "maxNearbyEntities",
                BaseConfig.MAX_NEARBY_ENTITIES_KEY, config.getMaxNearbyEntities(), config::setMaxNearbyEntities);

        content.childById(LabelComponent.class, "verbose-label").text(Text.of(BaseConfig.VERBOSE_KEY));
        CheckboxComponent verbose = content.childById(CheckboxComponent.class, "verbose");
        verbose.checked(config.isVerbose());
        verbose.onChanged(config::setVerbose);

        onPressSaveButton(rootComponent, button -> {
            networkManager.sendPacket(new UpdateBaseConfigPacket(config));
            close();
        });
    }

    private void bindSlider(
            FlowLayout content,
            String labelId,
            String sliderId,
            String labelText,
            int initialValue,
            java.util.function.IntConsumer setter
    ) {
        content.childById(LabelComponent.class, labelId).text(Text.of(labelText));

        DiscreteSliderComponent slider = content.childById(DiscreteSliderComponent.class, sliderId);
        slider.setFromDiscreteValue(initialValue);
        slider.onChanged().subscribe(v -> setter.accept((int) Math.round(v)));
    }
}
