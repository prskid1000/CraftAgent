package me.prskid1000.craftagent.database.resources

import me.prskid1000.craftagent.database.repositories.ContactRepository
import me.prskid1000.craftagent.database.repositories.ConversationRepository
import me.prskid1000.craftagent.database.repositories.LocationMemoryRepository
import me.prskid1000.craftagent.database.repositories.MessageRepository
import me.prskid1000.craftagent.database.repositories.SharebookRepository
import me.prskid1000.craftagent.history.Message
import me.prskid1000.craftagent.model.database.Conversation
import me.prskid1000.craftagent.util.LogUtil
import java.util.UUID

import java.util.concurrent.CompletableFuture

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ResourceProvider(
    val conversationRepository: ConversationRepository,
    val locationRepository: LocationMemoryRepository? = null,
    val contactRepository: ContactRepository? = null,
    val messageRepository: MessageRepository? = null,
    val sharebookRepository: SharebookRepository? = null
) {
    private lateinit var executorService: ExecutorService
    val loadedConversations = hashMapOf<UUID, List<Conversation>>()

    /**
     * Loads conversations recipes from db/mc into memory
     */
    fun loadResources(uuids: List<UUID>) {
        executorService = initExecutorPool()
        runAsync {
            LogUtil.info("Loading conversations into memory...")
            uuids.forEach {
                this.loadedConversations[it] = conversationRepository.selectByUuid(it)
            }
        }.thenRun {
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
    }

    fun addConversations(uuid: UUID, messages: List<Message>) {
        this.loadedConversations[uuid] = messages.map { Conversation(uuid, it.role, it.message) }
    }

    /**
     * Saves recipes and conversations to local db. (called on server stop)
     *
     * Stops initial resources indexing if not finished by shutting down executor
     */
    fun saveResources() {
        shutdownServiceNow()
        executorService = initExecutorPool()
        runAsync {
            loadedConversations.forEach { conversations ->
                conversations.value.forEach { conversationRepository.insert(it) } }
            LogUtil.info("Saved conversations to db")
        }.get()
        executorService.shutdownNow()
    }

    private fun shutdownServiceNow() {
        if (::executorService.isInitialized && !executorService.isTerminated) {
            executorService.shutdownNow()
            LogUtil.error("Initial loading of resources interrupted - Wait for termination")
            try {
                executorService.awaitTermination(500, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun runAsync(task: () -> Unit): CompletableFuture<Void> {
        return CompletableFuture.runAsync(task , executorService).exceptionally {
            LogUtil.error("Error loading/saving resources into memory", it)
            null
        }
    }

    private fun initExecutorPool(): ExecutorService {
        return Executors.newFixedThreadPool(2)
    }
}
