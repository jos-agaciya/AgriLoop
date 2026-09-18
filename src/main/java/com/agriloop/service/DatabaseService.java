package com.agriloop.service;

import com.agriloop.config.DatabaseConfig;
import com.agriloop.database.ConnectionResult;
import com.agriloop.database.DatabaseConnection;
import com.agriloop.database.DatabaseManager;
import com.agriloop.database.SchemaInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * High-level service for managing database configuration, testing, and schema setup.
 */
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static DatabaseService instance;

    private DatabaseService() {}

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    public ConnectionResult testActiveConnection() {
        DatabaseConfig cfg = DatabaseConfig.getInstance();
        return DatabaseConnection.testConnection(
            cfg.getHost(), cfg.getPort(), cfg.getDatabaseName(), cfg.getUsername(), cfg.getPassword()
        );
    }

    public ConnectionResult testCustomConnection(String host, int port, String database, String user, String pass) {
        return DatabaseConnection.testConnection(host, port, database, user, pass);
    }

    public boolean applyAndSaveConfiguration(String host, int port, String database, String user, String pass) {
        DatabaseConfig cfg = DatabaseConfig.getInstance();
        cfg.setHost(host);
        cfg.setPort(port);
        cfg.setDatabaseName(database);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.saveToFile();

        return DatabaseManager.getInstance().initializePool();
    }

    public SchemaInitializer.SchemaInitResult initializeDatabaseSchema() {
        return SchemaInitializer.initializeSchema();
    }

    public boolean isConnected() {
        return DatabaseManager.getInstance().isConnected();
    }

    public void addStatusListener(Consumer<Boolean> listener) {
        DatabaseManager.getInstance().addStatusListener(listener);
    }

    public void removeStatusListener(Consumer<Boolean> listener) {
        DatabaseManager.getInstance().removeStatusListener(listener);
    }
}
