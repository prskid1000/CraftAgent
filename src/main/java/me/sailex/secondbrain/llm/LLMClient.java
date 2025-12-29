package me.sailex.secondbrain.llm;

import me.sailex.secondbrain.history.Message;

import java.util.List;

public interface LLMClient {

    /**
     * Lets the LLM generate a response with tool calling support.
     * Commands are executed via tool calls, messages use structured output.
     * @param messages conversationHistory
     * @return ToolCallResponse containing tool calls and/or content
     */
    ToolCallResponse chatWithTools(List<Message> messages);

    /**
     * Check if the service is reachable
     */
    void checkServiceIsReachable();

	/**
	 * Stops the Executor service
	 */
	void stopService();
}
