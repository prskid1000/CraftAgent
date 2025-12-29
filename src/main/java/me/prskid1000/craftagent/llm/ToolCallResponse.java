package me.prskid1000.craftagent.llm;

import java.util.List;
import java.util.Map;

/**
 * Response from LLM that may contain tool calls and/or content.
 * Used for hybrid approach: tool calls for commands, structured output for messages.
 */
public class ToolCallResponse {
    private final String content;  // Chat message (structured output or plain text)
    private final List<ToolCall> toolCalls;  // Tool calls for command execution

    public ToolCallResponse(String content, List<ToolCall> toolCalls) {
        this.content = content != null ? content : "";
        this.toolCalls = toolCalls != null ? toolCalls : List.of();
    }

    public String getContent() {
        return content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    /**
     * Represents a tool call from the LLM.
     */
    public static class ToolCall {
        private final String id;
        private final String name;
        private final Map<String, Object> arguments;

        public ToolCall(String id, String name, Map<String, Object> arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public String getCommand() {
            if ("execute_command".equals(name) && arguments != null) {
                Object cmd = arguments.get("command");
                return cmd != null ? cmd.toString() : null;
            }
            return null;
        }
    }
}

