package me.prskid1000.craftagent.database.resources

import me.prskid1000.craftagent.database.repositories.ConversationRepository
import me.prskid1000.craftagent.database.repositories.MessageRepository
import me.prskid1000.craftagent.database.repositories.PrivateBookPageRepository
import me.prskid1000.craftagent.database.repositories.SharebookRepository
import me.prskid1000.craftagent.util.LogUtil
import java.util.UUID

import java.util.concurrent.CompletableFuture

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ResourceProvider(
    val conversationRepository: ConversationRepository,
    val privateBookPageRepository: PrivateBookPageRepository? = null,
    val messageRepository: MessageRepository? = null,
    val sharebookRepository: SharebookRepository? = null
) {
    /**
     * Loads resources from database.
     * Note: All resources are now stored directly in database, no need to load into memory.
     */
    fun loadResources(uuids: List<UUID>) {
        // All resources (conversations, private pages, etc.) are stored directly in database
        // No need to pre-load them into memory
    }

    /**
     * Saves resources to database. (called on server stop)
     * Note: All resources are already saved in database as they're created, no need to save again.
     */
    fun saveResources() {
        // All resources are already saved in database as they're created
        // No need to save them again on shutdown
    }
}
