package me.prskid1000.craftagent.database.repositories

import me.prskid1000.craftagent.database.SqliteClient
import me.prskid1000.craftagent.model.database.LocationMemory
import java.util.UUID

class LocationMemoryRepository(
    val sqliteClient: SqliteClient,
) {
    
    fun init() {
        createTable()
    }

    fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS location_memory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid CHARACTER(36) NOT NULL,
                name TEXT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                description TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            );
        """
        sqliteClient.update(sql)
        
        // Create index for faster lookups
        val indexSql = "CREATE INDEX IF NOT EXISTS idx_location_uuid ON location_memory(uuid);"
        sqliteClient.update(indexSql)
    }

    fun insert(location: LocationMemory, maxLocations: Int) {
        // Delete oldest if at limit
        val existing = selectByUuid(location.uuid, maxLocations)
        if (existing.size >= maxLocations) {
            val oldest = existing.minByOrNull { it.timestamp }
            oldest?.let { delete(it.uuid, it.name) }
        }
        
        val statement = sqliteClient.buildPreparedStatement(
            "INSERT INTO location_memory (uuid, name, x, y, z, description, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
        )
        statement?.setString(1, location.uuid.toString())
        statement?.setString(2, location.name)
        statement?.setInt(3, location.x)
        statement?.setInt(4, location.y)
        statement?.setInt(5, location.z)
        statement?.setString(6, location.description)
        statement?.setLong(7, location.timestamp)
        sqliteClient.update(statement)
    }

    fun selectByUuid(uuid: UUID, maxLocations: Int = 10): List<LocationMemory> {
        val sql = "SELECT * FROM location_memory WHERE uuid = '%s' ORDER BY timestamp DESC LIMIT %d".format(uuid.toString(), maxLocations)
        return executeAndProcessLocations(sql)
    }

    fun delete(uuid: UUID, name: String) {
        val sql = "DELETE FROM location_memory WHERE uuid = '%s' AND name = '%s'".format(uuid.toString(), name.replace("'", "''"))
        sqliteClient.update(sql)
    }

    fun deleteByUuid(uuid: UUID) {
        val sql = "DELETE FROM location_memory WHERE uuid = '%s'".format(uuid.toString())
        sqliteClient.update(sql)
    }

    private fun executeAndProcessLocations(sql: String): List<LocationMemory> {
        val result = sqliteClient.query(sql)
        val locations = arrayListOf<LocationMemory>()

        if (result == null) return emptyList()

        while (result.next()) {
            val location = LocationMemory(
                UUID.fromString(result.getString("uuid")),
                result.getString("name"),
                result.getInt("x"),
                result.getInt("y"),
                result.getInt("z"),
                result.getString("description"),
                result.getLong("timestamp")
            )
            locations.add(location)
        }
        result.close()
        return locations
    }
}

