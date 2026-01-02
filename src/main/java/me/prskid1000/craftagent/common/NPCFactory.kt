package me.prskid1000.craftagent.common

import me.prskid1000.craftagent.config.ConfigProvider
import me.prskid1000.craftagent.config.NPCConfig
import me.prskid1000.craftagent.constant.Instructions
import me.prskid1000.craftagent.context.ContextProvider
import me.prskid1000.craftagent.database.repositories.ContactRepository
import me.prskid1000.craftagent.database.repositories.LocationMemoryRepository
import me.prskid1000.craftagent.database.repositories.MessageRepository
import me.prskid1000.craftagent.database.repositories.SharebookRepository
import me.prskid1000.craftagent.event.NPCEventHandler
import me.prskid1000.craftagent.exception.NPCCreationException
import me.prskid1000.craftagent.history.ConversationHistory
import me.prskid1000.craftagent.history.Message
import me.prskid1000.craftagent.llm.LLMClient
import me.prskid1000.craftagent.llm.LLMType
import me.prskid1000.craftagent.llm.ollama.OllamaClient
import me.prskid1000.craftagent.llm.lmstudio.LMStudioClient
import me.prskid1000.craftagent.memory.MemoryManager
import me.prskid1000.craftagent.model.NPC
import me.prskid1000.craftagent.model.database.Conversation
import net.minecraft.server.network.ServerPlayerEntity

class NPCFactory(
    private val configProvider: ConfigProvider,
    private val locationRepository: LocationMemoryRepository,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val sharebookRepository: SharebookRepository
) {
     fun createNpc(npcEntity: ServerPlayerEntity, config: NPCConfig, loadedConversation: List<Conversation>?): NPC {
        val baseConfig = configProvider.baseConfig
        val contextProvider = ContextProvider(npcEntity, baseConfig)
        
        // Create memory manager
        val memoryManager = MemoryManager(locationRepository, contactRepository, config.uuid, baseConfig)
        contextProvider.memoryManager = memoryManager
        
        // Set repositories for mail and sharebook
        contextProvider.setRepositories(messageRepository, sharebookRepository, config.uuid)

        val llmClient = initLLMClient(config)

        // Always use default prompt, append custom prompt if provided
        // Get commands from Brigadier instead of AltoClef
        val server = npcEntity.server
        val minecraftCommands = me.prskid1000.craftagent.util.MinecraftCommandUtil.getFormattedCommandList(server)
        val defaultPrompt = Instructions.getLlmSystemPrompt(
            config.npcName,
            config.age,
            config.gender,
            minecraftCommands,
            config.customSystemPrompt,
            config.llmType
        )

        val messages = loadedConversation
            ?.map { Message(it.message, it.role) }
            ?.toMutableList() ?: mutableListOf()
        val history = ConversationHistory(llmClient, defaultPrompt, messages, baseConfig.conversationHistoryLength)
        val eventHandler = NPCEventHandler(llmClient, history, contextProvider, config, messageRepository, sharebookRepository)
        return NPC(npcEntity, llmClient, history, eventHandler, contextProvider, config)
    }

    private fun initLLMClient(config: NPCConfig): LLMClient {
        val baseConfig = configProvider.baseConfig
        me.prskid1000.craftagent.util.LogUtil.info("initLLMClient: NPC=${config.npcName}, llmType=${config.llmType}, ollamaUrl=${config.ollamaUrl}, lmStudioUrl=${config.lmStudioUrl}")
        val llmClient = when (config.llmType) {
            LLMType.OLLAMA -> {
                me.prskid1000.craftagent.util.LogUtil.info("Creating OllamaClient for NPC: ${config.npcName}")
                OllamaClient(config.llmModel, config.ollamaUrl, baseConfig.llmTimeout, baseConfig.isVerbose)
            }
            LLMType.LM_STUDIO -> {
                me.prskid1000.craftagent.util.LogUtil.info("Creating LMStudioClient for NPC: ${config.npcName}")
                LMStudioClient(config.llmModel, config.lmStudioUrl, baseConfig.llmTimeout)
            }
            else -> throw NPCCreationException("Invalid LLM type: ${config.llmType}")
        }
        // Note: Health check is done in NPCService.createNpc() before spawning to avoid blocking server thread
        return llmClient
    }
    
    /**
     * Check if the LLM service is reachable. This should be called on a background thread,
     * not on the Minecraft server thread.
     */
    fun checkLLMServiceReachable(config: NPCConfig) {
        val baseConfig = configProvider.baseConfig
        me.prskid1000.craftagent.util.LogUtil.info("checkLLMServiceReachable: NPC=${config.npcName}, llmType=${config.llmType}, ollamaUrl=${config.ollamaUrl}, lmStudioUrl=${config.lmStudioUrl}")
        val llmClient = when (config.llmType) {
            LLMType.OLLAMA -> {
                me.prskid1000.craftagent.util.LogUtil.info("Creating OllamaClient for NPC: ${config.npcName}")
                OllamaClient(config.llmModel, config.ollamaUrl, baseConfig.llmTimeout, baseConfig.isVerbose)
            }
            LLMType.LM_STUDIO -> {
                me.prskid1000.craftagent.util.LogUtil.info("Creating LMStudioClient for NPC: ${config.npcName}")
                LMStudioClient(config.llmModel, config.lmStudioUrl, baseConfig.llmTimeout)
            }
            else -> throw NPCCreationException("Invalid LLM type: ${config.llmType}")
        }
        llmClient.checkServiceIsReachable()
    }

}
