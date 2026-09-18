package com.agriloop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized Database Configuration manager.
 * Supports environment variables, configuration properties file, and runtime updates.
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String CONFIG_FILE_NAME = "database.properties";

    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;
    private int maxPoolSize;
    private int minIdle;
    private long connectionTimeoutMs;
    private long idleTimeoutMs;

    private static DatabaseConfig instance;

    private DatabaseConfig() {
        loadConfiguration();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public synchronized void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try loading from working directory file
        File localFile = new File(CONFIG_FILE_NAME);
        if (localFile.exists()) {
            try (InputStream in = new FileInputStream(localFile)) {
                props.load(in);
                logger.info("Loaded database configuration from local file: {}", localFile.getAbsolutePath());
            } catch (Exception e) {
                logger.warn("Could not read local database.properties: {}", e.getMessage());
            }
        } else {
            // 2. Try loading from classpath resources
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                if (in != null) {
                    props.load(in);
                    logger.info("Loaded database configuration from classpath resource.");
                }
            } catch (Exception e) {
                logger.warn("Could not read classpath database.properties: {}", e.getMessage());
            }
        }

        // Apply properties with default fallbacks
        this.host = props.getProperty("db.host", "localhost");
        this.port = parseInt(props.getProperty("db.port"), 3306);
        this.databaseName = props.getProperty("db.name", "agriloop_db");
        this.username = props.getProperty("db.user", "root");
        this.password = props.getProperty("db.password", "");
        this.maxPoolSize = parseInt(props.getProperty("db.pool.max_size"), 10);
        this.minIdle = parseInt(props.getProperty("db.pool.min_idle"), 2);
        this.connectionTimeoutMs = parseLong(props.getProperty("db.pool.connection_timeout"), 5000L);
        this.idleTimeoutMs = parseLong(props.getProperty("db.pool.idle_timeout"), 600000L);

        // 3. Environment variables override file properties if present
        String envHost = System.getenv("DB_HOST");
        if (envHost != null && !envHost.isBlank()) this.host = envHost.trim();

        String envPort = System.getenv("DB_PORT");
        if (envPort != null && !envPort.isBlank()) this.port = parseInt(envPort, this.port);

        String envDb = System.getenv("DB_NAME");
        if (envDb != null && !envDb.isBlank()) this.databaseName = envDb.trim();

        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.isBlank()) this.username = envUser.trim();

        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) this.password = envPass;
    }

    public synchronized void saveToFile() {
        Properties props = new Properties();
        props.setProperty("db.host", host != null ? host : "localhost");
        props.setProperty("db.port", String.valueOf(port));
        props.setProperty("db.name", databaseName != null ? databaseName : "agriloop_db");
        props.setProperty("db.user", username != null ? username : "root");
        props.setProperty("db.password", password != null ? password : "");
        props.setProperty("db.pool.max_size", String.valueOf(maxPoolSize));
        props.setProperty("db.pool.min_idle", String.valueOf(minIdle));
        props.setProperty("db.pool.connection_timeout", String.valueOf(connectionTimeoutMs));
        props.setProperty("db.pool.idle_timeout", String.valueOf(idleTimeoutMs));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE_NAME)) {
            props.store(out, "AgriLoop Database Configuration - Updated at runtime");
            logger.info("Saved database configuration to {}", CONFIG_FILE_NAME);
        } catch (Exception e) {
            logger.error("Failed to save database configuration to file: {}", e.getMessage());
        }
    }

    public String getJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
            host, port, databaseName
        );
    }

    public String getRootJdbcUrlWithoutDatabase() {
        return String.format(
            "jdbc:mysql://%s:%d/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
            host, port
        );
    }

    private int parseInt(String val, int defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private long parseLong(String val, long defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // Getters and Setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getMaxPoolSize() { return maxPoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }

    public int getMinIdle() { return minIdle; }
    public void setMinIdle(int minIdle) { this.minIdle = minIdle; }

    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }

    public long getIdleTimeoutMs() { return idleTimeoutMs; }
    public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }
}
