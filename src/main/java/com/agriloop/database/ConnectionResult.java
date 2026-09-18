package com.agriloop.database;

/**
 * Encapsulates the outcome and diagnostics of a database connection attempt.
 */
public class ConnectionResult {
    private final boolean successful;
    private final String message;
    private final long latencyMs;
    private final String serverVersion;
    private final String targetDatabase;
    private final String errorMessage;

    public ConnectionResult(boolean successful, String message, long latencyMs, String serverVersion, String targetDatabase, String errorMessage) {
        this.successful = successful;
        this.message = message;
        this.latencyMs = latencyMs;
        this.serverVersion = serverVersion;
        this.targetDatabase = targetDatabase;
        this.errorMessage = errorMessage;
    }

    public static ConnectionResult success(String message, long latencyMs, String serverVersion, String targetDatabase) {
        return new ConnectionResult(true, message, latencyMs, serverVersion, targetDatabase, null);
    }

    public static ConnectionResult failure(String message, String errorMessage) {
        return new ConnectionResult(false, message, 0, "Unknown", "None", errorMessage);
    }

    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
    public long getLatencyMs() { return latencyMs; }
    public String getServerVersion() { return serverVersion; }
    public String getTargetDatabase() { return targetDatabase; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public String toString() {
        if (successful) {
            return String.format("Connected to %s (v%s) in %dms - %s", targetDatabase, serverVersion, latencyMs, message);
        } else {
            return String.format("Connection Failed: %s (%s)", message, errorMessage);
        }
    }
}
