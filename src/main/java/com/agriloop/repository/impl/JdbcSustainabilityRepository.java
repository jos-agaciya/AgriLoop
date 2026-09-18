package com.agriloop.repository.impl;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.DatabaseException;
import com.agriloop.model.SustainabilityRecord;
import com.agriloop.repository.SustainabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Real JDBC implementation of SustainabilityRepository.
 */
public class JdbcSustainabilityRepository implements SustainabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(JdbcSustainabilityRepository.class);

    @Override
    public Optional<SustainabilityRecord> findById(Long id) {
        String sql = "SELECT * FROM sustainability_records WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRecord(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find sustainability record by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<SustainabilityRecord> findAll() {
        String sql = "SELECT * FROM sustainability_records ORDER BY calculation_date DESC, id DESC";
        List<SustainabilityRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRecord(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all sustainability records", e);
        }
        return list;
    }

    @Override
    public List<SustainabilityRecord> findByUserId(Long userId) {
        String sql = "SELECT * FROM sustainability_records WHERE user_id = ? ORDER BY calculation_date DESC";
        List<SustainabilityRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRecord(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch sustainability records for user: " + userId, e);
        }
        return list;
    }

    @Override
    public BigDecimal getCo2SavedByUserId(Long userId) {
        if (userId == null) return BigDecimal.ZERO;
        String sql = "SELECT COALESCE(SUM(co2_saved_kg), 0) FROM sustainability_records WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            logger.warn("Could not query CO2 sum for user {}: {}", userId, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWasteDivertedByUserId(Long userId) {
        if (userId == null) return BigDecimal.ZERO;
        String sql = "SELECT COALESCE(SUM(waste_diverted_tons), 0) FROM sustainability_records WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            logger.warn("Could not query waste diverted sum for user {}: {}", userId, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalWasteDivertedTons() {
        return queryAggregateSum("SELECT COALESCE(SUM(waste_diverted_tons), 0) FROM sustainability_records");
    }

    @Override
    public BigDecimal getTotalCo2SavedKg() {
        return queryAggregateSum("SELECT COALESCE(SUM(co2_saved_kg), 0) FROM sustainability_records");
    }

    @Override
    public BigDecimal getTotalMethanePreventedKg() {
        return queryAggregateSum("SELECT COALESCE(SUM(methane_prevented_kg), 0) FROM sustainability_records");
    }

    @Override
    public BigDecimal getTotalEnergyGeneratedKwh() {
        return queryAggregateSum("SELECT COALESCE(SUM(energy_generated_kwh), 0) FROM sustainability_records");
    }

    private BigDecimal queryAggregateSum(String sql) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            logger.warn("Could not query aggregate sum: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public SustainabilityRecord save(SustainabilityRecord record) {
        if (record.getId() == null) {
            String sql = "INSERT INTO sustainability_records (user_id, waste_diverted_tons, co2_saved_kg, methane_prevented_kg, energy_generated_kwh, calculation_date) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, record.getUserId());
                stmt.setBigDecimal(2, record.getWasteDivertedTons());
                stmt.setBigDecimal(3, record.getCo2SavedKg());
                stmt.setBigDecimal(4, record.getMethanePreventedKg());
                stmt.setBigDecimal(5, record.getEnergyGeneratedKwh());
                stmt.setDate(6, Date.valueOf(record.getCalculationDate()));

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) record.setId(rs.getLong(1));
                }
                record.setCreatedAt(LocalDateTime.now());
                return record;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert sustainability record", e);
            }
        } else {
            String sql = "UPDATE sustainability_records SET waste_diverted_tons = ?, co2_saved_kg = ?, methane_prevented_kg = ?, energy_generated_kwh = ?, calculation_date = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBigDecimal(1, record.getWasteDivertedTons());
                stmt.setBigDecimal(2, record.getCo2SavedKg());
                stmt.setBigDecimal(3, record.getMethanePreventedKg());
                stmt.setBigDecimal(4, record.getEnergyGeneratedKwh());
                stmt.setDate(5, Date.valueOf(record.getCalculationDate()));
                stmt.setLong(6, record.getId());
                stmt.executeUpdate();
                return record;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update sustainability record: " + record.getId(), e);
            }
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM sustainability_records WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete sustainability record by ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM sustainability_records";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count sustainability records", e);
        }
        return 0;
    }

    private SustainabilityRecord mapRecord(ResultSet rs) throws SQLException {
        SustainabilityRecord r = new SustainabilityRecord();
        r.setId(rs.getLong("id"));
        r.setUserId(rs.getLong("user_id"));
        r.setWasteDivertedTons(rs.getBigDecimal("waste_diverted_tons"));
        r.setCo2SavedKg(rs.getBigDecimal("co2_saved_kg"));
        r.setMethanePreventedKg(rs.getBigDecimal("methane_prevented_kg"));
        r.setEnergyGeneratedKwh(rs.getBigDecimal("energy_generated_kwh"));

        Date d = rs.getDate("calculation_date");
        if (d != null) r.setCalculationDate(d.toLocalDate());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) r.setCreatedAt(created.toLocalDateTime());

        return r;
    }
}
