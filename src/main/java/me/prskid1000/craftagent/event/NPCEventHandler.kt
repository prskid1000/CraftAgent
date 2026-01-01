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
            
            // Process tool calls (commands and memory management)
            var command: String? = null
            if (toolResponse.hasToolCalls()) {
                // Handle memory management tools first
                toolResponse.toolCalls.forEach { toolCall ->
                    when (toolCall.name) {
                        "execute_command" -> {
                            command = toolCall.getCommand()
                        }
                        "addOrUpdateInfo" -> {
                            handleAddOrUpdateInfo(toolCall)
                        }
                        "removeInfo" -> {
                            handleRemoveInfo(toolCall)
                        }
                    }
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

    private fun handleAddContact(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val contactName = args["contactName"]?.toString() ?: return
        val relationship = args["relationship"]?.toString() ?: "neutral"
        val notes = args["notes"]?.toString() ?: ""
        val initialEnmity = when (val enmity = args["initialEnmity"]) {
            is Number -> enmity.toDouble().coerceIn(0.0, 1.0)
            is String -> enmity.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.0
            else -> 0.0
        }
        val initialFriendship = when (val friendship = args["initialFriendship"]) {
            is Number -> friendship.toDouble().coerceIn(0.0, 1.0)
            is String -> friendship.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.0
            else -> 0.0
        }
        
        // Find the entity from nearby entities in context
        val nearbyEntities = contextProvider.buildContext().nearbyEntities()
        val entity = nearbyEntities.firstOrNull { 
            it.name().equals(contactName, ignoreCase = true) 
        }
        
        if (entity != null) {
            val world = contextProvider.getNpcEntity().world
            val entityById = world.getEntityById(entity.id())
            if (entityById != null) {
                val contactType = if (entityById.isPlayer) "player" else "npc"
                memoryManager.addOrUpdateContact(
                    entityById.uuid,
                    contactName,
                    contactType,
                    relationship,
                    notes,
                    initialEnmity,
                    initialFriendship
                )
                LogUtil.info("Added contact: $contactName (type: $contactType, relationship: $relationship)")
            } else {
                LogUtil.debugInChat("Could not find entity in world: $contactName")
            }
        } else {
            LogUtil.debugInChat("Contact not found in nearby entities: $contactName. Make sure they are visible in your context.")
        }
    }

    /**
     * Finds a contact UUID by name from nearby entities or existing contacts
     */
    private fun findContactUuidByName(contactName: String): java.util.UUID? {
        val memoryManager = contextProvider.memoryManager ?: return null
        
        // First, check existing contacts
        val existingContact = memoryManager.getContacts().firstOrNull { 
            it.contactName.equals(contactName, ignoreCase = true) 
        }
        if (existingContact != null) {
            return existingContact.contactUuid
        }
        
        // Then check nearby entities
        val nearbyEntities = contextProvider.buildContext().nearbyEntities()
        val entity = nearbyEntities.firstOrNull { 
            it.name().equals(contactName, ignoreCase = true) 
        }
        if (entity != null) {
            // Try to find the actual entity in the world to get its UUID
            val world = contextProvider.npcEntity.world
            val entityById = world.getEntityById(entity.id())
            if (entityById != null) {
                return entityById.uuid
            }
        }
        
        return null
    }

    private fun handleUpdateContactRelationship(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val contactName = args["contactName"]?.toString() ?: return
        val relationship = args["relationship"]?.toString() ?: return
        
        val contactUuid = findContactUuidByName(contactName)
        if (contactUuid != null) {
            memoryManager.updateContactRelationship(contactUuid, relationship)
            LogUtil.info("Updated relationship with $contactName to $relationship")
        } else {
            LogUtil.debugInChat("Could not find contact: $contactName")
        }
    }

    private fun handleUpdateContactEnmity(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val contactName = args["contactName"]?.toString() ?: return
        val enmityChange = when (val change = args["enmityChange"]) {
            is Number -> change.toDouble()
            is String -> change.toDoubleOrNull() ?: return
            else -> return
        }
        
        val contactUuid = findContactUuidByName(contactName)
        if (contactUuid != null) {
            memoryManager.updateContactEnmity(contactUuid, enmityChange)
            LogUtil.info("Updated enmity with $contactName by $enmityChange")
        } else {
            // If contact doesn't exist, create it first
            val nearbyEntities = contextProvider.buildContext().nearbyEntities()
            val entity = nearbyEntities.firstOrNull { 
                it.name().equals(contactName, ignoreCase = true) 
            }
            if (entity != null) {
                val world = contextProvider.getNpcEntity().world
                val entityById = world.getEntityById(entity.id())
                if (entityById != null) {
                    val contactType = if (entityById.isPlayer) "player" else "npc"
                    val initialEnmity = enmityChange.coerceIn(0.0, 1.0)
                    memoryManager.addOrUpdateContact(
                        entityById.uuid,
                        contactName,
                        contactType,
                        "neutral",
                        "",
                        initialEnmity,
                        0.0
                    )
                    LogUtil.info("Created contact $contactName with enmity $initialEnmity")
                }
            }
        }
    }

    private fun handleUpdateContactFriendship(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val contactName = args["contactName"]?.toString() ?: return
        val friendshipChange = when (val change = args["friendshipChange"]) {
            is Number -> change.toDouble()
            is String -> change.toDoubleOrNull() ?: return
            else -> return
        }
        
        val contactUuid = findContactUuidByName(contactName)
        if (contactUuid != null) {
            memoryManager.updateContactFriendship(contactUuid, friendshipChange)
            LogUtil.info("Updated friendship with $contactName by $friendshipChange")
        } else {
            // If contact doesn't exist, create it first
            val nearbyEntities = contextProvider.buildContext().nearbyEntities()
            val entity = nearbyEntities.firstOrNull { 
                it.name().equals(contactName, ignoreCase = true) 
            }
            if (entity != null) {
                val world = contextProvider.getNpcEntity().world
                val entityById = world.getEntityById(entity.id())
                if (entityById != null) {
                    val contactType = if (entityById.isPlayer) "player" else "npc"
                    val initialFriendship = friendshipChange.coerceIn(0.0, 1.0)
                    memoryManager.addOrUpdateContact(
                        entityById.uuid,
                        contactName,
                        contactType,
                        "neutral",
                        "",
                        0.0,
                        initialFriendship
                    )
                    LogUtil.info("Created contact $contactName with friendship $initialFriendship")
                }
            }
        }
    }

    private fun handleSaveLocation(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val locationName = args["locationName"]?.toString() ?: return
        val description = args["description"]?.toString() ?: ""
        
        val position = contextProvider.getNpcEntity().blockPos
        memoryManager.saveLocation(locationName, position, description)
        LogUtil.info("Saved location: $locationName at ${position.x}, ${position.y}, ${position.z}")
    }

    private fun handleRemoveContact(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val contactName = args["contactName"]?.toString() ?: return
        
        val contact = memoryManager.getContacts().firstOrNull { 
            it.contactName.equals(contactName, ignoreCase = true) 
        }
        if (contact != null) {
            memoryManager.removeContact(contact.contactUuid)
            LogUtil.info("Removed contact: $contactName")
        } else {
            LogUtil.debugInChat("Could not find contact to remove: $contactName")
        }
    }

    private fun handleRemoveLocation(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val locationName = args["locationName"]?.toString() ?: return
        
        val location = memoryManager.getLocation(locationName)
        if (location != null) {
            memoryManager.deleteLocation(locationName)
            LogUtil.info("Removed location: $locationName")
        } else {
            LogUtil.debugInChat("Could not find location to remove: $locationName")
        }
    }

}