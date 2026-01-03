package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.CraftAgent
import me.prskid1000.craftagent.database.SqliteClient

class RepositoryFactory(
    val sqliteClient: SqliteClient
) {
    val conversationRepository = ConversationRepository(sqliteClient)
    val privateBookPageRepository = PrivateBookPageRepository(sqliteClient)
    val messageRepository = MessageRepository(sqliteClient)
    val sharebookRepository = SharebookRepository(sqliteClient)

    fun initRepositories() {
        sqliteClient.initDatabase(CraftAgent.MOD_ID)
        conversationRepository.init()
        privateBookPageRepository.init()
        messageRepository.init()
        sharebookRepository.init()
    }
}
