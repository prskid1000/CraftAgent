package me.prskid1000.craftagent.context;

import me.prskid1000.craftagent.model.context.ContextData;
import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides line of sight detection for NPCs using Minecraft's raycasting.
 * Detects items, entities, and blocks that are visible in the NPC's line of sight.
 */
public class LineOfSightProvider {
    
    private final ServerPlayerEntity npcEntity;
    private final double maxRange;
    private final double itemDetectionRange;
    
    public LineOfSightProvider(ServerPlayerEntity npcEntity, double maxRange, double itemDetectionRange) {
        this.npcEntity = npcEntity;
        this.maxRange = maxRange;
        this.itemDetectionRange = itemDetectionRange;
    }
    
    /**
     * Gets all items visible in line of sight.
     */
    public List<ContextData.ItemEntityData> getItemsInLineOfSight() {
        List<ContextData.ItemEntityData> visibleItems = new ArrayList<>();
        
        try {
            World world = npcEntity.getWorld();
            Vec3d eyePos = npcEntity.getEyePos();
            Vec3d lookVec = npcEntity.getRotationVector();
            Vec3d endPos = eyePos.add(lookVec.multiply(maxRange));
            
            // Get nearby item entities
            Box searchBox = npcEntity.getBoundingBox().expand(itemDetectionRange);
            List<ItemEntity> nearbyItems = world.getEntitiesByClass(
                ItemEntity.class,
                searchBox,
                item -> true
            );
            
            // Check line of sight for each item
            for (ItemEntity item : nearbyItems) {
                if (isInLineOfSight(eyePos, item.getPos(), maxRange)) {
                    String itemName = item.getStack().getItem().getTranslationKey();
                    String itemType = itemName.substring(itemName.lastIndexOf(".") + 1);
                    int count = item.getStack().getCount();
                    double distance = eyePos.distanceTo(item.getPos());
                    
                    visibleItems.add(new ContextData.ItemEntityData(
                        itemType,
                        count,
                        item.getBlockPos(),
                        distance
                    ));
                }
            }
            
            // Sort by distance (closest first)
            visibleItems.sort((a, b) -> Double.compare(a.distance(), b.distance()));
            
        } catch (Exception e) {
            LogUtil.error("Error getting items in line of sight", e);
        }
        
        return visibleItems;
    }
    
    /**
     * Gets all entities visible in line of sight.
     */
    public List<ContextData.EntityData> getEntitiesInLineOfSight() {
        List<ContextData.EntityData> visibleEntities = new ArrayList<>();
        
        try {
            World world = npcEntity.getWorld();
            Vec3d eyePos = npcEntity.getEyePos();
            Vec3d lookVec = npcEntity.getRotationVector();
            Vec3d endPos = eyePos.add(lookVec.multiply(maxRange));
            
            // Get nearby entities (excluding the NPC itself)
            Box searchBox = npcEntity.getBoundingBox().expand(maxRange);
            List<Entity> nearbyEntities = world.getOtherEntities(
                npcEntity,
                searchBox,
                entity -> !(entity instanceof ItemEntity) // Exclude items (handled separately)
            );
            
            // Check line of sight for each entity
            for (Entity entity : nearbyEntities) {
                Vec3d entityPos = entity.getBoundingBox().getCenter();
                if (isInLineOfSight(eyePos, entityPos, maxRange)) {
                    double distance = eyePos.distanceTo(entityPos);
                    
                    visibleEntities.add(new ContextData.EntityData(
                        entity.getId(),
                        entity.getName().getString(),
                        entity.isPlayer()
                    ));
                }
            }
            
            // Sort by distance (closest first)
            visibleEntities.sort((a, b) -> {
                // We can't easily get distance here, so just prioritize players
                if (a.isPlayer() && !b.isPlayer()) return -1;
                if (!a.isPlayer() && b.isPlayer()) return 1;
                return 0;
            });
            
        } catch (Exception e) {
            LogUtil.error("Error getting entities in line of sight", e);
        }
        
        return visibleEntities;
    }
    
    /**
     * Gets the block directly in line of sight (where the NPC is looking).
     */
    public ContextData.BlockData getBlockInLineOfSight() {
        try {
            World world = npcEntity.getWorld();
            Vec3d eyePos = npcEntity.getEyePos();
            Vec3d lookVec = npcEntity.getRotationVector();
            Vec3d endPos = eyePos.add(lookVec.multiply(maxRange));
            
            // Perform raycast to find the block being looked at
            BlockHitResult hitResult = world.raycast(new RaycastContext(
                eyePos,
                endPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                npcEntity
            ));
            
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = hitResult.getBlockPos();
                var blockState = world.getBlockState(blockPos);
                String blockName = blockState.getBlock().getName().getString()
                    .toLowerCase().replace(" ", "_");
                
                // Skip air blocks
                if (blockName.contains("air")) {
                    return null;
                }
                
                double distance = eyePos.distanceTo(hitResult.getPos());
                
                return new ContextData.BlockData(
                    blockName,
                    blockPos,
                    me.prskid1000.craftagent.util.MCDataUtil.getMiningLevel(blockState),
                    me.prskid1000.craftagent.util.MCDataUtil.getToolNeeded(blockState)
                );
            }
            
        } catch (Exception e) {
            LogUtil.error("Error getting block in line of sight", e);
        }
        
        return null;
    }
    
    /**
     * Gets all blocks visible in a cone in front of the NPC (within field of view).
     */
    public List<ContextData.BlockData> getBlocksInLineOfSight(int maxBlocks) {
        List<ContextData.BlockData> visibleBlocks = new ArrayList<>();
        
        try {
            World world = npcEntity.getWorld();
            Vec3d eyePos = npcEntity.getEyePos();
            Vec3d lookVec = npcEntity.getRotationVector();
            
            // Sample points along the look vector and in a cone
            for (double distance = 1.0; distance <= maxRange && visibleBlocks.size() < maxBlocks; distance += 2.0) {
                Vec3d samplePos = eyePos.add(lookVec.multiply(distance));
                BlockPos blockPos = BlockPos.ofFloored(samplePos);
                
                if (!world.isAir(blockPos)) {
                    var blockState = world.getBlockState(blockPos);
                    String blockName = blockState.getBlock().getName().getString()
                        .toLowerCase().replace(" ", "_");
                    
                    if (!blockName.contains("air") && isInLineOfSight(eyePos, samplePos, maxRange)) {
                        visibleBlocks.add(new ContextData.BlockData(
                            blockName,
                            blockPos,
                            me.prskid1000.craftagent.util.MCDataUtil.getMiningLevel(blockState),
                            me.prskid1000.craftagent.util.MCDataUtil.getToolNeeded(blockState)
                        ));
                    }
                }
            }
            
        } catch (Exception e) {
            LogUtil.error("Error getting blocks in line of sight", e);
        }
        
        return visibleBlocks;
    }
    
    /**
     * Checks if a target position is in line of sight (no blocks blocking the view).
     */
    private boolean isInLineOfSight(Vec3d startPos, Vec3d targetPos, double maxRange) {
        try {
            World world = npcEntity.getWorld();
            double distance = startPos.distanceTo(targetPos);
            
            if (distance > maxRange) {
                return false;
            }
            
            // Perform raycast to check if there are blocking blocks
            BlockHitResult hitResult = world.raycast(new RaycastContext(
                startPos,
                targetPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                npcEntity
            ));
            
            // If we hit a block before reaching the target, it's not in line of sight
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                double hitDistance = startPos.distanceTo(hitResult.getPos());
                // Allow small tolerance (0.5 blocks) for edge cases
                return hitDistance >= distance - 0.5;
            }
            
            return true;
            
        } catch (Exception e) {
            LogUtil.error("Error checking line of sight", e);
            return false;
        }
    }
}

