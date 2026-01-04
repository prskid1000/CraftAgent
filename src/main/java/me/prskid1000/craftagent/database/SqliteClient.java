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
