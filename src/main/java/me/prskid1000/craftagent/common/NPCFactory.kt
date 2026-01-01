package me.prskid1000.craftagent.common

import me.sailex.altoclef.AltoClefController
import me.sailex.automatone.api.BaritoneAPI
import me.prskid1000.craftagent.config.ConfigProvider
import me.prskid1000.craftagent.config.NPCConfig
import me.prskid1000.craftagent.constant.Instructions
import me.prskid1000.craftagent.context.ContextProvider
import me.prskid1000.craftagent.event.NPCEventHandler
import me.prskid1000.craftagent.exception.NPCCreationException
import me.prskid1000.craftagent.history.ConversationHistory
import me.prskid1000.craftagent.history.Message
import me.prskid1000.craftagent.llm.LLMClient
import me.prskid1000.craftagent.llm.LLMType
import me.prskid1000.craftagent.llm.ollama.OllamaClient
import me.prskid1000.craftagent.llm.lmstudio.LMStudioClient
import me.prskid1000.craftagent.model.NPC
import me.prskid1000.craftagent.model.database.Conversation
import net.minecraft.server.network.ServerPlayerEntity

class NPCFactory(
    private val configProvider: ConfigProvider,
) {
     fun createNpc(npcEntity: ServerPlayerEntity, config: NPCConfig, loadedConversation: List<Conversation>?): NPC {
        val baseConfig = configProvider.baseConfig
        val contextProvider = ContextProvider(npcEntity, baseConfig)

        val llmClient = initLLMClient(config)

        val controller = initController(npcEntity)
        val defaultPrompt = Instructions.getLlmSystemPrompt(config.npcName,
            config.llmCharacter,
            controller.commandExecutor.allCommands(),
            config.llmType)

        val messages = loadedConversation
            ?.map { Message(it.message, it.role) }
            ?.toMutableList() ?: mutableListOf()
        val history = ConversationHistory(llmClient, defaultPrompt, messages, baseConfig.conversationHistoryLength)
        val eventHandler = NPCEventHandler(llmClient, history, contextProvider, controller, config)
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
