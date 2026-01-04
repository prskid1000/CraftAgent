package me.prskid1000.craftagent.context;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Tracks the navigation state of an NPC.
 * Maintains information about whether the NPC is traveling to a destination or idle.
 */
public class NavigationState {
    
    public enum State {
        IDLE,
        TRAVELING,
        ARRIVED
    }
    
    private State currentState;
    private BlockPos destination;
    private Vec3d destinationVec;
    private double distanceThreshold;
    private long stateChangeTime;
    
    public NavigationState() {
        this.currentState = State.IDLE;
        this.destination = null;
        this.destinationVec = null;
        this.distanceThreshold = 3.0; // Default: consider arrived within 3 blocks
        this.stateChangeTime = System.currentTimeMillis();
    }
    
    /**
     * Sets the NPC to traveling state with a block position destination.
     */
    public void setTravelingTo(BlockPos destination) {
        this.currentState = State.TRAVELING;
        this.destination = destination;
        this.destinationVec = destination.toCenterPos();
        this.stateChangeTime = System.currentTimeMillis();
    }
    
    /**
     * Sets the NPC to traveling state with a vector destination.
     */
    public void setTravelingTo(Vec3d destination) {
        this.currentState = State.TRAVELING;
        this.destination = BlockPos.ofFloored(destination);
        this.destinationVec = destination;
        this.stateChangeTime = System.currentTimeMillis();
    }
    
    /**
     * Sets the NPC to idle state (no destination).
     */
    public void setIdle() {
        this.currentState = State.IDLE;
        this.destination = null;
        this.destinationVec = null;
        this.stateChangeTime = System.currentTimeMillis();
    }
    
    /**
     * Marks the NPC as arrived at destination.
     */
    public void setArrived() {
        this.currentState = State.ARRIVED;
        this.stateChangeTime = System.currentTimeMillis();
    }
    
    /**
     * Updates the state based on current position.
     * Checks if NPC has arrived at destination.
     */
    public void update(Vec3d currentPosition) {
        if (currentState == State.TRAVELING && destinationVec != null) {
            double distance = currentPosition.distanceTo(destinationVec);
            if (distance <= distanceThreshold) {
                setArrived();
            }
        }
    }
    
    /**
     * Gets the current navigation state.
     */
    public State getState() {
        return currentState;
    }
    
    /**
     * Gets the destination block position, if any.
     */
    public Optional<BlockPos> getDestination() {
        return Optional.ofNullable(destination);
    }
    
    /**
     * Gets the destination vector, if any.
     */
    public Optional<Vec3d> getDestinationVec() {
        return Optional.ofNullable(destinationVec);
    }
    
    /**
     * Gets the distance threshold for considering arrival.
     */
    public double getDistanceThreshold() {
        return distanceThreshold;
    }
    
    /**
     * Sets the distance threshold for considering arrival.
     */
    public void setDistanceThreshold(double distanceThreshold) {
        this.distanceThreshold = distanceThreshold;
    }
    
    /**
     * Gets the time when the state was last changed (milliseconds since epoch).
     */
    public long getStateChangeTime() {
        return stateChangeTime;
    }
    
    /**
     * Gets the time spent in current state (milliseconds).
     */
    public long getTimeInCurrentState() {
        return System.currentTimeMillis() - stateChangeTime;
    }
    
    /**
     * Checks if the NPC is currently traveling.
     */
    public boolean isTraveling() {
        return currentState == State.TRAVELING;
    }
    
    /**
     * Checks if the NPC is idle.
     */
    public boolean isIdle() {
        return currentState == State.IDLE;
    }
    
    /**
     * Checks if the NPC has arrived at destination.
     */
    public boolean hasArrived() {
        return currentState == State.ARRIVED;
    }
    
    /**
     * Gets a string representation of the navigation state for context.
     */
    public String getStateDescription() {
        switch (currentState) {
            case IDLE:
                return "idle";
            case TRAVELING:
                if (destination != null) {
                    return String.format("traveling to (%d, %d, %d)", 
                        destination.getX(), destination.getY(), destination.getZ());
                }
                return "traveling";
            case ARRIVED:
                return "arrived at destination";
            default:
                return "unknown";
        }
    }
}

