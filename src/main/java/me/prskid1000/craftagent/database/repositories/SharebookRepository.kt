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
            CREATE TABLE IF NOT EXISTS sharebook (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                page_title TEXT NOT NULL UNIQUE,
                content TEXT NOT NULL,
                author_uuid CHARACTER(36) NOT NULL,
                author_name TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """
        sqliteClient.update(sql)
        
        // Create index
        val indexSql = "CREATE INDEX IF NOT EXISTS idx_sharebook_title ON sharebook(page_title);"
        sqliteClient.update(indexSql)
    }

    fun insertOrUpdate(page: SharebookPage, maxPages: Int) {
        // Check if we need to delete oldest page if at limit (only for new pages, not updates)
        val existing = selectByTitle(page.pageTitle)
        if (existing == null) {
            // New page - check if we're at limit
            val allPages = selectAll()
            if (allPages.size >= maxPages) {
                val oldest = allPages.minByOrNull { it.timestamp }
                oldest?.let { delete(it.pageTitle) }
            }
        }
        
        val statement = sqliteClient.buildPreparedStatement(
            """INSERT INTO sharebook (page_title, content, author_uuid, author_name, timestamp)
               VALUES (?, ?, ?, ?, ?)
               ON CONFLICT(page_title) DO UPDATE SET
               content = excluded.content,
               author_uuid = excluded.author_uuid,
               author_name = excluded.author_name,
               timestamp = excluded.timestamp""",
        )
        statement?.setString(1, page.pageTitle)
        statement?.setString(2, page.content)
        statement?.setString(3, page.authorUuid)
        statement?.setString(4, page.authorName)
        statement?.setLong(5, page.timestamp)
        sqliteClient.update(statement)
    }

    fun selectAll(): List<SharebookPage> {
        val sql = "SELECT * FROM sharebook ORDER BY timestamp DESC"
        return executeAndProcessPages(sql)
    }

    fun selectByTitle(pageTitle: String): SharebookPage? {
        val sql = "SELECT * FROM sharebook WHERE page_title = '%s'".format(pageTitle.replace("'", "''"))
        val pages = executeAndProcessPages(sql)
        return pages.firstOrNull()
    }

    fun delete(pageTitle: String) {
        val sql = "DELETE FROM sharebook WHERE page_title = '%s'".format(pageTitle.replace("'", "''"))
        sqliteClient.update(sql)
    }

    fun deleteByAuthorUuid(authorUuid: String) {
        val sql = "DELETE FROM sharebook WHERE author_uuid = '%s'".format(authorUuid.replace("'", "''"))
        sqliteClient.update(sql)
    }

    /**
     * Deletes all sharebook pages.
     * Used when the last NPC is removed to clear shared knowledge.
     */
    fun deleteAll() {
        val sql = "DELETE FROM sharebook"
        sqliteClient.update(sql)
    }

    private fun executeAndProcessPages(sql: String): List<SharebookPage> {
        val result = sqliteClient.query(sql)
        val pages = arrayListOf<SharebookPage>()

        if (result == null) return emptyList()

        while (result.next()) {
            val page = SharebookPage(
                result.getLong("id"),
                result.getString("page_title"),
                result.getString("content"),
                result.getString("author_uuid"),
                result.getString("author_name"),
                result.getLong("timestamp")
            )
            pages.add(page)
        }
        result.close()
        return pages
    }
}
