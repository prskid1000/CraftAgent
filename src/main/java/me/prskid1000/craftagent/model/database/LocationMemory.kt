package me.prskid1000.craftagent.model.database

import java.util.UUID

data class LocationMemory(
    val uuid: UUID,
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

