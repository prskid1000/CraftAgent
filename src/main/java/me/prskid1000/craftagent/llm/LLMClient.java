package me.prskid1000.craftagent.llm;

import me.prskid1000.craftagent.history.Message;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public interface LLMClient {

    /**
     * Lets the LLM generate a response with tool calling support.
     * Commands are executed via tool calls, messages use structured output.
     * Uses tools with command information from the server.
     * 
     * @param messages conversationHistory
     * @param server The Minecraft server instance (for getting command information)
     * @return ToolCallResponse containing tool calls and/or content
     */
    ToolCallResponse chatWithTools(List<Message> messages, MinecraftServer server);
    
    /**
     * Lets the LLM generate a response with tool calling support (without server - fallback).
     * Uses generic tool definitions without command information.
     * 
     * @param messages conversationHistory
     * @return ToolCallResponse containing tool calls and/or content
     */
    default ToolCallResponse chatWithTools(List<Message> messages) {
        return chatWithTools(messages, null);
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
