package me.prskid1000.craftagent.database;

import me.prskid1000.craftagent.CraftAgent;
import java.io.File;
import java.sql.*;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SQLite client for managing the database.
 */
public class SqliteClient {

	private static final Logger LOGGER = LogManager.getLogger(SqliteClient.class);
	private Connection connection;
	private String databasePath;
	private String databaseName;

	/**
	 * Create the database.
	 */
	public void initDatabase(String databaseName) {
		this.databaseName = databaseName;
		this.databasePath = initDataBaseDir();
		try {
			String jdbcUrl = String.format("jdbc:sqlite:%s/%s.db", databasePath, databaseName);
			connection = DriverManager.getConnection(jdbcUrl);
			if (connection.isValid(3)) {
				LOGGER.info("Connected to database at: {}", databasePath);
			}
		} catch (SQLException e) {
			LOGGER.error("Error creating/connecting to database: {}", e.getMessage());
		}
	}

	private String initDataBaseDir() {
		File configDir = FabricLoader.getInstance().getConfigDir().toFile();
		File sqlDbDir = new File(configDir, CraftAgent.MOD_ID);
		if (sqlDbDir.mkdirs()) {
			LOGGER.info("Database directory created at: {}", sqlDbDir.getAbsolutePath());
		}
		return sqlDbDir.getAbsolutePath();
	}

	/**
	 * Select entries from db.
	 * @param sql the SQL query
	 * @return ResultSet or null if error occurred
	 */
	public ResultSet query(String sql) {
		try {
			if (connection == null || connection.isClosed()) {
				LOGGER.error("Database connection is null or closed");
				return null;
			}
			Statement statement = connection.createStatement();
			statement.closeOnCompletion();
			return statement.executeQuery(sql);
		} catch (SQLException e) {
			LOGGER.error("Error executing query: {}", sql, e);
			return null;
		}
	}

	/**
	 * Execute prepared statement.
	 * @param statement the prepared statement
	 */
	public void update(PreparedStatement statement) {
		if (statement == null) {
			LOGGER.error("PreparedStatement is null");
			return;
		}
		try {
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Error executing prepared statement: {}", e.getMessage(), e);
		} finally {
			try {
				statement.close();
			} catch (SQLException e) {
				LOGGER.error("Error closing prepared statement: {}", e.getMessage());
			}
		}
	}

	public PreparedStatement buildPreparedStatement(String sql) {
		try {
			if (connection == null || connection.isClosed()) {
				LOGGER.error("Database connection is null or closed");
				return null;
			}
			return connection.prepareStatement(sql);
		} catch (SQLException e) {
			LOGGER.error("Error building prepared statement: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Create a table in the database.
	 * @param sql the SQL query to create a table
	 * @throws SQLException if the query fails (for migration error handling)
	 */
	public void update(String sql) throws SQLException {
		if (connection == null || connection.isClosed()) {
			throw new SQLException("Database connection is null or closed");
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		} catch (SQLException e) {
			LOGGER.error("Error executing query {} : {}", sql, e.getMessage());
			throw e; // Re-throw to allow migration error handling
		}
	}

	/**
	 * Check if a table exists in the database.
	 * @param tableName the name of the table to check
	 * @return true if the table exists, false otherwise
	 */
	public boolean tableExists(String tableName) {
		try {
			if (connection == null || connection.isClosed()) {
				return false;
			}
			DatabaseMetaData metaData = connection.getMetaData();
			ResultSet tables = metaData.getTables(null, null, tableName, null);
			boolean exists = tables.next();
			tables.close();
			return exists;
		} catch (SQLException e) {
			LOGGER.error("Error checking if table {} exists: {}", tableName, e.getMessage());
			return false;
		}
	}

	/**
	 * Drop a table if it exists. This will also automatically drop all indexes on the table.
	 * @param tableName the name of the table to drop
	 * @throws SQLException if the operation fails
	 */
	public void dropTable(String tableName) throws SQLException {
		if (tableExists(tableName)) {
			update("DROP TABLE " + tableName);
			LOGGER.info("Dropped table: {}", tableName);
		}
	}

	/**
	 * Initialize the schema version tracking table.
	 * @throws SQLException if the operation fails
	 */
	public void initSchemaVersionTable() throws SQLException {
		String sql = """
			CREATE TABLE IF NOT EXISTS schema_versions (
				table_name TEXT PRIMARY KEY,
				schema_hash TEXT NOT NULL
			);
		""";
		update(sql);
	}

	/**
	 * Get the stored schema hash for a table.
	 * @param tableName the name of the table
	 * @return the schema hash, or null if not found
	 */
	public String getSchemaVersion(String tableName) {
		try {
			initSchemaVersionTable();
			PreparedStatement stmt = buildPreparedStatement("SELECT schema_hash FROM schema_versions WHERE table_name = ?");
			if (stmt == null) {
				return null;
			}
			stmt.setString(1, tableName);
			ResultSet rs = stmt.executeQuery();
			if (rs != null && rs.next()) {
				String hash = rs.getString("schema_hash");
				rs.close();
				stmt.close();
				return hash;
			}
			if (rs != null) {
				rs.close();
			}
			stmt.close();
			return null;
		} catch (SQLException e) {
			LOGGER.error("Error getting schema version for table {}: {}", tableName, e.getMessage());
			return null;
		}
	}

	/**
	 * Store the schema hash for a table.
	 * @param tableName the name of the table
	 * @param schemaHash the hash of the schema
	 * @throws SQLException if the operation fails
	 */
	public void setSchemaVersion(String tableName, String schemaHash) throws SQLException {
		initSchemaVersionTable();
		// Use INSERT OR REPLACE to update if exists
		PreparedStatement stmt = buildPreparedStatement("INSERT OR REPLACE INTO schema_versions (table_name, schema_hash) VALUES (?, ?)");
		if (stmt != null) {
			stmt.setString(1, tableName);
			stmt.setString(2, schemaHash);
			stmt.executeUpdate();
			stmt.close();
		}
	}

	/**
	 * Calculate a simple hash of the schema SQL (for versioning).
	 * @param schemaSql the CREATE TABLE SQL statement
	 * @return a hash string representing the schema
	 */
	public String calculateSchemaHash(String schemaSql) {
		// Normalize the SQL (remove whitespace, convert to lowercase) for consistent hashing
		String normalized = schemaSql.replaceAll("\\s+", " ").trim().toLowerCase();
		// Use a simple hash code (can be improved with proper hashing, but this works for schema changes)
		return String.valueOf(normalized.hashCode());
	}

	/**
	 * Check if the table schema needs to be recreated.
	 * @param tableName the name of the table
	 * @param schemaSql the CREATE TABLE SQL statement
	 * @return true if the schema has changed and table needs to be recreated
	 */
	public boolean needsSchemaUpdate(String tableName, String schemaSql) {
		if (!tableExists(tableName)) {
			return true; // Table doesn't exist, need to create it
		}
		
		String currentHash = getSchemaVersion(tableName);
		String expectedHash = calculateSchemaHash(schemaSql);
		
		// If no version stored, table exists but we don't know its schema version
		// Recreate it to ensure it matches current schema and store the version
		if (currentHash == null) {
			return true; // No version stored - drop table and recreate with version
		}
		
		return !currentHash.equals(expectedHash); // Schema changed if hashes don't match
	}

	/**
	 * Delete the database file and close the connection.
	 * Used when migration errors occur to reset the database.
	 */
	public void deleteDatabase() {
		try {
			// Close connection first
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
			connection = null;
			
			// Delete database file
			if (databasePath != null && databaseName != null) {
				File dbFile = new File(databasePath, databaseName + ".db");
				if (dbFile.exists()) {
					if (dbFile.delete()) {
						LOGGER.warn("Database deleted due to migration error: {}", dbFile.getAbsolutePath());
					} else {
						LOGGER.error("Failed to delete database file: {}", dbFile.getAbsolutePath());
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error closing database connection before deletion: {}", e.getMessage());
		}
	}

	/**
	 * Close the database connection.
	 */
	public void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
				LOGGER.info("Database connection closed.");
			}
		} catch (SQLException e) {
			LOGGER.error("Error closing database connection: {}", e.getMessage());
		}
	}
}
