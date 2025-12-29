package me.prskid1000.craftagent.model

import me.sailex.altoclef.AltoClefController
import me.prskid1000.craftagent.config.NPCConfig
import me.prskid1000.craftagent.context.ContextProvider
import me.prskid1000.craftagent.event.EventHandler
import me.prskid1000.craftagent.history.ConversationHistory
import me.prskid1000.craftagent.llm.LLMClient
import net.minecraft.server.network.ServerPlayerEntity

data class NPC(
    val entity: ServerPlayerEntity,
    val llmClient: LLMClient,
    val history: ConversationHistory,
    val eventHandler: EventHandler,
    val controller: AltoClefController,
    val contextProvider: ContextProvider,
    val config: NPCConfig
)