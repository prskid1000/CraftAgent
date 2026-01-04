package me.prskid1000.craftagent.model.database

data class SharebookPage(
    val pageTitle: String,
    val content: String,
    val authorUuid: String,
    val timestamp: Long = System.currentTimeMillis()
)
