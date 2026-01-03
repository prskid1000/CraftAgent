package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.common.NPCService
import me.prskid1000.craftagent.config.ConfigProvider
import me.prskid1000.craftagent.util.LogUtil
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ArrayBlockingQueue

/**
 * Scheduler that processes NPCs serially in a FIFO queue with configurable intervals.
 * Uses a single-threaded executor to ensure only one NPC processes at a time.
 */
class LLMProcessingScheduler(
    private val npcService: NPCService,
    private val configProvider: ConfigProvider
) : BaseEventListener() {

    // Thread-safe queue for multi-threaded access
    private val fifoQueue = ConcurrentLinkedQueue<UUID>()
    private val lastSuccessfulTrigger = ConcurrentHashMap<UUID, Long>()
    private var lastProcessingTime = 0L
    
    // Single-threaded executor ensures serial processing
    private val executorService: ThreadPoolExecutor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(100),
        ThreadPoolExecutor.CallerRunsPolicy()
    )

    override fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val currentTime = System.currentTimeMillis()
            val baseConfig = configProvider.baseConfig
            val interval = baseConfig.llmProcessingInterval * 1000L // X seconds to ms
            val minInterval = baseConfig.llmMinInterval * 1000L // Y seconds to ms

            // Check if interval X has passed and executor is idle
            if (executorService.queue.isEmpty() && currentTime - lastProcessingTime >= interval) {
                syncQueueWithNPCs() // Ensure queue is up to date
                processNextNPC(server, currentTime, minInterval)
                lastProcessingTime = currentTime
            }
        }
    }

    private fun syncQueueWithNPCs() {
        val currentNPCs = npcService.uuidToNpc.keys.toSet()
        val queueSet = fifoQueue.toSet()
        
        // Add new NPCs to queue
        currentNPCs.forEach { uuid ->
            if (!queueSet.contains(uuid)) {
                fifoQueue.offer(uuid)
                LogUtil.info("Added NPC $uuid to LLM processing queue")
            }
        }
        // Remove deleted NPCs from queue
        val iterator = fifoQueue.iterator()
        while (iterator.hasNext()) {
            val uuid = iterator.next()
            if (!currentNPCs.contains(uuid)) {
                iterator.remove()
                LogUtil.info("Removed deleted NPC $uuid from LLM processing queue")
            }
        }
    }

    private fun processNextNPC(server: MinecraftServer, currentTime: Long, minInterval: Long) {
        val npcUuid = fifoQueue.poll() ?: return

        // Get NPC reference - check again in executor to handle removal
        val npc = npcService.uuidToNpc[npcUuid]
        
        if (npc == null) {
            // NPC was removed, skip and put back at end
            fifoQueue.offer(npcUuid)
            return
        }

        // Check if Y time has passed since last successful processing
        val lastSuccess = lastSuccessfulTrigger[npcUuid] ?: 0L
        val timeSinceLastSuccess = currentTime - lastSuccess

        if (timeSinceLastSuccess >= minInterval) {
            // Y time has passed, process the NPC
            // Capture NPC name for logging (safer than full reference)
            val npcName = npc.config.npcName
            executorService.submit {
                try {
                    // Re-check NPC exists (could be removed while queued)
                    val currentNpc = npcService.uuidToNpc[npcUuid]
                    if (currentNpc == null) {
                        LogUtil.info("NPC $npcName was removed during processing, skipping")
                        return@submit
                    }
                    
                    val success = currentNpc.eventHandler.processLLM()
                    if (success) {
                        lastSuccessfulTrigger[npcUuid] = currentTime
                        LogUtil.info("Successfully processed LLM for NPC: $npcName")
                    } else {
                        LogUtil.info("LLM processing returned false for NPC: $npcName")
                    }
                } catch (e: Exception) {
                    LogUtil.error("Error processing LLM for NPC: $npcName", e)
                } finally {
                    // Always put back at end of queue after processing
                    fifoQueue.offer(npcUuid)
                }
            }
        } else {
            // Y time has not passed, skip and put back at end
            fifoQueue.offer(npcUuid)
        }
    }
    
    fun shutdown() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

