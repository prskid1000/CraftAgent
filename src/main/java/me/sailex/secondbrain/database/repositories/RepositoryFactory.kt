package me.sailex.secondbrain.database.repositories

import me.sailex.secondbrain.CraftAgent
import me.sailex.secondbrain.database.SqliteClient

class RepositoryFactory(
    val sqliteClient: SqliteClient
) {
    val conversationRepository = ConversationRepository(sqliteClient)

    fun initRepositories() {
        sqliteClient.initDatabase(CraftAgent.MOD_ID)
        conversationRepository.init()
    }
}
