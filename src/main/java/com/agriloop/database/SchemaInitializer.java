package com.agriloop.database;

import com.agriloop.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Automates execution of the initial database schema (schema.sql).
 * Can create the database and all 10 core tables safely.
 */
public class SchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaInitializer.class);
    private static final String SCHEMA_FILE = "schema.sql";

    public static class SchemaInitResult {
        public final boolean success;
        public final int statementsExecuted;
        public final String message;
        public final List<String> errors;

        public SchemaInitResult(boolean success, int statementsExecuted, String message, List<String> errors) {
            this.success = success;
            this.statementsExecuted = statementsExecuted;
            this.message = message;
            this.errors = errors;
        }
    }

    /**
     * Initializes the AgriLoop database schema using raw JDBC connection to MySQL server.
     */
    public static SchemaInitResult initializeSchema() {
        DatabaseConfig config = DatabaseConfig.getInstance();
        List<String> errors = new ArrayList<>();
        int executedCount = 0;

        List<String> statements = loadSqlStatements();
        if (statements.isEmpty()) {
            return new SchemaInitResult(false, 0, "No SQL statements found in schema.sql", List.of("schema.sql is empty or missing"));
        }

        String serverUrl = config.getRootJdbcUrlWithoutDatabase();
        logger.info("Running schema initialization on MySQL Server at {}:{}", config.getHost(), config.getPort());

        try (Connection conn = DriverManager.getConnection(serverUrl, config.getUsername(), config.getPassword())) {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    if (sql.isBlank()) continue;
                    try {
                        stmt.execute(sql);
                        executedCount++;
                    } catch (Exception e) {
                        logger.warn("SQL Statement failed during schema init: {} -> {}", sql.substring(0, Math.min(sql.length(), 60)), e.getMessage());
                        errors.add(e.getMessage());
                    }
                }
            }

            // Re-initialize the pool after schema initialization
            DatabaseManager.getInstance().initializePool();

            if (errors.isEmpty()) {
                logger.info("Schema initialization succeeded. Executed {} statements.", executedCount);
                return new SchemaInitResult(true, executedCount, "Database schema initialized successfully! All tables created.", errors);
            } else {
                return new SchemaInitResult(false, executedCount, "Schema executed with warnings/errors.", errors);
            }
        } catch (Exception e) {
            logger.error("Failed to connect for schema initialization", e);
            errors.add(e.getMessage());
            return new SchemaInitResult(false, executedCount, "Failed to connect to MySQL: " + e.getMessage(), errors);
        }
    }

    private static List<String> loadSqlStatements() {
        List<String> statements = new ArrayList<>();
        try (InputStream in = SchemaInitializer.class.getClassLoader().getResourceAsStream(SCHEMA_FILE)) {
            if (in == null) {
                logger.error("Could not find {} in classpath resources", SCHEMA_FILE);
                return statements;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder currentStmt = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                        continue;
                    }

                    currentStmt.append(line).append("\n");

                    if (trimmed.endsWith(";")) {
                        String finalSql = currentStmt.toString().trim();
                        // Remove trailing semicolon
                        if (finalSql.endsWith(";")) {
                            finalSql = finalSql.substring(0, finalSql.length() - 1);
                        }
                        if (!finalSql.isBlank()) {
                            statements.add(finalSql);
                        }
                        currentStmt.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error reading schema.sql", e);
        }
        return statements;
    }
}
