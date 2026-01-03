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
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                recipient_uuid CHARACTER(36) NOT NULL,
                sender_uuid CHARACTER(36) NOT NULL,
                sender_name TEXT NOT NULL,
                sender_type TEXT NOT NULL,
                subject TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                read INTEGER NOT NULL DEFAULT 0
            );
        """
        sqliteClient.update(sql)
        
        // Create indexes
        val indexSql1 = "CREATE INDEX IF NOT EXISTS idx_message_recipient ON messages(recipient_uuid);"
        val indexSql2 = "CREATE INDEX IF NOT EXISTS idx_message_sender ON messages(sender_uuid);"
        val indexSql3 = "CREATE INDEX IF NOT EXISTS idx_message_read ON messages(recipient_uuid, read);"
        sqliteClient.update(indexSql1)
        sqliteClient.update(indexSql2)
        sqliteClient.update(indexSql3)
    }

    fun insert(message: Message, maxMessages: Int) {
        // Check if we need to delete oldest message if at limit
        val existing = selectByRecipient(message.recipientUuid, maxMessages + 1, false)
        if (existing.size >= maxMessages) {
            // Delete oldest message (prioritize read messages for deletion)
            val oldestRead = existing.filter { it.read }.minByOrNull { it.timestamp }
            val oldest = oldestRead ?: existing.minByOrNull { it.timestamp }
            oldest?.let { delete(it.id) }
        }
        
        val statement = sqliteClient.buildPreparedStatement(
            """INSERT INTO messages (recipient_uuid, sender_uuid, sender_name, sender_type, subject, content, timestamp, read)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
        )
        statement?.setString(1, message.recipientUuid.toString())
        statement?.setString(2, message.senderUuid.toString())
        statement?.setString(3, message.senderName)
        statement?.setString(4, message.senderType)
        statement?.setString(5, message.subject)
        statement?.setString(6, message.content)
        statement?.setLong(7, message.timestamp)
        statement?.setInt(8, if (message.read) 1 else 0)
        sqliteClient.update(statement)
    }

    fun selectByRecipient(recipientUuid: UUID, limit: Int = 50, unreadOnly: Boolean = false): List<Message> {
        val unreadFilter = if (unreadOnly) " AND read = 0" else ""
        val sql = "SELECT * FROM messages WHERE recipient_uuid = '%s'$unreadFilter ORDER BY timestamp DESC LIMIT %d".format(
            recipientUuid.toString(), limit
        )
        return executeAndProcessMessages(sql)
    }

    fun markAsRead(messageId: Long) {
        val sql = "UPDATE messages SET read = 1 WHERE id = %d".format(messageId)
        sqliteClient.update(sql)
    }

    fun delete(messageId: Long) {
        val sql = "DELETE FROM messages WHERE id = %d".format(messageId)
        sqliteClient.update(sql)
    }

    fun deleteByRecipient(recipientUuid: UUID) {
        val sql = "DELETE FROM messages WHERE recipient_uuid = '%s'".format(recipientUuid.toString())
        sqliteClient.update(sql)
    }

    fun deleteBySender(senderUuid: UUID) {
        val sql = "DELETE FROM messages WHERE sender_uuid = '%s'".format(senderUuid.toString())
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
                result.getString("sender_type"),
                result.getString("subject"),
                result.getString("content"),
                result.getLong("timestamp"),
                result.getInt("read") == 1
            )
            messages.add(message)
        }
        result.close()
        return messages
    }
}

