package com.agriloop.database;

import com.agriloop.config.DatabaseConfig;
import com.agriloop.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * High-performance, thread-safe connection pool manager using HikariCP.
 * Features listener callbacks for live UI status indicators.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;

    private HikariDataSource dataSource;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private String lastStatusMessage = "Not connected";
    private final List<Consumer<Boolean>> statusListeners = new ArrayList<>();

    private DatabaseManager() {
        // Lazy initialization
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initializes or restarts the Hikari connection pool with the current DatabaseConfig.
     */
    public synchronized boolean initializePool() {
        closePool();

        DatabaseConfig config = DatabaseConfig.getInstance();
        logger.info("Initializing HikariCP DataSource for {}:{}", config.getHost(), config.getPort());

        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getJdbcUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
            hikariConfig.setMinimumIdle(config.getMinIdle());
            hikariConfig.setConnectionTimeout(config.getConnectionTimeoutMs());
            hikariConfig.setIdleTimeout(config.getIdleTimeoutMs());
            hikariConfig.setPoolName("AgriLoopHikariPool");

            // Recommended MySQL performance properties
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

            this.dataSource = new HikariDataSource(hikariConfig);

            // Validate pool by acquiring one connection
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(3)) {
                    isConnected.set(true);
                    lastStatusMessage = "Connected to " + config.getDatabaseName();
                    logger.info("HikariCP connection pool initialized successfully.");
                    notifyListeners(true);
                    return true;
                }
            }
        } catch (Exception e) {
            isConnected.set(false);
            lastStatusMessage = "Connection failed: " + e.getMessage();
            logger.warn("HikariCP initialization failed: {}", e.getMessage());
            closePool();
            notifyListeners(false);
            return false;
        }

        isConnected.set(false);
        notifyListeners(false);
        return false;
    }

    /**
     * Retrieves a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            boolean success = initializePool();
            if (!success || dataSource == null) {
                throw new SQLException("Database connection pool is unavailable. " + lastStatusMessage);
            }
        }
        return dataSource.getConnection();
    }

    /**
     * Checks if the database pool is currently active and healthy.
     */
    public boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) {
            initializePool();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            try (Connection conn = dataSource.getConnection()) {
                boolean valid = conn.isValid(2);
                isConnected.set(valid);
                return valid;
            } catch (Exception e) {
                isConnected.set(false);
                return false;
            }
        }
        return false;
    }

    /**
     * Safely tears down the connection pool.
     */
    public synchronized void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                logger.info("HikariCP connection pool closed.");
            } catch (Exception e) {
                logger.error("Error closing connection pool", e);
            } finally {
                dataSource = null;
                isConnected.set(false);
                notifyListeners(false);
            }
        }
    }

    public synchronized void addStatusListener(Consumer<Boolean> listener) {
        if (listener != null) {
            statusListeners.add(listener);
            listener.accept(isConnected.get());
        }
    }

    public synchronized void removeStatusListener(Consumer<Boolean> listener) {
        statusListeners.remove(listener);
    }

    private void notifyListeners(boolean status) {
        for (Consumer<Boolean> listener : statusListeners) {
            try {
                listener.accept(status);
            } catch (Exception e) {
                logger.warn("Error in connection status listener", e);
            }
        }
    }

    public String getLastStatusMessage() {
        return lastStatusMessage;
    }
}
