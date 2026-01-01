package me.prskid1000.craftagent.event

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import me.sailex.altoclef.AltoClefController
import me.sailex.altoclef.tasks.LookAtOwnerTask
import me.prskid1000.craftagent.config.NPCConfig
import me.prskid1000.craftagent.constant.Instructions
import me.prskid1000.craftagent.context.ContextProvider
import me.prskid1000.craftagent.database.repositories.MessageRepository
import me.prskid1000.craftagent.database.repositories.SharebookRepository
import me.prskid1000.craftagent.history.ConversationHistory
import me.prskid1000.craftagent.history.Message
import me.prskid1000.craftagent.llm.LLMClient
import me.prskid1000.craftagent.model.database.Message as DatabaseMessage
import me.prskid1000.craftagent.model.database.SharebookPage
import me.prskid1000.craftagent.util.LogUtil
import me.prskid1000.craftagent.util.StructuredInputFormatter
import java.util.UUID
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
    private val messageRepository: MessageRepository,
    private val sharebookRepository: SharebookRepository
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
     * @deprecated Use updateState() instead. This now just calls updateState() for backward compatibility.
     */
    @Deprecated("Use updateState() instead", ReplaceWith("updateState(prompt)"))
    override fun onEvent(prompt: String) {
        updateState(prompt)
    }

    /**
     * Updates state only (store prompt in history) without triggering LLM.
     * Called when events/messages occur.
     */
    override fun updateState(prompt: String) {
        CompletableFuture.runAsync({
            LogUtil.info("updateState: $prompt")
            // Store only the original prompt in history (without context to avoid duplication)
            history.add(Message(prompt, "user"))
        }, executorService)
            .exceptionally {
                LogUtil.error("Error updating state: $prompt", it)
                null
            }
    }

    /**
     * Processes LLM call. Called by scheduler periodically.
     * Only triggers if queue is empty (checked by scheduler).
     * @return true if LLM call was successfully submitted, false otherwise
     */
    override fun processLLM(): Boolean {
        // Double-check queue is empty (scheduler also checks, but be safe)
        if (!executorService.queue.isEmpty()) {
            LogUtil.debug("processLLM: Queue not empty, skipping for NPC ${config.npcName}")
            return false
        }
        
        return try {
            CompletableFuture.runAsync({
                LogUtil.info("processLLM: Processing LLM for NPC ${config.npcName}")

                // Perform summarization if needed (this ensures it goes through scheduler)
                history.performSummarizationIfNeeded()

                // Build messages for LLM: history + current state with context
                val messagesForLLM = mutableListOf<Message>()
                // Add all history messages (without context)
                messagesForLLM.addAll(history.latestConversations)
                
                // If there's a last user message, replace it with formatted version that includes context
                if (messagesForLLM.isNotEmpty() && messagesForLLM.last().role == "user") {
                    val lastUserMessage = messagesForLLM.last().content
                    val formattedPrompt: String = StructuredInputFormatter.formatStructured(lastUserMessage, contextProvider.buildContext())
                    messagesForLLM[messagesForLLM.size - 1] = Message(formattedPrompt, "user")
                } else {
                    // No recent user message, create a context-only prompt
                    val contextPrompt = "Current state and context. What should I do?"
                    val formattedPrompt: String = StructuredInputFormatter.formatStructured(contextPrompt, contextProvider.buildContext())
                    messagesForLLM.add(Message(formattedPrompt, "user"))
                }
                
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
                            "sendMessage" -> {
                                handleSendMessage(toolCall)
                            }
                            "readMessage" -> {
                                handleReadMessage(toolCall)
                            }
                            "addOrUpdatePageToBook" -> {
                                handleAddOrUpdatePageToBook(toolCall)
                            }
                            "removePageFromBook" -> {
                                handleRemovePageFromBook(toolCall)
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
                        return@supplyAsync false
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
                    LogUtil.error("Error occurred processing LLM for NPC ${config.npcName}", it)
                    null
                }
            
            // Return true to indicate work was submitted successfully
            // Scheduler tracks processing state and will clear flag when queue is empty
            LogUtil.info("Submitted LLM processing task for NPC: ${config.npcName}")
            true
        } catch (e: Exception) {
            LogUtil.error("Error submitting LLM processing task for NPC ${config.npcName}", e)
            false
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
            // Store error in state, will be picked up by scheduler
            this.updateState(errorMessage)
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
            val world = contextProvider.getNpcEntity().world
            val entityById = world.getEntityById(entity.id())
            if (entityById != null) {
                return entityById.uuid
            }
        }
        
        return null
    }

    private fun handleAddOrUpdateInfo(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val infoType = args["infoType"]?.toString() ?: return
        val name = args["name"]?.toString() ?: return
        
        when (infoType) {
            "contact" -> {
                // Get or find contact UUID
                var contactUuid = findContactUuidByName(name)
                
                // If contact doesn't exist, try to find from nearby entities
                if (contactUuid == null) {
                    val nearbyEntities = contextProvider.buildContext().nearbyEntities()
                    val entity = nearbyEntities.firstOrNull { 
                        it.name().equals(name, ignoreCase = true) 
                    }
                    if (entity != null) {
                        val world = contextProvider.getNpcEntity().world
                        val entityById = world.getEntityById(entity.id())
                        if (entityById != null) {
                            contactUuid = entityById.uuid
                        }
                    }
                }
                
                if (contactUuid == null) {
                    LogUtil.debugInChat("Could not find contact: $name. Make sure they are visible in nearbyEntities.")
                    return
                }
                
                // Get existing contact to preserve values if not provided
                val existingContact = memoryManager.getContact(contactUuid)
                val relationship = args["relationship"]?.toString() ?: existingContact?.relationship ?: "neutral"
                val notes = args["notes"]?.toString() ?: existingContact?.notes ?: ""
                
                // Handle enmityLevel - if provided, set absolute value; otherwise preserve existing
                val enmityLevel = when (val enmity = args["enmityLevel"]) {
                    is Number -> enmity.toDouble().coerceIn(0.0, 1.0)
                    is String -> enmity.toDoubleOrNull()?.coerceIn(0.0, 1.0)
                    else -> null
                } ?: existingContact?.enmityLevel ?: 0.0
                
                // Handle friendshipLevel - if provided, set absolute value; otherwise preserve existing
                val friendshipLevel = when (val friendship = args["friendshipLevel"]) {
                    is Number -> friendship.toDouble().coerceIn(0.0, 1.0)
                    is String -> friendship.toDoubleOrNull()?.coerceIn(0.0, 1.0)
                    else -> null
                } ?: existingContact?.friendshipLevel ?: 0.0
                
                val contactType = existingContact?.contactType ?: run {
                    // Determine type from entity
                    val nearbyEntities = contextProvider.buildContext().nearbyEntities()
                    val entity = nearbyEntities.firstOrNull { 
                        it.name().equals(name, ignoreCase = true) 
                    }
                    if (entity != null) {
                        val world = contextProvider.getNpcEntity().world
                        val entityById = world.getEntityById(entity.id())
                        if (entityById != null) {
                            if (entityById.isPlayer) "player" else "npc"
                        } else "npc"
                    } else "npc"
                }
                
                memoryManager.addOrUpdateContact(
                    contactUuid,
                    name,
                    contactType,
                    relationship,
                    notes,
                    enmityLevel,
                    friendshipLevel
                )
                LogUtil.info("Added/updated contact: $name (relationship: $relationship, enmity: $enmityLevel, friendship: $friendshipLevel)")
            }
            "location" -> {
                val description = args["description"]?.toString() ?: ""
                val position = contextProvider.getNpcEntity().blockPos
                memoryManager.saveLocation(name, position, description)
                LogUtil.info("Saved/updated location: $name at ${position.x}, ${position.y}, ${position.z}")
            }
            else -> {
                LogUtil.debugInChat("Unknown infoType: $infoType. Use 'contact' or 'location'.")
            }
        }
    }

    private fun handleRemoveInfo(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val infoType = args["infoType"]?.toString() ?: return
        val name = args["name"]?.toString() ?: return
        
        when (infoType) {
            "contact" -> {
                val contact = memoryManager.getContacts().firstOrNull { 
                    it.contactName.equals(name, ignoreCase = true) 
                }
                if (contact != null) {
                    memoryManager.removeContact(contact.contactUuid)
                    LogUtil.info("Removed contact: $name")
                } else {
                    LogUtil.debugInChat("Could not find contact to remove: $name")
                }
            }
            "location" -> {
                val location = memoryManager.getLocation(name)
                if (location != null) {
                    memoryManager.deleteLocation(name)
                    LogUtil.info("Removed location: $name")
                } else {
                    LogUtil.debugInChat("Could not find location to remove: $name")
                }
            }
            else -> {
                LogUtil.debugInChat("Unknown infoType: $infoType. Use 'contact' or 'location'.")
            }
        }
    }

    private fun handleSendMessage(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val args = toolCall.arguments
        
        val recipientName = args["recipientName"]?.toString() ?: return
        val subject = args["subject"]?.toString() ?: return
        val content = args["content"]?.toString() ?: return
        
        // Find recipient UUID
        val recipientUuid = findContactUuidByName(recipientName)
        if (recipientUuid == null) {
            LogUtil.debugInChat("Could not find recipient: $recipientName. Make sure they are in your contacts or nearbyEntities.")
            return
        }
        
        // Get sender info
        val senderUuid = config.uuid
        val senderName = config.npcName
        val senderType = "npc"
        
        val message = DatabaseMessage(
            recipientUuid = recipientUuid,
            senderUuid = senderUuid,
            senderName = senderName,
            senderType = senderType,
            subject = subject,
            content = content
        )
        
        val maxMessages = contextProvider.getBaseConfig().getMaxMessages()
        messageRepository.insert(message, maxMessages)
        
        // Display message in chat like it used to be before
        val chatMessage = if (subject.isNotEmpty() && subject != content) {
            "$senderName says to $recipientName [$subject]: $content"
        } else {
            "$senderName says to $recipientName: $content"
        }
        controller.controllerExtras.chat(chatMessage)
        
        LogUtil.info("Sent message to $recipientName: $subject")
    }

    private fun handleReadMessage(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val args = toolCall.arguments
        
        val unreadOnly = when (val unread = args["unreadOnly"]) {
            is Boolean -> unread
            is String -> unread.toBoolean()
            else -> false
        }
        val limit = when (val lim = args["limit"]) {
            is Number -> lim.toInt().coerceIn(1, 50)
            is String -> lim.toIntOrNull()?.coerceIn(1, 50) ?: 10
            else -> 10
        }
        
        val messages = messageRepository.selectByRecipient(config.uuid, limit, unreadOnly)
        
        // Mark messages as read
        messages.forEach { message ->
            if (!message.read) {
                messageRepository.markAsRead(message.id)
            }
        }
        
        LogUtil.info("Read ${messages.size} message(s) (unreadOnly: $unreadOnly)")
        // Messages are included in context, so LLM can see them
    }

    private fun handleAddOrUpdatePageToBook(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val args = toolCall.arguments
        
        val pageTitle = args["pageTitle"]?.toString() ?: return
        val content = args["content"]?.toString() ?: return
        
        val page = SharebookPage(
            pageTitle = pageTitle,
            content = content,
            authorUuid = config.uuid.toString(),
            authorName = config.npcName
        )
        
        val maxSharebookPages = contextProvider.getBaseConfig().getMaxSharebookPages()
        sharebookRepository.insertOrUpdate(page, maxSharebookPages)
        LogUtil.info("Added/updated sharebook page: $pageTitle")
    }

    private fun handleRemovePageFromBook(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val args = toolCall.arguments
        
        val pageTitle = args["pageTitle"]?.toString() ?: return
        
        sharebookRepository.delete(pageTitle)
        LogUtil.info("Removed sharebook page: $pageTitle")
    }

}