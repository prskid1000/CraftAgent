package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.database.SqliteClient
import me.prskid1000.craftagent.model.database.Contact
import java.util.UUID

class ContactRepository(
    val sqliteClient: SqliteClient,
) {
    
    fun init() {
        createTable()
    }

    fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                npc_uuid CHARACTER(36) NOT NULL,
                contact_uuid CHARACTER(36) NOT NULL,
                contact_name TEXT NOT NULL,
                contact_type TEXT NOT NULL,
                relationship TEXT NOT NULL,
                last_seen INTEGER NOT NULL,
                notes TEXT NOT NULL,
                UNIQUE(npc_uuid, contact_uuid)
            );
        """
        sqliteClient.update(sql)
        
        // Create indexes
        val indexSql1 = "CREATE INDEX IF NOT EXISTS idx_contact_npc_uuid ON contacts(npc_uuid);"
        val indexSql2 = "CREATE INDEX IF NOT EXISTS idx_contact_contact_uuid ON contacts(contact_uuid);"
        sqliteClient.update(indexSql1)
        sqliteClient.update(indexSql2)
    }

    fun insertOrUpdate(contact: Contact, maxContacts: Int) {
        // Check if we need to delete oldest contact (only for new contacts)
        val existing = selectByNpcUuid(contact.npcUuid, maxContacts)
        if (existing.size >= maxContacts && !existing.any { it.contactUuid == contact.contactUuid }) {
            // New contact and at limit - delete oldest
            val oldest = existing.minByOrNull { it.lastSeen }
            oldest?.let { delete(contact.npcUuid, it.contactUuid) }
        }
        
        val statement = sqliteClient.buildPreparedStatement(
            """INSERT INTO contacts (npc_uuid, contact_uuid, contact_name, contact_type, relationship, last_seen, notes)
               VALUES (?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT(npc_uuid, contact_uuid) DO UPDATE SET
               contact_name = excluded.contact_name,
               contact_type = excluded.contact_type,
               relationship = excluded.relationship,
               last_seen = excluded.last_seen,
               notes = excluded.notes""",
        )
        statement?.setString(1, contact.npcUuid.toString())
        statement?.setString(2, contact.contactUuid.toString())
        statement?.setString(3, contact.contactName)
        statement?.setString(4, contact.contactType)
        statement?.setString(5, contact.relationship)
        statement?.setLong(6, contact.lastSeen)
        statement?.setString(7, contact.notes)
        sqliteClient.update(statement)
    }

    fun selectByNpcUuid(npcUuid: UUID, maxContacts: Int = 20): List<Contact> {
        val sql = "SELECT * FROM contacts WHERE npc_uuid = '%s' ORDER BY last_seen DESC LIMIT %d".format(npcUuid.toString(), maxContacts)
        return executeAndProcessContacts(sql)
    }

    fun delete(npcUuid: UUID, contactUuid: UUID) {
        val sql = "DELETE FROM contacts WHERE npc_uuid = '%s' AND contact_uuid = '%s'".format(npcUuid.toString(), contactUuid.toString())
        sqliteClient.update(sql)
    }

    fun deleteByNpcUuid(npcUuid: UUID) {
        val sql = "DELETE FROM contacts WHERE npc_uuid = '%s'".format(npcUuid.toString())
        sqliteClient.update(sql)
    }

    private fun executeAndProcessContacts(sql: String): List<Contact> {
        val result = sqliteClient.query(sql)
        val contacts = arrayListOf<Contact>()

        if (result == null) return emptyList()

        while (result.next()) {
            val contact = Contact(
                UUID.fromString(result.getString("npc_uuid")),
                UUID.fromString(result.getString("contact_uuid")),
                result.getString("contact_name"),
                result.getString("contact_type"),
                result.getString("relationship"),
                result.getLong("last_seen"),
                result.getString("notes")
            )
            contacts.add(contact)
        }
        result.close()
        return contacts
    }
}

