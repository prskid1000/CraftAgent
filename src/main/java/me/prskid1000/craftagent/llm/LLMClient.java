package me.prskid1000.craftagent.llm;

import me.prskid1000.craftagent.history.ConversationMessage;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public interface LLMClient {

    /**
     * Lets the LLM generate a response.
     * Uses structured output for messages.
     * 
     * @param messages conversationHistory
     * @param server The Minecraft server instance
     * @return LLMResponse containing content
     */
    LLMResponse chat(List<ConversationMessage> messages, MinecraftServer server);
    
    /**
     * Lets the LLM generate a response (without server - fallback).
     * 
     * @param messages conversationHistory
     * @return LLMResponse containing content
     */
    default LLMResponse chat(List<ConversationMessage> messages) {
        return chat(messages, null);
    }

    /**
     * Check if the service is reachable
     */
    void checkServiceIsReachable();

	/**
	 * Stops the Executor service
	 */
	void stopService();
}
