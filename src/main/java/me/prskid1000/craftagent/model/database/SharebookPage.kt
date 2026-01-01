package me.prskid1000.craftagent.model.database

data class SharebookPage(
    val id: Long = 0,
    val pageTitle: String, // Title of the page
    val content: String, // Content of the page
    val authorUuid: String, // UUID of the NPC/player who created/updated it
    val authorName: String, // Name of the author
    val timestamp: Long = System.currentTimeMillis() // Last update time
)
