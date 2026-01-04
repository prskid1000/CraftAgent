package me.prskid1000.craftagent.context;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the current action state for an NPC.
 * Tracks what action is being performed and related state information.
 */
public class ActionStateManager {
    
    public enum ActionType {
        IDLE,
        MINING,
        BUILDING,
        CRAFTING,
        HUNTING,
        FARMING,
        FISHING,
        COMBAT,
        TRAVELING
    }
    
    private ActionType currentAction;
    private Map<String, Object> actionData;
    private long actionStartTime;
    
    public ActionStateManager() {
        this.currentAction = ActionType.IDLE;
        this.actionData = new HashMap<>();
        this.actionStartTime = System.currentTimeMillis();
    }
    
    /**
     * Sets the current action type.
     */
    public void setAction(ActionType actionType) {
        this.currentAction = actionType;
        this.actionData.clear();
        this.actionStartTime = System.currentTimeMillis();
    }
    
    /**
     * Sets the current action with data.
     */
    public void setAction(ActionType actionType, Map<String, Object> data) {
        this.currentAction = actionType;
        this.actionData = data != null ? new HashMap<>(data) : new HashMap<>();
        this.actionStartTime = System.currentTimeMillis();
    }
    
    /**
     * Updates action data without changing action type.
     */
    public void updateActionData(String key, Object value) {
        if (this.actionData == null) {
            this.actionData = new HashMap<>();
        }
        this.actionData.put(key, value);
    }
    
    /**
     * Sets action to idle.
     */
    public void setIdle() {
        this.currentAction = ActionType.IDLE;
        this.actionData.clear();
        this.actionStartTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the current action type.
     */
    public ActionType getCurrentAction() {
        return currentAction;
    }
    
    /**
     * Gets action data.
     */
    public Map<String, Object> getActionData() {
        return new HashMap<>(actionData);
    }
    
    /**
     * Gets a specific action data value.
     */
    public Optional<Object> getActionData(String key) {
        return Optional.ofNullable(actionData.get(key));
    }
    
    /**
     * Gets the time when the current action started (milliseconds since epoch).
     */
    public long getActionStartTime() {
        return actionStartTime;
    }
    
    /**
     * Gets the time spent in current action (milliseconds).
     */
    public long getTimeInCurrentAction() {
        return System.currentTimeMillis() - actionStartTime;
    }
    
    /**
     * Checks if NPC is currently performing an action.
     */
    public boolean isIdle() {
        return currentAction == ActionType.IDLE;
    }
    
    /**
     * Checks if NPC is performing a specific action.
     */
    public boolean isPerforming(ActionType actionType) {
        return currentAction == actionType;
    }
    
    /**
     * Gets a human-readable description of the current action state.
     */
    public String getActionDescription() {
        switch (currentAction) {
            case IDLE:
                return "idle";
            case MINING:
                String blockType = (String) actionData.getOrDefault("blockType", "unknown");
                Integer count = (Integer) actionData.getOrDefault("count", 1);
                Integer mined = (Integer) actionData.getOrDefault("mined", 0);
                return String.format("mining %s (%d/%d)", blockType, mined, count);
            case BUILDING:
                String buildBlock = (String) actionData.getOrDefault("blockType", "unknown");
                BlockPos buildPos = (BlockPos) actionData.get("position");
                if (buildPos != null) {
                    return String.format("building %s at (%d, %d, %d)", buildBlock, buildPos.getX(), buildPos.getY(), buildPos.getZ());
                }
                return "building " + buildBlock;
            case CRAFTING:
                String itemName = (String) actionData.getOrDefault("itemName", "unknown");
                return "crafting " + itemName;
            case HUNTING:
                String target = (String) actionData.getOrDefault("target", "unknown");
                return "hunting " + target;
            case FARMING:
                String operation = (String) actionData.getOrDefault("operation", "unknown");
                String cropType = (String) actionData.getOrDefault("cropType", "");
                if (!cropType.isEmpty()) {
                    return String.format("farming %s %s", operation, cropType);
                }
                return "farming " + operation;
            case FISHING:
                return "fishing";
            case COMBAT:
                String enemy = (String) actionData.getOrDefault("target", "unknown");
                return "in combat with " + enemy;
            case TRAVELING:
                BlockPos dest = (BlockPos) actionData.get("destination");
                if (dest != null) {
                    return String.format("traveling to (%d, %d, %d)", dest.getX(), dest.getY(), dest.getZ());
                }
                return "traveling";
            default:
                return "unknown";
        }
    }
}

