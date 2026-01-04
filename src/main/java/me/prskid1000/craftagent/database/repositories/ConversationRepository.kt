package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.database.SqliteClient
import me.prskid1000.craftagent.model.database.Conversation
import java.util.UUID

class ConversationRepository(
    val sqliteClient: SqliteClient,
) {
    fun init() {
        createTable()
    }

    fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS conversations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid CHARACTER(36) NOT NULL,
                role CHARACTER(9) NOT NULL,
                message TEXT NOT NULL,
                timestamp INTEGER NOT NULL DEFAULT 0
            );
        """
        sqliteClient.update(sql)
        
        // Migration: Add timestamp column if it doesn't exist (for existing databases)
        try {
            sqliteClient.update("ALTER TABLE conversations ADD COLUMN timestamp INTEGER DEFAULT 0")
            // Update existing rows to have current timestamp
            sqliteClient.update("UPDATE conversations SET timestamp = ${System.currentTimeMillis()} WHERE timestamp = 0 OR timestamp IS NULL")
        } catch (e: Exception) {
            // Column already exists, ignore
        }
    }

    fun insert(conversation: Conversation) {
        val statement =
            sqliteClient.buildPreparedStatement(
                "INSERT INTO conversations (uuid, role, message, timestamp) VALUES (?, ?, ?, ?)",
            )
        statement?.setString(1, conversation.uuid.toString())
        statement?.setString(2, conversation.role)
        statement?.setString(3, conversation.message)
        statement?.setLong(4, conversation.timestamp)
        sqliteClient.update(statement)
    }

    /**
     * Selects latest conversations of an NPC, ordered by timestamp (oldest first)
     * @param limit Maximum number of conversations to return (default 100)
     */
    fun selectByUuid(uuid: UUID, limit: Int = 100): List<Conversation> {
        val sql = "SELECT * FROM conversations WHERE uuid = '%s' ORDER BY timestamp ASC LIMIT %d".format(uuid.toString(), limit)
        return executeAndProcessConversations(sql)
    }

    /**
     * Deletes all conversations of the given uuid.
     */
    fun deleteByUuid(uuid: UUID) {
        val sql = "DELETE FROM conversations WHERE uuid = '%s'".format(uuid.toString())
        sqliteClient.update(sql)
    }

    /**
     * Deletes conversations by their database IDs
     */
    fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        val idsString = ids.joinToString(",")
        val sql = "DELETE FROM conversations WHERE id IN ($idsString)"
        sqliteClient.update(sql)
    }

    /**
     * Updates a conversation message by finding the system message and updating it
     */
    fun updateSystemMessage(uuid: UUID, newMessage: String) {
        // Delete old system message
        val deleteSql = "DELETE FROM conversations WHERE uuid = '%s' AND role = 'system'".format(uuid.toString())
        sqliteClient.update(deleteSql)
        // Insert new system message with earliest timestamp
        val statement = sqliteClient.buildPreparedStatement(
            "INSERT INTO conversations (uuid, role, message, timestamp) VALUES (?, 'system', ?, 0)"
        )
        statement?.setString(1, uuid.toString())
        statement?.setString(2, newMessage)
        sqliteClient.update(statement)
    }

    private fun executeAndProcessConversations(sql: String): List<Conversation> {
        val result = sqliteClient.query(sql)
        val conversations = arrayListOf<Conversation>()

        if (result == null) return emptyList()

        while (result.next()) {
            val conversation =
                Conversation(
                    result.getLong("id"),
                    UUID.fromString(result.getString("uuid")),
                    result.getString("role"),
                    result.getString("message"),
                    try {
                        result.getLong("timestamp")
                    } catch (e: Exception) {
                        System.currentTimeMillis() // Fallback for old records without timestamp
                    }
                )
            conversations.add(conversation)
        }
        result.close()
        return conversations
    }
}
