package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.CraftAgent
import me.prskid1000.craftagent.database.SqliteClient

class RepositoryFactory(
    val sqliteClient: SqliteClient
) {
    val conversationRepository = ConversationRepository(sqliteClient)

    fun initRepositories() {
        sqliteClient.initDatabase(CraftAgent.MOD_ID)
        conversationRepository.init()
    }
}
