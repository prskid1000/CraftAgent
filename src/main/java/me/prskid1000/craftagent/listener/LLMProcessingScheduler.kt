package me.prskid1000.craftagent.listener

import me.prskid1000.craftagent.common.NPCService
import me.prskid1000.craftagent.config.ConfigProvider
import me.prskid1000.craftagent.util.LogUtil
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Scheduler that processes NPCs in a circular queue with configurable intervals.
 * Processes one NPC every X seconds, checking minimum interval Y between successful triggers.
 */
class LLMProcessingScheduler(
    private val npcService: NPCService,
    private val configProvider: ConfigProvider
) : AEventListener() {

    private val circularQueue = LinkedList<UUID>()
    private val lastSuccessfulTrigger = ConcurrentHashMap<UUID, Long>()
    private val currentlyProcessing = ConcurrentHashMap<UUID, Boolean>()
    private var lastProcessingTime = 0L

    override fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val currentTime = System.currentTimeMillis()
            val baseConfig = configProvider.baseConfig
            val interval = baseConfig.llmProcessingInterval * 1000L // X seconds to ms
            val minInterval = baseConfig.llmMinInterval * 1000L // Y seconds to ms

            // Check if interval X has passed
            if (currentTime - lastProcessingTime >= interval) {
                syncQueueWithNPCs() // Ensure queue is up to date
                processNextNPC(server, currentTime, minInterval)
                lastProcessingTime = currentTime
            }
        }
    }

    private fun syncQueueWithNPCs() {
        val currentNPCs = npcService.uuidToNpc.keys.toSet()
        // Add new NPCs to queue
        currentNPCs.forEach { uuid ->
            if (!circularQueue.contains(uuid)) {
                circularQueue.addLast(uuid)
                LogUtil.info("Added NPC $uuid to LLM processing queue")
            }
        }
        // Remove deleted NPCs from queue and processing tracking
        val removed = circularQueue.removeIf { !currentNPCs.contains(it) }
        if (removed) {
            LogUtil.info("Removed deleted NPCs from LLM processing queue")
        }
        // Clean up processing flags for removed NPCs
        currentlyProcessing.keys.removeIf { !currentNPCs.contains(it) }
        
        // Check and clear processing flags for NPCs that have finished
        currentNPCs.forEach { uuid ->
            if (currentlyProcessing[uuid] == true) {
                val npc = npcService.uuidToNpc[uuid]
                if (npc != null && npc.eventHandler.queueIsEmpty()) {
                    // Processing completed, clear flag
                    currentlyProcessing.remove(uuid)
                    LogUtil.debug("Cleared processing flag for NPC: ${npc.config.npcName}")
                }
            }
        }
    }

    private fun processNextNPC(server: MinecraftServer, currentTime: Long, minInterval: Long) {
        if (circularQueue.isEmpty()) return

        var processed = false
        var attempts = 0
        val maxAttempts = circularQueue.size // Prevent infinite loop

        while (!processed && attempts < maxAttempts) {
            val npcUuid = circularQueue.removeFirst()
            val npc = npcService.uuidToNpc[npcUuid] ?: run {
                // NPC was removed, skip
                attempts++
                continue
            }

            // Check minimum interval Y
            val lastSuccess = lastSuccessfulTrigger[npcUuid] ?: 0L
            val timeSinceLastSuccess = currentTime - lastSuccess

            if (timeSinceLastSuccess < minInterval) {
                // Min interval not met, move to end
                circularQueue.addLast(npcUuid)
                attempts++
                continue
            }

            // Check if NPC is already processing
            if (currentlyProcessing[npcUuid] == true) {
                // Already processing, move to end
                circularQueue.addLast(npcUuid)
                attempts++
                continue
            }

            // Check if NPC queue is empty (not already processing)
            if (!npc.eventHandler.queueIsEmpty()) {
                // Already processing, move to end
                circularQueue.addLast(npcUuid)
                attempts++
                continue
            }

            // Conditions met, process NPC
            try {
                // Mark as processing
                currentlyProcessing[npcUuid] = true
                
                val success = npc.eventHandler.processLLM()
                if (success) {
                    lastSuccessfulTrigger[npcUuid] = currentTime
                    LogUtil.info("Successfully triggered LLM processing for NPC: ${npc.config.npcName}")
                } else {
                    LogUtil.debug("LLM processing returned false for NPC: ${npc.config.npcName}")
                    // If failed, clear processing flag immediately
                    currentlyProcessing.remove(npcUuid)
                }
                // Note: processing flag will be cleared when queue becomes empty (checked in next cycle)
                // Always move to end for circular rotation
                circularQueue.addLast(npcUuid)
                processed = true
            } catch (e: Exception) {
                // On error, clear processing flag and move to end
                currentlyProcessing.remove(npcUuid)
                LogUtil.error("Error processing LLM for NPC: ${npc.config.npcName}", e)
                circularQueue.addLast(npcUuid)
                attempts++
            }
        }

        if (!processed && attempts >= maxAttempts) {
            LogUtil.debug("Could not process any NPC in this cycle (all skipped)")
        }
    }
}

