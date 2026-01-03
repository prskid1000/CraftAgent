package me.prskid1000.craftagent.config;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class BaseConfig implements Configurable {
    private int llmTimeout = 10;
    private int contextChunkRadius = 4;
    private int contextVerticalScanRange = 8;
    private int chunkExpiryTime = 60;
    private int conversationHistoryLength = 5;
    private int maxLocations = 10;
    private int maxContacts = 20;
    private int maxMessages = 50;
    private int maxSharebookPages = 20;
    private int maxNearbyBlocks = 30;
    private int maxNearbyEntities = 15;
    private boolean verbose = false;
    private boolean disableDirectVanillaCommands = false;
    private boolean disableUtilityCommands = false;
    private int llmProcessingInterval = 5;
    private int llmMinInterval = 10;

    public int getLlmTimeout() {
        return llmTimeout;
    }

    public int getContextVerticalScanRange() {
        return contextVerticalScanRange;
    }

    public int getContextChunkRadius() {
        return contextChunkRadius;
    }

    public int getChunkExpiryTime() {
        return chunkExpiryTime;
    }

    public void setContextChunkRadius(int contextChunkRadius) {
        this.contextChunkRadius = contextChunkRadius;
    }

    public void setChunkExpiryTime(int chunkExpiryTime) {
        this.chunkExpiryTime = chunkExpiryTime;
    }

    public void setContextVerticalScanRange(int contextVerticalScanRange) {
        this.contextVerticalScanRange = contextVerticalScanRange;
    }

    public void setLlmTimeout(int llmTimeout) {
        this.llmTimeout = llmTimeout;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isDisableDirectVanillaCommands() {
        return disableDirectVanillaCommands;
    }

    public void setDisableDirectVanillaCommands(boolean disableDirectVanillaCommands) {
        this.disableDirectVanillaCommands = disableDirectVanillaCommands;
    }

    public boolean isDisableUtilityCommands() {
        return disableUtilityCommands;
    }

    public void setDisableUtilityCommands(boolean disableUtilityCommands) {
        this.disableUtilityCommands = disableUtilityCommands;
    }

    public int getConversationHistoryLength() {
        return conversationHistoryLength;
    }

    public void setConversationHistoryLength(int conversationHistoryLength) {
        this.conversationHistoryLength = conversationHistoryLength;
    }

    public int getMaxLocations() {
        return maxLocations;
    }

    public void setMaxLocations(int maxLocations) {
        this.maxLocations = maxLocations;
    }

    public int getMaxContacts() {
        return maxContacts;
    }

    public void setMaxContacts(int maxContacts) {
        this.maxContacts = maxContacts;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public int getMaxSharebookPages() {
        return maxSharebookPages;
    }

    public void setMaxSharebookPages(int maxSharebookPages) {
        this.maxSharebookPages = maxSharebookPages;
    }

    public int getMaxNearbyBlocks() {
        return maxNearbyBlocks;
    }

    public void setMaxNearbyBlocks(int maxNearbyBlocks) {
        this.maxNearbyBlocks = maxNearbyBlocks;
    }

    public int getMaxNearbyEntities() {
        return maxNearbyEntities;
    }

    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = maxNearbyEntities;
    }

    public int getLlmProcessingInterval() {
        return llmProcessingInterval;
    }

    public void setLlmProcessingInterval(int llmProcessingInterval) {
        this.llmProcessingInterval = llmProcessingInterval;
    }

    public int getLlmMinInterval() {
        return llmMinInterval;
    }

    public void setLlmMinInterval(int llmMinInterval) {
        this.llmMinInterval = llmMinInterval;
    }

    @Override
    public String getConfigName() {
        return "base";
    }

    public static final StructEndec<BaseConfig> ENDEC = StructEndecBuilder.of(
            Endec.INT.fieldOf("llmTimeout", BaseConfig::getLlmTimeout),
            Endec.INT.fieldOf("contextChunkRadius", BaseConfig::getContextChunkRadius),
            Endec.INT.fieldOf("contextVerticalScanRange", BaseConfig::getContextVerticalScanRange),
            Endec.INT.fieldOf("chunkExpiryTime", BaseConfig::getChunkExpiryTime),
            Endec.INT.fieldOf("conversationHistoryLength", BaseConfig::getConversationHistoryLength),
            Endec.INT.fieldOf("maxLocations", BaseConfig::getMaxLocations),
            Endec.INT.fieldOf("maxContacts", BaseConfig::getMaxContacts),
            Endec.INT.fieldOf("maxMessages", BaseConfig::getMaxMessages),
            Endec.INT.fieldOf("maxSharebookPages", BaseConfig::getMaxSharebookPages),
            Endec.INT.fieldOf("maxNearbyBlocks", BaseConfig::getMaxNearbyBlocks),
            Endec.INT.fieldOf("maxNearbyEntities", BaseConfig::getMaxNearbyEntities),
            Endec.BOOLEAN.fieldOf("verbose", BaseConfig::isVerbose),
            Endec.BOOLEAN.fieldOf("disableDirectVanillaCommands", BaseConfig::isDisableDirectVanillaCommands),
            Endec.BOOLEAN.fieldOf("disableUtilityCommands", BaseConfig::isDisableUtilityCommands),
            Endec.INT.fieldOf("llmProcessingInterval", BaseConfig::getLlmProcessingInterval),
            Endec.INT.fieldOf("llmMinInterval", BaseConfig::getLlmMinInterval),
            BaseConfig::new
    );

    @Override
    public String toString() {
        return "BaseConfig{" +
                "llmTimeout=" + llmTimeout +
                ",contextChunkRadius=" + contextChunkRadius +
                ",contextVerticalScanRange=" + contextVerticalScanRange +
                ",chunkExpiryTime=" + chunkExpiryTime +
                ",conversationHistoryLength=" + conversationHistoryLength +
                ",maxLocations=" + maxLocations +
                ",maxContacts=" + maxContacts +
                ",maxMessages=" + maxMessages +
                ",maxSharebookPages=" + maxSharebookPages +
                ",maxNearbyBlocks=" + maxNearbyBlocks +
                ",maxNearbyEntities=" + maxNearbyEntities +
                ",verbose=" + verbose +
                ",disableDirectVanillaCommands=" + disableDirectVanillaCommands +
                ",disableUtilityCommands=" + disableUtilityCommands +
                ",llmProcessingInterval=" + llmProcessingInterval +
                ",llmMinInterval=" + llmMinInterval +"}";
    }

    public static final String LLM_TIMEOUT_KEY = "LLM Service Timeout";
    public static final String CONTEXT_CHUNK_RADIUS_KEY = "Chunk Radius";
    public static final String CONTEXT_VERTICAL_RANGE_KEY = "Vertical Scan Range";
    public static final String CHUNK_EXPIRY_TIME_KEY = "Chunk Expiry Time";
    public static final String CONVERSATION_HISTORY_LENGTH_KEY = "Conversation History Length";
    public static final String MAX_LOCATIONS_KEY = "Max Locations";
    public static final String MAX_CONTACTS_KEY = "Max Contacts";
    public static final String MAX_MESSAGES_KEY = "Max Messages";
    public static final String MAX_SHAREBOOK_PAGES_KEY = "Max Sharebook Pages";
    public static final String MAX_NEARBY_BLOCKS_KEY = "Max Nearby Blocks";
    public static final String MAX_NEARBY_ENTITIES_KEY = "Max Nearby Entities";
    public static final String VERBOSE_KEY = "Debug Mode";
    public static final String DISABLE_DIRECT_VANILLA_COMMANDS_KEY = "Disable Direct Vanilla Commands";
    public static final String DISABLE_UTILITY_COMMANDS_KEY = "Disable Utility Commands";
    public static final String LLM_PROCESSING_INTERVAL_KEY = "LLM Processing Interval (seconds)";
    public static final String LLM_MIN_INTERVAL_KEY = "LLM Min Interval Between Success (seconds)";
}
