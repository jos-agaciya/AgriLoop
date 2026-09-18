package com.agriloop;

import com.agriloop.database.DatabaseManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class DbInspector {

    @Test
    void inspectAllTables() throws Exception {
        System.out.println("================ DATABASE INSPECTOR ================");
        String[] tables = {
            "users", "farmer_profiles", "manufacturer_profiles", "transporter_profiles",
            "waste_listings", "orders", "order_items", "deliveries",
            "transactions", "sustainability_records", "notifications"
        };

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            for (String tbl : tables) {
                System.out.println("\n--- TABLE: " + tbl + " ---");
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tbl)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        StringBuilder sb = new StringBuilder("Row " + count + ": ");
                        for (int i = 1; i <= cols; i++) {
                            sb.append(md.getColumnName(i)).append("=").append(rs.getString(i)).append(" | ");
                        }
                        System.out.println(sb);
                    }
                    if (count == 0) {
                        System.out.println("[EMPTY TABLE]");
                    }
                }
            }
        }
        System.out.println("================ END INSPECTION ================");
    }
}
