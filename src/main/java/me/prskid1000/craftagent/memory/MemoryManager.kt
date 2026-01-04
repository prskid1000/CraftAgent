package me.prskid1000.craftagent.memory

import me.prskid1000.craftagent.config.BaseConfig
import me.prskid1000.craftagent.database.repositories.PrivateBookPageRepository
import me.prskid1000.craftagent.model.database.PrivateBookPage
import me.prskid1000.craftagent.util.LogUtil
import java.util.UUID

/**
 * Manages NPC memory: private book pages
 * Uses direct database calls instead of in-memory caching.
 */
class MemoryManager(
    private val privateBookPageRepository: PrivateBookPageRepository,
    private val npcUuid: UUID,
    private val config: BaseConfig
) {
    /**
     * Saves or updates a private book page directly to database
     */
    fun savePage(pageTitle: String, content: String) {
        val page = PrivateBookPage(
            npcUuid = npcUuid,
            pageTitle = pageTitle,
            content = content
        )
        try {
            privateBookPageRepository.insertOrUpdate(page, config.maxPrivatePages)
        } catch (e: Exception) {
            LogUtil.error("Error saving private page: $pageTitle", e)
        }
    }

    /**
     * Gets all private book pages for this NPC from database
     */
    fun getPages(): List<PrivateBookPage> {
        return try {
            privateBookPageRepository.selectByNpcUuid(npcUuid)
                .sortedByDescending { it.timestamp }
                .take(config.maxPrivatePages)
        } catch (e: Exception) {
            LogUtil.error("Error loading private pages for NPC: $npcUuid", e)
            emptyList()
        }
    }

    /**
     * Gets a specific private book page by title from database
     */
    fun getPage(pageTitle: String): PrivateBookPage? {
        return try {
            privateBookPageRepository.selectByNpcUuid(npcUuid)
                .firstOrNull { it.pageTitle == pageTitle }
        } catch (e: Exception) {
            LogUtil.error("Error loading private page: $pageTitle", e)
            null
        }
    }

    /**
     * Deletes a private book page from database
     */
    fun deletePage(pageTitle: String) {
        try {
            privateBookPageRepository.delete(npcUuid, pageTitle)
        } catch (e: Exception) {
            LogUtil.error("Error deleting private page: $pageTitle", e)
        }
    }
}
