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

        bindSlider(content, "maxPrivatePages-label", "maxPrivatePages",
                BaseConfig.MAX_PRIVATE_PAGES_KEY, config.getMaxPrivatePages(), config::setMaxPrivatePages);

        bindSlider(content, "maxMessages-label", "maxMessages",
                BaseConfig.MAX_MESSAGES_KEY, config.getMaxMessages(), config::setMaxMessages);

        bindSlider(content, "maxSharebookPages-label", "maxSharebookPages",
                BaseConfig.MAX_SHAREBOOK_PAGES_KEY, config.getMaxSharebookPages(), config::setMaxSharebookPages);

        bindSlider(content, "maxNearbyBlocks-label", "maxNearbyBlocks",
                BaseConfig.MAX_NEARBY_BLOCKS_KEY, config.getMaxNearbyBlocks(), config::setMaxNearbyBlocks);

        bindSlider(content, "maxNearbyEntities-label", "maxNearbyEntities",
                BaseConfig.MAX_NEARBY_ENTITIES_KEY, config.getMaxNearbyEntities(), config::setMaxNearbyEntities);

        bindSlider(content, "lineOfSightMaxRange-label", "lineOfSightMaxRange",
                BaseConfig.LINE_OF_SIGHT_MAX_RANGE_KEY, config.getLineOfSightMaxRange(), config::setLineOfSightMaxRange);

        bindSlider(content, "lineOfSightItemDetectionRange-label", "lineOfSightItemDetectionRange",
                BaseConfig.LINE_OF_SIGHT_ITEM_DETECTION_RANGE_KEY, config.getLineOfSightItemDetectionRange(), config::setLineOfSightItemDetectionRange);

        content.childById(LabelComponent.class, "verbose-label").text(Text.of(BaseConfig.VERBOSE_KEY));
        CheckboxComponent verbose = content.childById(CheckboxComponent.class, "verbose");
        verbose.checked(config.isVerbose());
        verbose.onChanged(config::setVerbose);

        bindSlider(content, "llmProcessingInterval-label", "llmProcessingInterval",
                BaseConfig.LLM_PROCESSING_INTERVAL_KEY, config.getLlmProcessingInterval(), config::setLlmProcessingInterval);

        bindSlider(content, "llmMinInterval-label", "llmMinInterval",
                BaseConfig.LLM_MIN_INTERVAL_KEY, config.getLlmMinInterval(), config::setLlmMinInterval);

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
