package me.prskid1000.craftagent.event

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import me.sailex.altoclef.AltoClefController
import me.sailex.altoclef.tasks.LookAtOwnerTask
import me.prskid1000.craftagent.config.NPCConfig
import me.prskid1000.craftagent.constant.Instructions
import me.prskid1000.craftagent.context.ContextProvider
import me.prskid1000.craftagent.history.ConversationHistory
import me.prskid1000.craftagent.history.Message
import me.prskid1000.craftagent.llm.LLMClient
import me.prskid1000.craftagent.util.LogUtil
import me.prskid1000.craftagent.util.StructuredInputFormatter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class NPCEventHandler(
    private val llmClient: LLMClient,
    private val history: ConversationHistory,
    private val contextProvider: ContextProvider,
    private val controller: AltoClefController,
    private val config: NPCConfig,
): EventHandler {
    companion object {
        private val gson = GsonBuilder()
            .setLenient()
            .create()
    }

    private val executorService: ThreadPoolExecutor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(10),
        ThreadPoolExecutor.DiscardPolicy()
    )

    /**
     * Processes an event asynchronously by allowing call actions from llm using the specified prompt.
     * Saves the prompt and responses in conversation history.
     *
     * @param prompt prompt of a user or system e.g. chatmessage of a player
     */
    override fun onEvent(prompt: String) {
        CompletableFuture.runAsync({
            LogUtil.info("onEvent: $prompt")

            // Store only the original prompt in history (without context to avoid duplication)
            history.add(Message(prompt, "user"))
            
            // Build messages for LLM: history + current message with context
            val messagesForLLM = mutableListOf<Message>()
            // Add all history messages (without context)
            messagesForLLM.addAll(history.latestConversations)
            // Replace the last message (current user message) with formatted version that includes context
            val formattedPrompt: String = StructuredInputFormatter.formatStructured(prompt, contextProvider.buildContext())
            messagesForLLM[messagesForLLM.size - 1] = Message(formattedPrompt, "user")
            
            // Use tool calling for commands, structured output for messages (hybrid approach)
            val toolResponse = llmClient.chatWithTools(messagesForLLM)
            
            // Extract command from tool calls
            var command: String? = null
            if (toolResponse.hasToolCalls()) {
                val executeCommandCall = toolResponse.toolCalls.firstOrNull { it.name == "execute_command" }
                if (executeCommandCall != null) {
                    command = executeCommandCall.getCommand()
                }
            }
            
            // Extract message from content (structured output or plain text)
            var message = toolResponse.content.trim()
            // If content is JSON (structured output), try to parse it
            if (message.startsWith("{") && message.contains("\"message\"")) {
                try {
                    val parsedMessage = parse(message)
                    message = parsedMessage.message
                    // If no command from tool call, try to get from parsed JSON (fallback for compatibility)
                    if (command == null && parsedMessage.command.isNotEmpty()) {
                        command = parsedMessage.command
                    }
                } catch (e: Exception) {
                    // If parsing fails, try to extract message field directly
                    try {
                        val messageStart = message.indexOf("\"message\"")
                        if (messageStart >= 0) {
                            val valueStart = message.indexOf("\"", messageStart + 10) + 1
                            val valueEnd = message.indexOf("\"", valueStart)
                            if (valueEnd > valueStart) {
                                message = message.substring(valueStart, valueEnd)
                            }
                        }
                    } catch (e2: Exception) {
                        // If all parsing fails, use content as-is (might be plain text)
                    }
                }
            }
            // If message is empty or just whitespace, treat as no message
            if (message.isBlank()) {
                message = ""
            }
            
            // Execute command if present
            if (command != null && command.isNotEmpty() && command != "idle") {
                val succeeded = execute(command)
                if (!succeeded) {
                    return@runAsync
                }
            }
            
            // Send message if present and different from last
            if (message.isNotEmpty() && message != history.getLastMessage()) {
                controller.controllerExtras.chat(message)
            }
            
            // Add response to history (for tool calls or messages)
            val responseText = when {
                message.isNotEmpty() -> message
                command != null -> "Executed command: $command"
                else -> toolResponse.content.ifEmpty { "No action" }
            }
            history.add(Message(responseText, "assistant"))
        }, executorService)
            .exceptionally {
                LogUtil.debugInChat("Could not generate a response: " + buildErrorMessage(it))
                LogUtil.error("Error occurred handling event: $prompt", it)
                null
            }
    }

    override fun stopService() {
        executorService.shutdownNow()
    }

    override fun queueIsEmpty(): Boolean {
        return executorService.queue.isEmpty()
    }

    //TODO: refactor this into own class
    private fun parse(content: String): CommandMessage {
        return try {
            parseContent(content)
        } catch (_: JsonParseException) {
            val cleanedContent = content
                .replace("```json", "")
                .replace("```", "")
            try {
                parseContent(cleanedContent)
            } catch (e: JsonParseException) {
                throw CustomEventException("The selected model may be too small to understand the context or to reliably produce valid JSON. " +
                        "Please switch to a larger or more capable LLM model.", e)
            }
        }
    }

    private fun parseContent(content: String): CommandMessage {
        return gson.fromJson(content, CommandMessage::class.java)
    }

    fun execute(command: String): Boolean {
        var successful = true
        val cmdExecutor = controller.commandExecutor
        val commandWithPrefix = if (cmdExecutor.isClientCommand(command)) {
            command
        } else {
            cmdExecutor.commandPrefix + command
        }
        cmdExecutor.execute(commandWithPrefix, {
            controller.runUserTask(LookAtOwnerTask())
//            if (queueIsEmpty()) {
//                //this.onEvent(Instructions.COMMAND_FINISHED_PROMPT.format(commandWithPrefix))
//            }
        }, {
            successful = false
            val errorMessage = if (it.message?.contains("does not exist") == true || it.message?.contains("Invalid command") == true) {
                val availableCommands = cmdExecutor.allCommands().joinToString(", ") { cmd -> cmd.name }
                "Command '%s' does not exist. Error: %s. You MUST use ONLY these available commands: %s. Do not invent new commands like 'build'. Use 'mine', 'craft', and other commands from the list instead.".format(
                    commandWithPrefix, it.message ?: "Unknown error", availableCommands
                )
            } else {
                Instructions.COMMAND_ERROR_PROMPT.format(commandWithPrefix, it.message ?: "Unknown error")
            }
            this.onEvent(errorMessage)
            LogUtil.error("Error executing command: $commandWithPrefix", it)
        })
        return successful
    }

    data class CommandMessage(
        val command: String,
        val message: String
    )

    private fun buildErrorMessage(exception: Throwable): String? {
        val chain = generateSequence(exception) { it.cause }
        val custom = chain.filterIsInstance<CustomEventException>().firstOrNull()
        if (custom != null) {
            return custom.message
        }
        return generateSequence(exception) { it.cause }.last().message
    }

}