package me.prskid1000.craftagent.memory

import me.prskid1000.craftagent.config.BaseConfig
import me.prskid1000.craftagent.database.repositories.PrivateBookPageRepository
import me.prskid1000.craftagent.model.database.PrivateBookPage
import me.prskid1000.craftagent.util.LogUtil
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages NPC memory: private book pages
 */
class MemoryManager(
    private val privateBookPageRepository: PrivateBookPageRepository,
    private val npcUuid: UUID,
    private val config: BaseConfig
) {
    
    private val cachedPages = ConcurrentHashMap<String, PrivateBookPage>()

    init {
        loadFromDatabase()
    }

    private fun loadFromDatabase() {
        try {
            // Load private book pages
            privateBookPageRepository.selectByNpcUuid(npcUuid).forEach {
                cachedPages[it.pageTitle] = it
            }
        } catch (e: Exception) {
            LogUtil.error("Error loading memory for NPC: $npcUuid", e)
        }
    }

    /**
     * Saves or updates a private book page
     */
    fun savePage(pageTitle: String, content: String) {
        val page = PrivateBookPage(
            npcUuid = npcUuid,
            pageTitle = pageTitle,
            content = content
        )
        cachedPages[pageTitle] = page
        try {
            privateBookPageRepository.insertOrUpdate(page, config.maxPrivatePages)
        } catch (e: Exception) {
            LogUtil.error("Error saving private page: $pageTitle", e)
        }
    }

    /**
     * Gets all private book pages for this NPC
     */
    fun getPages(): List<PrivateBookPage> {
        return cachedPages.values.sortedByDescending { it.timestamp }.take(config.maxPrivatePages)
    }

    /**
     * Gets a specific private book page by title
     */
    fun getPage(pageTitle: String): PrivateBookPage? {
        return cachedPages[pageTitle]
    }

    /**
     * Deletes a private book page
     */
    fun deletePage(pageTitle: String) {
        cachedPages.remove(pageTitle)
        try {
            privateBookPageRepository.delete(npcUuid, pageTitle)
        } catch (e: Exception) {
            LogUtil.error("Error deleting private page: $pageTitle", e)
        }
    }

    fun cleanup() {
        // Cleanup is handled by repository deletion in NPCService
    }
}
