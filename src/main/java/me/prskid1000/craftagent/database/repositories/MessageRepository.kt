package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.database.SqliteClient
import me.prskid1000.craftagent.model.database.Message
import java.util.UUID

class MessageRepository(
    val sqliteClient: SqliteClient,
) {
    
    fun init() {
        createTable()
    }

    fun createTable() {
        val sql = """
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                recipient_uuid TEXT NOT NULL,
                sender_uuid TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                sender_type TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """
        
        // Only recreate table if schema has changed
        if (sqliteClient.needsSchemaUpdate("messages", sql)) {
            // Drop table if it exists (this also drops indexes)
            sqliteClient.dropTable("messages")
            
            // Verify table was dropped before creating
            if (sqliteClient.tableExists("messages")) {
                throw RuntimeException("Failed to drop messages table before recreation")
            }
            
            // Create table from scratch
            sqliteClient.update(sql)
            
            // Create index (simplified - no read column)
            val indexSql = "CREATE INDEX idx_message_recipient ON messages(recipient_uuid);"
            sqliteClient.update(indexSql)
            
            // Store schema version
            sqliteClient.setSchemaVersion("messages", sqliteClient.calculateSchemaHash(sql))
        }
    }

    fun insert(message: Message, maxMessages: Int) {
        // Check if we need to delete oldest message if at limit
        val existing = selectByRecipient(message.recipientUuid, maxMessages + 1)
        if (existing.size >= maxMessages) {
            // Delete oldest message by timestamp
            val oldest = existing.minByOrNull { it.timestamp }
            oldest?.let { delete(it.id) }
        }
        
        val statement = sqliteClient.buildPreparedStatement(
            """INSERT INTO messages (recipient_uuid, sender_uuid, sender_name, sender_type, content, timestamp)
               VALUES (?, ?, ?, ?, ?, ?)""",
        )
        if (statement == null) {
            throw RuntimeException("Failed to create prepared statement for message insert")
        }
        
        statement.setString(1, message.recipientUuid.toString())
        statement.setString(2, message.senderUuid.toString())
        statement.setString(3, message.senderName)
        statement.setString(4, message.senderType)
        statement.setString(5, message.content)
        statement.setLong(6, message.timestamp)
        sqliteClient.update(statement)
        
        // Note: sqliteClient.update() swallows exceptions, so verification should be done
        // by checking if the message exists after the insert
    }

    fun selectByRecipient(recipientUuid: UUID, limit: Int = 50): List<Message> {
        val sql = "SELECT * FROM messages WHERE recipient_uuid = '%s' ORDER BY timestamp DESC LIMIT %d".format(
            recipientUuid.toString(), limit
        )
        return executeAndProcessMessages(sql)
    }

    fun delete(messageId: Long) {
        val sql = "DELETE FROM messages WHERE id = %d".format(messageId)
        sqliteClient.update(sql)
    }

    fun deleteByNpcUuid(npcUuid: UUID) {
        // Delete messages where NPC is either sender or recipient
        val sql = "DELETE FROM messages WHERE sender_uuid = '%s' OR recipient_uuid = '%s'".format(
            npcUuid.toString(), npcUuid.toString()
        )
        sqliteClient.update(sql)
    }

    private fun executeAndProcessMessages(sql: String): List<Message> {
        val result = sqliteClient.query(sql)
        val messages = arrayListOf<Message>()

        if (result == null) return emptyList()

        while (result.next()) {
            val message = Message(
                result.getLong("id"),
                UUID.fromString(result.getString("recipient_uuid")),
                UUID.fromString(result.getString("sender_uuid")),
                result.getString("sender_name"),
                result.getString("sender_type") ?: "NPC", // Default to "NPC" for backward compatibility
                result.getString("content"),
                result.getLong("timestamp")
            )
            messages.add(message)
        }
        result.close()
        return messages
    }
}

