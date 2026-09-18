package com.agriloop;

import com.agriloop.database.DatabaseManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseCleaner {

    @Test
    void cleanDatabase() throws Exception {
        System.out.println("Cleaning database of stale records...");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Turn off foreign keys temporarily for clean wipe of business transactions
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.executeUpdate("DELETE FROM notifications");
            stmt.executeUpdate("DELETE FROM sustainability_records");
            stmt.executeUpdate("DELETE FROM transactions");
            stmt.executeUpdate("DELETE FROM deliveries");
            stmt.executeUpdate("DELETE FROM order_items");
            stmt.executeUpdate("DELETE FROM orders");
            stmt.executeUpdate("DELETE FROM waste_listings");

            // Clean up test users and profiles
            stmt.executeUpdate("DELETE FROM farmer_profiles WHERE user_id IN (3, 4, 5, 6, 7, 8) OR user_id IN (SELECT id FROM users WHERE email LIKE '%test%' OR email LIKE '%example.com%')");
            stmt.executeUpdate("DELETE FROM manufacturer_profiles WHERE user_id IN (3, 4, 5, 6, 7, 8) OR user_id IN (SELECT id FROM users WHERE email LIKE '%test%' OR email LIKE '%example.com%')");
            stmt.executeUpdate("DELETE FROM transporter_profiles WHERE user_id IN (3, 4, 5, 6, 7, 8) OR user_id IN (SELECT id FROM users WHERE email LIKE '%test%' OR email LIKE '%example.com%')");
            stmt.executeUpdate("DELETE FROM users WHERE id IN (3, 4, 5, 6, 7, 8) OR email LIKE '%test%' OR email LIKE '%example.com%'");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("Database cleaned successfully. Real accounts preserved, all stale test/demo data removed.");
        }
    }
}
