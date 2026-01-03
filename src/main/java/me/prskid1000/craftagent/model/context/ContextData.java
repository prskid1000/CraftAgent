package me.prskid1000.craftagent.model.context;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Consolidated data classes for NPC context information.
 * Contains all context-related data structures in one place for better organization.
 */
public class ContextData {
    
    /**
     * Represents block information in the world
     */
    public record BlockData(
        String type,
        BlockPos position,
        String mineLevel,
        String toolNeeded
    ) {
        @Override
        public String toString() {
            return "BlockData{" +
                    "type='" + type + '\'' +
                    ", position=" + position +
                    '}';
        }
    }

    /**
     * Represents entity information in the world
     */
    public record EntityData(int id, String name, boolean isPlayer) {
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != this.getClass()) {
                return false;
            }
            EntityData other = (EntityData) obj;
            return other.name.equals(this.name);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }
    }

    /**
     * Represents item information in inventory
     */
    public record ItemData(String type, int count, int slot) {}

    /**
     * Represents inventory data (armor, main inventory, hotbar, off-hand)
     */
    public record InventoryData(
        List<ItemData> armor,
        List<ItemData> mainInventory,
        List<ItemData> hotbar,
        List<ItemData> offHand
    ) {
        public List<ItemData> getAllItems() {
            List<ItemData> allItems = new ArrayList<>();
            allItems.addAll(armor);
            allItems.addAll(mainInventory);
            allItems.addAll(hotbar);
            allItems.addAll(offHand);
            return allItems;
        }
    }

    /**
     * Represents NPC state data (position, health, food, biome)
     */
    public record StateData(BlockPos position, float health, int food, String biome) {}
}

