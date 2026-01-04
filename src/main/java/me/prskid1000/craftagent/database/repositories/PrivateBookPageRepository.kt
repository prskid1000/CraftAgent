package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.database.SqliteClient
import me.prskid1000.craftagent.model.database.PrivateBookPage
import java.util.UUID

class PrivateBookPageRepository(
    val sqliteClient: SqliteClient,
) {
    
    fun init() {
        createTable()
    }

    fun createTable() {
        val sql = """
            CREATE TABLE private_book (
                npc_uuid TEXT NOT NULL,
                page_title TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                PRIMARY KEY(npc_uuid, page_title)
            );
        """
        
        // Only recreate table if schema has changed
        if (sqliteClient.needsSchemaUpdate("private_book", sql)) {
            // Drop table if it exists (this also drops indexes)
            sqliteClient.dropTable("private_book")
            
            // Create table from scratch
            sqliteClient.update(sql)
            
            // Store schema version
            sqliteClient.setSchemaVersion("private_book", sqliteClient.calculateSchemaHash(sql))
        }
    }

    fun insertOrUpdate(page: PrivateBookPage, maxPages: Int) {
        // Check if we need to delete oldest page if at limit (only for new pages, not updates)
        val existing = selectByTitle(page.npcUuid, page.pageTitle)
        if (existing == null) {
            // New page - check if we're at limit for this NPC
            val allPages = selectByNpcUuid(page.npcUuid)
            if (allPages.size >= maxPages) {
                val oldest = allPages.minByOrNull { it.timestamp }
                oldest?.let { delete(page.npcUuid, it.pageTitle) }
            }
        }
        
        val statement = sqliteClient.buildPreparedStatement(
            """INSERT INTO private_book (npc_uuid, page_title, content, timestamp)
               VALUES (?, ?, ?, ?)
               ON CONFLICT(npc_uuid, page_title) DO UPDATE SET
               content = excluded.content,
               timestamp = excluded.timestamp""",
        )
        statement?.setString(1, page.npcUuid.toString())
        statement?.setString(2, page.pageTitle)
        statement?.setString(3, page.content)
        statement?.setLong(4, page.timestamp)
        sqliteClient.update(statement)
    }

    fun selectByNpcUuid(npcUuid: UUID): List<PrivateBookPage> {
        val sql = "SELECT * FROM private_book WHERE npc_uuid = '%s' ORDER BY timestamp DESC".format(npcUuid.toString())
        return executeAndProcessPages(sql)
    }

    fun selectByTitle(npcUuid: UUID, pageTitle: String): PrivateBookPage? {
        val sql = "SELECT * FROM private_book WHERE npc_uuid = '%s' AND page_title = '%s'".format(
            npcUuid.toString(), pageTitle.replace("'", "''")
        )
        val pages = executeAndProcessPages(sql)
        return pages.firstOrNull()
    }

    fun delete(npcUuid: UUID, pageTitle: String) {
        val sql = "DELETE FROM private_book WHERE npc_uuid = '%s' AND page_title = '%s'".format(
            npcUuid.toString(), pageTitle.replace("'", "''")
        )
        sqliteClient.update(sql)
    }

    fun deleteByNpcUuid(npcUuid: UUID) {
        val sql = "DELETE FROM private_book WHERE npc_uuid = '%s'".format(npcUuid.toString())
        sqliteClient.update(sql)
    }


    private fun executeAndProcessPages(sql: String): List<PrivateBookPage> {
        val result = sqliteClient.query(sql)
        val pages = arrayListOf<PrivateBookPage>()

        if (result == null) return emptyList()

        while (result.next()) {
            val page = PrivateBookPage(
                UUID.fromString(result.getString("npc_uuid")),
                result.getString("page_title"),
                result.getString("content"),
                result.getLong("timestamp")
            )
            pages.add(page)
        }
        result.close()
        return pages
    }
}

