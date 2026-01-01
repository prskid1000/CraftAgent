package me.prskid1000.craftagent.common

import me.sailex.altoclef.AltoClefController
import me.sailex.automatone.api.BaritoneAPI
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

        val controller = initController(npcEntity)
        // Always use default prompt, append custom prompt if provided
        val defaultPrompt = Instructions.getLlmSystemPrompt(
            config.npcName,
            config.age,
            config.gender,
            controller.commandExecutor.allCommands(),
            config.customSystemPrompt,
            config.llmType
        )

        val messages = loadedConversation
            ?.map { Message(it.message, it.role) }
            ?.toMutableList() ?: mutableListOf()
        val history = ConversationHistory(llmClient, defaultPrompt, messages, baseConfig.conversationHistoryLength)
        val eventHandler = NPCEventHandler(llmClient, history, contextProvider, controller, config, messageRepository, sharebookRepository)
        return NPC(npcEntity, llmClient, history, eventHandler, controller, contextProvider, config)
    }

    private fun initController(npcEntity: ServerPlayerEntity): AltoClefController {
        val automatone = BaritoneAPI.getProvider().getBaritone(npcEntity)
        return AltoClefController(automatone)
    }

    private fun initLLMClient(config: NPCConfig): LLMClient {
        val baseConfig = configProvider.baseConfig
        val llmClient = when (config.llmType) {
            LLMType.OLLAMA -> OllamaClient(config.llmModel, config.ollamaUrl, baseConfig.llmTimeout, baseConfig.isVerbose)
            LLMType.LM_STUDIO -> LMStudioClient(config.llmModel, config.lmStudioUrl, baseConfig.llmTimeout)
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
        val llmClient = when (config.llmType) {
            LLMType.OLLAMA -> OllamaClient(config.llmModel, config.ollamaUrl, baseConfig.llmTimeout, baseConfig.isVerbose)
            LLMType.LM_STUDIO -> LMStudioClient(config.llmModel, config.lmStudioUrl, baseConfig.llmTimeout)
            else -> throw NPCCreationException("Invalid LLM type: ${config.llmType}")
        }
        llmClient.checkServiceIsReachable()
    }

}
