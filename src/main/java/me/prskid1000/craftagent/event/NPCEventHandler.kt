package me.prskid1000.craftagent.event

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
import me.prskid1000.craftagent.util.CommandMessageParser
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
    private val config: NPCConfig,
    private val messageRepository: MessageRepository,
    private val sharebookRepository: SharebookRepository
): EventHandler {
    private val commandMessageParser = CommandMessageParser()

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
     * Processes LLM call synchronously. Called by scheduler periodically.
     * Processes serially - blocks until completion.
     * @return true if LLM call succeeded, false otherwise
     */
    override fun processLLM(): Boolean {
        return try {
            LogUtil.info("processLLM: Processing LLM for NPC ${config.npcName}")

            // Perform summarization if needed
            history.performSummarizationIfNeeded()

            // Build messages for LLM: history + current state with context
            val messagesForLLM = mutableListOf<Message>()
            // Add all history messages (without context)
            messagesForLLM.addAll(history.latestConversations)
            
            // If there's a last user message, replace it with formatted version that includes context
            if (messagesForLLM.isNotEmpty() && messagesForLLM.last().role == "user") {
                val lastUserMessage = messagesForLLM.last().message
                val formattedPrompt: String = StructuredInputFormatter.formatStructured(lastUserMessage, contextProvider.buildContext())
                messagesForLLM[messagesForLLM.size - 1] = Message(formattedPrompt, "user")
            } else {
                // No recent user message, create a context-only prompt
                val contextPrompt = "Current state and context. What should I do?"
                val formattedPrompt: String = StructuredInputFormatter.formatStructured(contextPrompt, contextProvider.buildContext())
                messagesForLLM.add(Message(formattedPrompt, "user"))
            }
            
            // Use structured output only (no tool calling)
            // The LLM returns JSON with thought, action array, and message
            val server = contextProvider.getNpcEntity().server
            val toolResponse = llmClient.chatWithTools(messagesForLLM, server)
            
            // Parse structured output (ActionResponse format)
            val content = toolResponse.content.trim()
            val actionResponse = try {
                if (content.startsWith("{") && (content.contains("\"action\"") || content.contains("\"message\""))) {
                    commandMessageParser.parseActionResponse(content)
                } else {
                    // Fallback: treat as plain message
                    CommandMessageParser.ActionResponse(
                        thought = null,
                        action = emptyList(),
                        message = content
                    )
                }
            } catch (e: Exception) {
                LogUtil.error("Failed to parse LLM response", e)
                // Fallback: treat as plain message
                CommandMessageParser.ActionResponse(
                    thought = null,
                    action = emptyList(),
                    message = content
                )
            }
            
            // Process actions in sequence
            val actionDescriptions = mutableListOf<String>()
            for (customCommand in actionResponse.action) {
                if (customCommand.isBlank() || customCommand == "idle") {
                    continue
                }
                
                // Map custom command to actual command/tool
                val mapped: String? = me.prskid1000.craftagent.util.CommandMapper.mapCommand(customCommand)
                if (mapped == null) {
                    LogUtil.info("Unknown custom command: $customCommand")
                    actionDescriptions.add("⚠️ Unknown command: $customCommand")
                    continue
                }
                
                // Check if it's a tool action (contains : or |)
                if (mapped.contains(":") || mapped.contains("|")) {
                    // Parse tool action with parameters
                    val toolParams = me.prskid1000.craftagent.util.CommandMapper.parseToolAction(mapped)
                    
                    when {
                        mapped.startsWith("manageMemory:") -> {
                            val actionParts = mapped.split(":")[1].split("|")[0].split(":")
                            val action = actionParts[0]
                            val infoType = if (actionParts.size > 1) actionParts[1] else null
                            
                            // Create a mock ToolCall with parsed parameters
                            val mockArgs = mutableMapOf<String, Any>()
                            mockArgs["action"] = action
                            if (infoType != null) mockArgs["infoType"] = infoType
                            toolParams.forEach { (key, value) ->
                                mockArgs[key] = value
                            }
                            
                            val mockToolCall = me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall(
                                "fake-id", "manageMemory", mockArgs
                            )
                            
                            handleManageMemory(mockToolCall)
                            val name = toolParams["name"] ?: "unknown"
                            val actionCap = action.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                            actionDescriptions.add("💾 $actionCap $infoType: $name")
                        }
                        mapped.startsWith("sendMessage") -> {
                            val recipient = toolParams["recipient"] ?: "unknown"
                            val subject = toolParams["subject"] ?: ""
                            val content = toolParams["content"] ?: ""
                            
                            // Create a mock ToolCall
                            val mockArgs = mutableMapOf<String, Any>()
                            mockArgs["recipientName"] = recipient
                            mockArgs["subject"] = subject
                            mockArgs["content"] = content
                            
                            val mockToolCall = me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall(
                                "fake-id", "sendMessage", mockArgs
                            )
                            
                            handleSendMessage(mockToolCall)
                            actionDescriptions.add("📧 Sent message to $recipient: $subject")
                        }
                        mapped.startsWith("manageBook:") -> {
                            val action = mapped.split(":")[1].split("|")[0]
                            val title = toolParams["title"] ?: "unknown"
                            val content = toolParams["content"] ?: ""
                            
                            // Create a mock ToolCall
                            val mockArgs = mutableMapOf<String, Any>()
                            mockArgs["action"] = action
                            mockArgs["pageTitle"] = title
                            mockArgs["content"] = content
                            
                            val mockToolCall = me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall(
                                "fake-id", "manageBook", mockArgs
                            )
                            
                            handleManageBook(mockToolCall)
                            val actionCap = action.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                            actionDescriptions.add("📖 $actionCap book page: $title")
                        }
                        else -> {
                            actionDescriptions.add("🔧 Tool action: $customCommand")
                        }
                    }
                } else {
                    // It's a Minecraft command - execute it
                    val succeeded = execute(mapped)
                    if (succeeded) {
                        actionDescriptions.add("✅ $customCommand")
                    } else {
                        actionDescriptions.add("❌ Failed: $customCommand")
                        // Don't stop on first failure, continue with other actions
                    }
                }
            }
            
            // Extract message
            val message = actionResponse.message.trim()
            
            // Send message if present and different from last
            if (message.isNotEmpty() && message != history.getLastMessage()) {
                val npcEntity = contextProvider.getNpcEntity()
                me.prskid1000.craftagent.util.ChatUtil.sendChatMessage(npcEntity, message)
            }
            
            // Add response to history
            val responseText = buildString {
                // Add thought if present
                if (!actionResponse.thought.isNullOrBlank()) {
                    append("💭 ${actionResponse.thought}\n\n")
                }
                
                // Add chat message if present
                if (message.isNotEmpty()) {
                    append(message)
                }
                
                // Add action descriptions if present
                if (actionDescriptions.isNotEmpty()) {
                    if (message.isNotEmpty() || !actionResponse.thought.isNullOrBlank()) {
                        append("\n\n")
                    }
                    append("**Actions:**\n")
                    actionDescriptions.forEach { desc ->
                        append("• $desc\n")
                    }
                }
                
                // Fallback if nothing else
                if (isEmpty()) {
                    append(content.ifEmpty { "No action" })
                }
            }.trim()
            
            history.add(Message(responseText, "assistant"))
            
            true
        } catch (e: Exception) {
            LogUtil.debugInChat("Could not generate a response: " + buildErrorMessage(e))
            LogUtil.error("Error occurred processing LLM for NPC ${config.npcName}", e)
            false
        }
    }

    override fun stopService() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    override fun queueIsEmpty(): Boolean {
        return executorService.queue.isEmpty()
    }

    fun execute(command: String): Boolean {
        val npcEntity = contextProvider.getNpcEntity()
        val server = npcEntity.server
        
        // Remove leading slash if present
        val commandToExecute = if (command.startsWith("/")) command.substring(1) else command
        
        // Execute using Brigadier via MinecraftCommandUtil
        return me.prskid1000.craftagent.util.MinecraftCommandUtil.executeCommand(
            npcEntity,
            commandToExecute,
            {
                // On success - no need for LookAtOwnerTask, can use vanilla commands if needed
            },
            { error ->
                // On error
                val availableCommands = me.prskid1000.craftagent.util.MinecraftCommandUtil.getFormattedCommandList(server)
                val errorMessage = if (error.message?.contains("does not exist") == true || error.message?.contains("Invalid command") == true || error.message?.contains("Unknown or incomplete command") == true) {
                    "Command '%s' does not exist. Error: %s. You MUST use ONLY these available Minecraft commands: %s. Use vanilla Minecraft commands like 'give', 'tp', 'effect', 'summon', 'setblock', 'fill', etc.".format(
                        commandToExecute, error.message ?: "Unknown error", availableCommands
                    )
                } else {
                    Instructions.COMMAND_ERROR_PROMPT.format(commandToExecute, error.message ?: "Unknown error")
                }
                // Store error in state, will be picked up by scheduler
                this.updateState(errorMessage)
                LogUtil.error("Error executing command: $commandToExecute", error)
            }
        )
    }

    private fun buildErrorMessage(exception: Throwable): String? {
        val chain = generateSequence(exception) { it.cause }
        val custom = chain.filterIsInstance<CustomEventException>().firstOrNull()
        if (custom != null) {
            return custom.message
        }
        return generateSequence(exception) { it.cause }.lastOrNull()?.message ?: exception.message
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

    private fun handleManageMemory(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val memoryManager = contextProvider.memoryManager ?: return
        val args = toolCall.arguments
        
        val action = args["action"]?.toString() ?: return
        val infoType = args["infoType"]?.toString() ?: return
        val name = args["name"]?.toString() ?: return
        
        when (action) {
            "add", "update" -> {
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
            "remove" -> {
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
            else -> {
                LogUtil.debugInChat("Unknown action: $action. Use 'add', 'update', or 'remove'.")
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
        val npcEntity = contextProvider.getNpcEntity()
        me.prskid1000.craftagent.util.ChatUtil.sendChatMessage(npcEntity, chatMessage)
        
        LogUtil.info("Sent message to $recipientName: $subject")
    }

    private fun formatManageMemoryDescription(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall): String {
        val args = toolCall.arguments
        val action = args["action"]?.toString() ?: return ""
        val infoType = args["infoType"]?.toString() ?: return ""
        val name = args["name"]?.toString() ?: return ""
        
        return when (action) {
            "add", "update" -> {
                when (infoType) {
                    "contact" -> {
                        val relationship = args["relationship"]?.toString() ?: "neutral"
                        val actionText = action.replaceFirstChar { it.uppercase() }
                        "📝 ${actionText}d contact: $name (relationship: $relationship)"
                    }
                    "location" -> {
                        val description = args["description"]?.toString() ?: ""
                        val actionText = action.replaceFirstChar { it.uppercase() }
                        "📍 ${actionText}d location: $name${if (description.isNotEmpty()) " - $description" else ""}"
                    }
                    else -> ""
                }
            }
            "remove" -> {
                when (infoType) {
                    "contact" -> "🗑️ Removed contact: $name"
                    "location" -> "🗑️ Removed location: $name"
                    else -> ""
                }
            }
            else -> ""
        }
    }
    
    private fun formatSendMessageDescription(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall): String {
        val args = toolCall.arguments
        val recipientName = args["recipientName"]?.toString() ?: return ""
        val subject = args["subject"]?.toString() ?: return ""
        return "📬 Sent message to $recipientName: \"$subject\""
    }
    
    private fun formatManageBookDescription(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall): String {
        val args = toolCall.arguments
        val action = args["action"]?.toString() ?: return ""
        val pageTitle = args["pageTitle"]?.toString() ?: return ""
        
        return when (action) {
            "add", "update" -> {
                val actionText = action.replaceFirstChar { it.uppercase() }
                "📖 ${actionText}d book page: \"$pageTitle\""
            }
            "remove" -> "🗑️ Removed book page: \"$pageTitle\""
            else -> ""
        }
    }

    private fun handleManageBook(toolCall: me.prskid1000.craftagent.llm.ToolCallResponse.ToolCall) {
        val args = toolCall.arguments
        
        val action = args["action"]?.toString() ?: return
        val pageTitle = args["pageTitle"]?.toString() ?: return
        
        when (action) {
            "add", "update" -> {
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
            "remove" -> {
                sharebookRepository.delete(pageTitle)
                LogUtil.info("Removed sharebook page: $pageTitle")
            }
            else -> {
                LogUtil.debugInChat("Unknown action: $action. Use 'add', 'update', or 'remove'.")
            }
        }
    }

}