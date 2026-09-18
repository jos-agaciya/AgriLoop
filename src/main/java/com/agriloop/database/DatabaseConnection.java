package com.agriloop.database;

import com.agriloop.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Raw JDBC connection utilities, standalone testing, and diagnostic helpers.
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            logger.info("MySQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC Driver not found in classpath!", e);
        }
    }

    /**
     * Attempts a real direct JDBC connection using the supplied configuration parameters.
     */
    public static ConnectionResult testConnection(String host, int port, String databaseName, String username, String password) {
        long startTime = System.currentTimeMillis();
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=5000&serverTimezone=UTC&characterEncoding=UTF-8",
            host, port, databaseName
        );

        logger.info("Testing JDBC connection to: {}:{} (database: {}, user: {})", host, port, databaseName, username);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            long latency = System.currentTimeMillis() - startTime;
            DatabaseMetaData meta = conn.getMetaData();
            String serverVersion = meta.getDatabaseProductVersion();
            String catalog = conn.getCatalog();

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    logger.info("Successfully connected to MySQL {} in {}ms", serverVersion, latency);
                    return ConnectionResult.success("Connection test successful. Database reachable.", latency, serverVersion, catalog);
                }
            }
            return ConnectionResult.success("Connected to database successfully.", latency, serverVersion, catalog);
        } catch (SQLException e) {
            long latency = System.currentTimeMillis() - startTime;
            logger.warn("Database connection test failed (code: {}): {}", e.getErrorCode(), e.getMessage());

            // Handle specific case where MySQL server is reachable, but the specific database does not exist yet
            if (e.getErrorCode() == 1049) { // ER_BAD_DB_ERROR
                return testServerOnlyConnection(host, port, username, password, databaseName);
            }

            return ConnectionResult.failure(
                getFriendlyErrorMessage(e),
                String.format("SQLState: %s, ErrorCode: %d, Message: %s", e.getSQLState(), e.getErrorCode(), e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Unexpected error during connection test", e);
            return ConnectionResult.failure("Unexpected connection error: " + e.getMessage(), e.toString());
        }
    }

    /**
     * Tests server connectivity when the specific database might not exist yet.
     */
    public static ConnectionResult testServerOnlyConnection(String host, int port, String username, String password, String targetDb) {
        long startTime = System.currentTimeMillis();
        String serverUrl = String.format(
            "jdbc:mysql://%s:%d/?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=5000&serverTimezone=UTC&characterEncoding=UTF-8",
            host, port
        );

        try (Connection conn = DriverManager.getConnection(serverUrl, username, password)) {
            long latency = System.currentTimeMillis() - startTime;
            DatabaseMetaData meta = conn.getMetaData();
            String serverVersion = meta.getDatabaseProductVersion();
            return ConnectionResult.success(
                String.format("MySQL Server reachable, but database '%s' has not been created yet. You can initialize the schema.", targetDb),
                latency, serverVersion, "none"
            );
        } catch (SQLException e) {
            return ConnectionResult.failure(getFriendlyErrorMessage(e), e.getMessage());
        }
    }

    /**
     * Translates raw SQL errors into clean human-friendly guidance.
     */
    public static String getFriendlyErrorMessage(SQLException e) {
        int errorCode = e.getErrorCode();
        return switch (errorCode) {
            case 0 -> "Could not connect to MySQL server at the specified host and port. Please ensure MySQL is running.";
            case 1045 -> "Access denied (Authentication failure). Please check your username and password.";
            case 1049 -> "Unknown database. The specified database does not exist on the server.";
            case 1044 -> "User has insufficient permissions to access the database.";
            case 2003, 2002 -> "Can't connect to MySQL server on host. Check firewall and port settings.";
            default -> "Database error: " + e.getMessage();
        };
    }

    /**
     * Closes any AutoCloseable or JDBC resource safely without throwing exceptions.
     */
    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                logger.trace("Error closing resource", e);
            }
        }
    }
}
