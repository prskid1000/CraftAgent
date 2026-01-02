package me.prskid1000.craftagent.context;

import lombok.Getter;
import me.prskid1000.craftagent.config.BaseConfig;
import me.prskid1000.craftagent.model.context.BlockData;
import me.prskid1000.craftagent.util.LogUtil;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.*;

import static me.prskid1000.craftagent.util.MCDataUtil.getMiningLevel;
import static me.prskid1000.craftagent.util.MCDataUtil.getToolNeeded;

public class ChunkManager {
    private volatile int maxNearbyBlocks;
    private volatile int verticalScanRange;
    private volatile int chunkRadius;

    private final ServerPlayerEntity npcEntity;
    private final ScheduledExecutorService threadPool;
    private final List<BlockData> currentLoadedBlocks;
    private java.util.concurrent.ScheduledFuture<?> refreshTask;

    private final List<BlockData> nearbyBlocks = new ArrayList<>();
    
    public List<BlockData> getNearbyBlocks() {
        // Return limited list (keep most recent/nearest)
        return nearbyBlocks.size() > maxNearbyBlocks 
            ? nearbyBlocks.subList(0, maxNearbyBlocks)
            : nearbyBlocks;
    }


    public ChunkManager(ServerPlayerEntity npcEntity, BaseConfig config) {
        this.npcEntity = npcEntity;
        this.maxNearbyBlocks = config.getMaxNearbyBlocks();
        this.verticalScanRange = config.getContextVerticalScanRange();
        this.chunkRadius = config.getContextChunkRadius();
        this.currentLoadedBlocks = new ArrayList<>();
        this.threadPool = Executors.newSingleThreadScheduledExecutor();
        scheduleRefreshBlocks(config.getChunkExpiryTime());
    }
    
    /**
     * Updates configuration values in real-time.
     * This allows configuration changes to take effect without restarting.
     */
    public synchronized void updateConfig(int newChunkRadius, int newVerticalScanRange, 
                                         int newMaxNearbyBlocks, int newChunkExpiryTime) {
        this.chunkRadius = newChunkRadius;
        this.verticalScanRange = newVerticalScanRange;
        this.maxNearbyBlocks = newMaxNearbyBlocks;
        // Reschedule with new expiry time
        scheduleRefreshBlocks(newChunkExpiryTime);
    }

    private void scheduleRefreshBlocks(int chunkExpiryTime) {
        // Cancel existing task if any
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel(false);
        }
        // Schedule new task with updated interval
        refreshTask = threadPool.scheduleAtFixedRate(() -> {
            synchronized (this) {
                updateAllBlocks();
                updateNearbyBlocks();
            }
        }, 0, chunkExpiryTime, TimeUnit.SECONDS);
    }

    public List<BlockData> getBlocksOfType(String type, int numberOfBlocks) {
        List<BlockData> blocksFound = new ArrayList<>();

        for (BlockData block : currentLoadedBlocks) {
            if (blocksFound.size() >= numberOfBlocks) {
                break;
            } else if (type.equals(block.type())) {
                blocksFound.add(block);
            }
        }
        if (blocksFound.size() < numberOfBlocks) {
            LogUtil.error("Only %s blocks found of %s (wanted: %s)".formatted(
                    blocksFound.size(), type, numberOfBlocks));
        }
        return blocksFound;
    }

    /**
     * Updates block data of every block type nearest block to the npc
     */
    private void updateNearbyBlocks() {
        Map<String, BlockData> nearestBlocks = new HashMap<>();

        for (BlockData block : currentLoadedBlocks) {
            String blockType = block.type();
            if (!nearestBlocks.containsKey(blockType) ||
                    isCloser(block.position(), nearestBlocks.get(blockType).position())) {
                nearestBlocks.put(blockType, block);
            }
        }
        
        // Clear and add limited blocks (keep only nearest of each type, limit total)
        this.nearbyBlocks.clear();
        List<BlockData> sortedBlocks = new ArrayList<>(nearestBlocks.values());
        sortedBlocks.sort((a, b) -> {
            double distA = npcEntity.getBlockPos().getSquaredDistance(a.position());
            double distB = npcEntity.getBlockPos().getSquaredDistance(b.position());
            return Double.compare(distA, distB);
        });
        
        // Take only the maxNearbyBlocks nearest blocks
        int limit = Math.min(sortedBlocks.size(), maxNearbyBlocks);
        for (int i = 0; i < limit; i++) {
            this.nearbyBlocks.add(sortedBlocks.get(i));
        }
    }

    /**
     * Updates all blocks in the chunks around the NPC
     */
    private void updateAllBlocks() {
        currentLoadedBlocks.clear();
        World world = npcEntity.getWorld();
        ChunkPos centerChunk = npcEntity.getChunkPos();

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                ChunkPos pos = new ChunkPos(centerChunk.x + x, centerChunk.z + z);

                boolean isLoaded = world.isChunkLoaded(pos.x, pos.z);

                if (isLoaded) {
                    currentLoadedBlocks.addAll(scanChunk(pos));
                }
            }
        }
    }

    private List<BlockData> scanChunk(ChunkPos chunk) {
        World world = npcEntity.getWorld();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int baseY = Math.max(0, npcEntity.getBlockPos().getY() - verticalScanRange);
        int maxY = Math.min(world.getHeight(), npcEntity.getBlockPos().getY() + verticalScanRange);

        List<BlockData> blocks = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = baseY; y < maxY; y++) {
                    pos.set(chunk.getStartX() + x, y, chunk.getStartZ() + z);
                    WorldChunk currentChunk = world.getWorldChunk(pos);

                    BlockState blockState = currentChunk.getBlockState(pos);
                    String blockType = blockState.getBlock()
                            .getName().getString()
                            .toLowerCase().replace(" ", "_");

                    if (blockType.contains("air")) continue;

                    if (isAccessible(pos, currentChunk)) {
                        blocks.add(new BlockData(blockType, pos.toImmutable(),
                                getMiningLevel(blockState), getToolNeeded(blockState)));
                    }
                }
            }
        }
        return blocks;
    }

    private boolean isAccessible(BlockPos pos, WorldChunk chunk) {
        for (Direction dir : Direction.values()) {
            if (chunk.getBlockState(pos.offset(dir)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private boolean isCloser(BlockPos pos1, BlockPos pos2) {
        double dist1 = npcEntity.getBlockPos().getSquaredDistance(pos1);
        double dist2 = npcEntity.getBlockPos().getSquaredDistance(pos2);
        return dist1 < dist2;
    }

    public void stopService() {
        this.threadPool.shutdownNow();
        try {
            if (!this.threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                this.threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}