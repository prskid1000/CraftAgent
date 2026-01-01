package me.prskid1000.craftagent.common

import me.sailex.altoclef.multiversion.EntityVer
import me.prskid1000.craftagent.auth.UsernameValidator
import me.prskid1000.craftagent.callback.NPCEvents
import me.prskid1000.craftagent.config.ConfigProvider
import me.prskid1000.craftagent.config.NPCConfig
import me.prskid1000.craftagent.constant.Instructions
import me.prskid1000.craftagent.database.resources.ResourceProvider
import me.prskid1000.craftagent.exception.NPCCreationException
import me.prskid1000.craftagent.model.NPC
import me.prskid1000.craftagent.util.LogUtil
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.PlayerManager
import net.minecraft.util.math.BlockPos
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NPCService(
    private val factory: NPCFactory,
    private val configProvider: ConfigProvider,
    private val resourceProvider: ResourceProvider
) {
    companion object {
        private const val MAX_NUMBER_OF_NPC = 10
    }

    private lateinit var executorService: ExecutorService
    val uuidToNpc = ConcurrentHashMap<UUID, NPC>()
    private val entityUuidToConfigUuid = ConcurrentHashMap<UUID, UUID>()
    private var deathEventRegistered = false

    fun init(server: MinecraftServer) {
        executorService = Executors.newSingleThreadExecutor()
        registerDeathEvent()
        respawnActiveNPCs(server)
    }

    private fun registerDeathEvent() {
        if (!deathEventRegistered) {
            NPCEvents.ON_DEATH.register { entity ->
                val configUuid = entityUuidToConfigUuid[entity.uuid]
                if (configUuid != null) {
                    removeNpc(configUuid, EntityVer.getWorld(entity).server!!.playerManager)
                } else {
                    val npc = uuidToNpc.values.firstOrNull { it.entity.uuid == entity.uuid }
                    if (npc != null) {
                        removeNpc(npc.config.uuid, EntityVer.getWorld(entity).server!!.playerManager)
                    }
                }
            }
            deathEventRegistered = true
        }
    }

    private fun respawnActiveNPCs(server: MinecraftServer) {
        configProvider.getNpcConfigs().forEach { config ->
            if (config.isActive && !uuidToNpc.containsKey(config.uuid)) {
                createNpc(config, server, null, null)
            }
        }
    }

    fun createNpc(newConfig: NPCConfig, server: MinecraftServer, spawnPos: BlockPos?, owner: PlayerEntity?) {
        CompletableFuture.runAsync({
            val name = newConfig.npcName
            checkLimit()
            checkNpcName(name)

            val config = updateConfig(newConfig)
            
            // Check LLM service reachability BEFORE spawning entity (on background thread, not server thread)
            try {
                LogUtil.info("Checking LLM service reachability for NPC: $name")
                factory.checkLLMServiceReachable(config)
                LogUtil.info("LLM service is reachable for NPC: $name")
            } catch (e: Exception) {
                LogUtil.error("LLM service is not reachable for NPC: $name", e)
                LogUtil.errorInChat("Failed to create NPC '$name': LLM service is not reachable. ${e.message}")
                val npcConfig = configProvider.getNpcConfig(config.uuid)
                if (npcConfig.isPresent) {
                    npcConfig.get().isActive = false
                }
                return@runAsync
            }

            NPCSpawner.spawn(config, server, spawnPos) { npcEntity ->
                server.execute {
                    try {
                        config.uuid = npcEntity.uuid
                        val npc = factory.createNpc(npcEntity, config, resourceProvider.loadedConversations[config.uuid])
                        npc.controller.owner = owner
                        uuidToNpc[config.uuid] = npc
                        entityUuidToConfigUuid[npcEntity.uuid] = config.uuid

                        LogUtil.infoInChat("Added NPC with name: $name")
                        npc.eventHandler.onEvent(Instructions.INITIAL_PROMPT)
                    } catch (e: Exception) {
                        LogUtil.error("Error creating NPC: $name", e)
                        LogUtil.errorInChat("Failed to initialize NPC: ${e.message}")
                        NPCSpawner.remove(npcEntity.uuid, server.playerManager)
                        val npcConfig = configProvider.getNpcConfig(config.uuid)
                        if (npcConfig.isPresent) {
                            npcConfig.get().isActive = false
                        }
                    }
                }
            }
        }, executorService).exceptionally {
            LogUtil.errorInChat(it.message)
            LogUtil.error(it)
            null
        }
    }

    fun removeNpc(uuid: UUID, playerManager: PlayerManager) {
        val npcToRemove = uuidToNpc[uuid]
        if (npcToRemove != null) {
            val server = playerManager.server
            val entityUuid = npcToRemove.entity.uuid
            
            server.execute {
                try {
                    npcToRemove.controller.stop()
                    npcToRemove.llmClient.stopService()
                    npcToRemove.eventHandler.stopService()
                    npcToRemove.contextProvider.chunkManager.stopService()
                    resourceProvider.addConversations(uuid, npcToRemove.history.latestConversations)
                    uuidToNpc.remove(uuid)
                    entityUuidToConfigUuid.remove(entityUuid)

                    NPCSpawner.remove(entityUuid, playerManager)

                    val config = configProvider.getNpcConfig(uuid)
                    if (config.isPresent) {
                        config.get().isActive = false
                        LogUtil.infoInChat("Removed NPC with name ${config.get().npcName}")
                    } else {
                        LogUtil.infoInChat("Removed NPC with uuid $uuid")
                    }
                } catch (e: Exception) {
                    LogUtil.error("Error removing NPC: $uuid", e)
                }
            }
        }
    }

    fun deleteNpc(uuid: UUID, playerManager: PlayerManager) {
        val npcToDelete = uuidToNpc[uuid]
        if (npcToDelete != null) {
            val server = playerManager.server
            val entityUuid = npcToDelete.entity.uuid
            
            // Stop services and remove from maps on server thread (thread-safe operations only)
            server.execute {
                try {
                    npcToDelete.controller.stop()
                    npcToDelete.llmClient.stopService()
                    npcToDelete.eventHandler.stopService()
                    npcToDelete.contextProvider.chunkManager.stopService()
                    
                    resourceProvider.loadedConversations.remove(uuid)
                    uuidToNpc.remove(uuid)
                    entityUuidToConfigUuid.remove(entityUuid)
                    
                    NPCSpawner.remove(entityUuid, playerManager)
                    
                    LogUtil.infoInChat("Deleted NPC with uuid $uuid")
                } catch (e: Exception) {
                    LogUtil.error("Error deleting NPC: $uuid", e)
                }
            }
            
            // Database operations on background thread (blocking I/O)
            CompletableFuture.runAsync({
                try {
                    resourceProvider.conversationRepository.deleteByUuid(uuid)
                    configProvider.deleteNpcConfig(uuid)
                } catch (e: Exception) {
                    LogUtil.error("Error deleting NPC data from database: $uuid", e)
                }
            }, executorService)
        } else {
            // NPC not in map, clean up data on background thread
            CompletableFuture.runAsync({
                try {
                    resourceProvider.loadedConversations.remove(uuid)
                    resourceProvider.conversationRepository.deleteByUuid(uuid)
                    configProvider.deleteNpcConfig(uuid)
                } catch (e: Exception) {
                    LogUtil.error("Error cleaning up NPC data: $uuid", e)
                }
            }, executorService)
        }
    }

    fun shutdownNPCs(server: MinecraftServer) {
        uuidToNpc.keys.forEach {
            removeNpc(it, server.playerManager)
        }
        executorService.shutdownNow()
    }

    private fun updateConfig(newConfig: NPCConfig): NPCConfig {
        val config = configProvider.getNpcConfigByName(newConfig.npcName)
        if (config.isEmpty) {
            return configProvider.addNpcConfig(newConfig)
        } else {
            config.get().isActive = true
            return config.get()
        }
    }

    private fun checkNpcName(npcName: String) {
        if (!UsernameValidator.isValid(npcName)) {
            throw NPCCreationException("NPC name is not valid. Use 3–16 characters: letters, numbers, or underscores only.")
        } else if (uuidToNpc.values.any { it.entity.name.string == npcName }) {
            throw NPCCreationException("A NPC with the name '$npcName' already exists.")
        }
    }

    private fun checkLimit() {
        if (uuidToNpc.size == MAX_NUMBER_OF_NPC) {
            throw NPCCreationException("Currently there are no more than" + MAX_NUMBER_OF_NPC +" parallel running " +
                    "NPCs supported!")
        }
    }

}