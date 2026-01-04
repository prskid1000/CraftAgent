package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.database.SqliteClient
import me.prskid1000.craftagent.model.database.SharebookPage

class SharebookRepository(
    val sqliteClient: SqliteClient,
) {
    
    fun init() {
        createTable()
    }

    fun createTable() {
        val sql = """
            CREATE TABLE sharebook (
                page_title TEXT NOT NULL,
                author_uuid TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                PRIMARY KEY(page_title, author_uuid)
            );
        """
        
        // Only recreate table if schema has changed
        if (sqliteClient.needsSchemaUpdate("sharebook", sql)) {
            // Drop table if it exists (this also drops indexes)
            sqliteClient.dropTable("sharebook")
            
            // Verify table was dropped before creating
            if (sqliteClient.tableExists("sharebook")) {
                throw RuntimeException("Failed to drop sharebook table before recreation")
            }
            
            // Create table from scratch
            sqliteClient.update(sql)
            
            // Store schema version
            sqliteClient.setSchemaVersion("sharebook", sqliteClient.calculateSchemaHash(sql))
        }
    }

    fun insertOrUpdate(page: SharebookPage, maxPages: Int) {
        // Check if we need to delete oldest page if at limit (only for new pages, not updates)
        val existing = selectByTitleAndAuthor(page.pageTitle, page.authorUuid)
        if (existing == null) {
            // New page - check if we're at limit
            val allPages = selectAll()
            if (allPages.size >= maxPages) {
                val oldest = allPages.minByOrNull { it.timestamp }
                oldest?.let { delete(it.pageTitle, it.authorUuid) }
            }
        }
        
        val statement = sqliteClient.buildPreparedStatement(
            """INSERT INTO sharebook (page_title, author_uuid, content, timestamp)
               VALUES (?, ?, ?, ?)
               ON CONFLICT(page_title, author_uuid) DO UPDATE SET
               content = excluded.content,
               timestamp = excluded.timestamp""",
        )
        if (statement == null) {
            throw RuntimeException("Failed to create prepared statement for sharebook insert/update")
        }
        
        statement.setString(1, page.pageTitle)
        statement.setString(2, page.authorUuid)
        statement.setString(3, page.content)
        statement.setLong(4, page.timestamp)
        sqliteClient.update(statement)
        
        // Note: sqliteClient.update() swallows exceptions, so we verify the insert worked
        // by checking if the page exists after the update
    }

    fun selectAll(): List<SharebookPage> {
        val sql = "SELECT * FROM sharebook ORDER BY timestamp DESC"
        return executeAndProcessPages(sql)
    }

    fun selectByTitleAndAuthor(pageTitle: String, authorUuid: String): SharebookPage? {
        val sql = "SELECT * FROM sharebook WHERE page_title = '%s' AND author_uuid = '%s'".format(
            pageTitle.replace("'", "''"), authorUuid.replace("'", "''")
        )
        val pages = executeAndProcessPages(sql)
        return pages.firstOrNull()
    }

    fun delete(pageTitle: String, authorUuid: String) {
        val sql = "DELETE FROM sharebook WHERE page_title = '%s' AND author_uuid = '%s'".format(
            pageTitle.replace("'", "''"), authorUuid.replace("'", "''")
        )
        sqliteClient.update(sql)
    }

    fun deleteAll() {
        sqliteClient.update("DELETE FROM sharebook")
    }

    private fun executeAndProcessPages(sql: String): List<SharebookPage> {
        val result = sqliteClient.query(sql)
        val pages = arrayListOf<SharebookPage>()

        if (result == null) return emptyList()

        while (result.next()) {
            val page = SharebookPage(
                result.getString("page_title"),
                result.getString("content"),
                result.getString("author_uuid"),
                result.getLong("timestamp")
            )
            pages.add(page)
        }
        result.close()
        return pages
    }
}
