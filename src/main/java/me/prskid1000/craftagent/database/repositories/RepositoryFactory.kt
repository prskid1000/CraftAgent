package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.CraftAgent
import me.prskid1000.craftagent.database.SqliteClient
import me.prskid1000.craftagent.util.LogUtil
import java.sql.SQLException

class RepositoryFactory(
    val sqliteClient: SqliteClient
) {
    val conversationRepository = ConversationRepository(sqliteClient)
    val privateBookPageRepository = PrivateBookPageRepository(sqliteClient)
    val messageRepository = MessageRepository(sqliteClient)
    val sharebookRepository = SharebookRepository(sqliteClient)

    fun initRepositories() {
        sqliteClient.initDatabase(CraftAgent.MOD_ID)
        
        try {
            conversationRepository.init()
            privateBookPageRepository.init()
            messageRepository.init()
            sharebookRepository.init()
        } catch (e: SQLException) {
            // Migration error detected - delete database and restart from beginning
            val errorMessage = e.message ?: "Unknown error"
            if (errorMessage.contains("duplicate column") || 
                errorMessage.contains("SQLITE_ERROR") ||
                errorMessage.contains("no such column") ||
                errorMessage.contains("syntax error")) {
                
                LogUtil.error("Migration error detected: $errorMessage. Resetting database and running migrations from beginning.")
                sqliteClient.deleteDatabase()
                
                // Re-initialize database and run migrations from beginning
                sqliteClient.initDatabase(CraftAgent.MOD_ID)
                conversationRepository.init()
                privateBookPageRepository.init()
                messageRepository.init()
                sharebookRepository.init()
            } else {
                // Re-throw if it's not a migration-related error
                throw e
            }
        }
    }
}
