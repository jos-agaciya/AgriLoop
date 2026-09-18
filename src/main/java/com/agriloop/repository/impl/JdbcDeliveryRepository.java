package com.agriloop.repository.impl;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.DatabaseException;
import com.agriloop.model.Delivery;
import com.agriloop.model.enums.DeliveryStatus;
import com.agriloop.repository.DeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
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
 * Real JDBC implementation of DeliveryRepository.
 */
public class JdbcDeliveryRepository implements DeliveryRepository {
    private static final Logger logger = LoggerFactory.getLogger(JdbcDeliveryRepository.class);

    @Override
    public Optional<Delivery> findById(Long id) {
        String sql = "SELECT d.*, o.order_number, u.full_name as transporter_name FROM deliveries d " +
                     "JOIN orders o ON d.order_id = o.id " +
                     "LEFT JOIN users u ON d.transporter_id = u.id WHERE d.id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDelivery(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find delivery by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Delivery> findByOrderId(Long orderId) {
        String sql = "SELECT d.*, o.order_number, u.full_name as transporter_name FROM deliveries d " +
                     "JOIN orders o ON d.order_id = o.id " +
                     "LEFT JOIN users u ON d.transporter_id = u.id WHERE d.order_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDelivery(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find delivery for order: " + orderId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Delivery> findByTrackingCode(String trackingCode) {
        String sql = "SELECT d.*, o.order_number, u.full_name as transporter_name FROM deliveries d " +
                     "JOIN orders o ON d.order_id = o.id " +
                     "LEFT JOIN users u ON d.transporter_id = u.id WHERE d.tracking_code = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trackingCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDelivery(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find delivery by tracking code: " + trackingCode, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Delivery> findAll() {
        String sql = "SELECT d.*, o.order_number, u.full_name as transporter_name FROM deliveries d " +
                     "JOIN orders o ON d.order_id = o.id " +
                     "LEFT JOIN users u ON d.transporter_id = u.id ORDER BY d.id DESC";
        List<Delivery> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapDelivery(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all deliveries", e);
        }
        return list;
    }

    @Override
    public List<Delivery> findByTransporterId(Long transporterId) {
        String sql = "SELECT d.*, o.order_number, u.full_name as transporter_name FROM deliveries d " +
                     "JOIN orders o ON d.order_id = o.id " +
                     "LEFT JOIN users u ON d.transporter_id = u.id WHERE d.transporter_id = ? ORDER BY d.id DESC";
        List<Delivery> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, transporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDelivery(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch deliveries for transporter: " + transporterId, e);
        }
        return list;
    }

    @Override
    public List<Delivery> findByStatus(DeliveryStatus status) {
        String sql = "SELECT d.*, o.order_number, u.full_name as transporter_name FROM deliveries d " +
                     "JOIN orders o ON d.order_id = o.id " +
                     "LEFT JOIN users u ON d.transporter_id = u.id WHERE d.status = ? ORDER BY d.id DESC";
        List<Delivery> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDelivery(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch deliveries by status: " + status, e);
        }
        return list;
    }

    @Override
    public Delivery save(Delivery delivery) {
        if (delivery.getId() == null) {
            String sql = "INSERT INTO deliveries (order_id, transporter_id, pickup_location, delivery_location, " +
                         "distance_km, delivery_cost, status, pickup_time, delivery_time, tracking_code) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, delivery.getOrderId());
                if (delivery.getTransporterId() != null) stmt.setLong(2, delivery.getTransporterId());
                else stmt.setNull(2, java.sql.Types.BIGINT);
                stmt.setString(3, delivery.getPickupLocation());
                stmt.setString(4, delivery.getDeliveryLocation());
                stmt.setBigDecimal(5, delivery.getDistanceKm());
                stmt.setBigDecimal(6, delivery.getDeliveryCost());
                stmt.setString(7, delivery.getStatus().name());
                stmt.setTimestamp(8, delivery.getPickupTime() != null ? Timestamp.valueOf(delivery.getPickupTime()) : null);
                stmt.setTimestamp(9, delivery.getDeliveryTime() != null ? Timestamp.valueOf(delivery.getDeliveryTime()) : null);
                stmt.setString(10, delivery.getTrackingCode());

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) delivery.setId(rs.getLong(1));
                }
                delivery.setCreatedAt(LocalDateTime.now());
                delivery.setUpdatedAt(LocalDateTime.now());
                return delivery;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert delivery: " + delivery.getTrackingCode(), e);
            }
        } else {
            String sql = "UPDATE deliveries SET transporter_id = ?, pickup_location = ?, delivery_location = ?, " +
                         "distance_km = ?, delivery_cost = ?, status = ?, pickup_time = ?, delivery_time = ?, " +
                         "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (delivery.getTransporterId() != null) stmt.setLong(1, delivery.getTransporterId());
                else stmt.setNull(1, java.sql.Types.BIGINT);
                stmt.setString(2, delivery.getPickupLocation());
                stmt.setString(3, delivery.getDeliveryLocation());
                stmt.setBigDecimal(4, delivery.getDistanceKm());
                stmt.setBigDecimal(5, delivery.getDeliveryCost());
                stmt.setString(6, delivery.getStatus().name());
                stmt.setTimestamp(7, delivery.getPickupTime() != null ? Timestamp.valueOf(delivery.getPickupTime()) : null);
                stmt.setTimestamp(8, delivery.getDeliveryTime() != null ? Timestamp.valueOf(delivery.getDeliveryTime()) : null);
                stmt.setLong(9, delivery.getId());

                stmt.executeUpdate();
                delivery.setUpdatedAt(LocalDateTime.now());
                return delivery;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update delivery: " + delivery.getId(), e);
            }
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM deliveries WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete delivery by ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM deliveries";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count deliveries", e);
        }
        return 0;
    }

    private Delivery mapDelivery(ResultSet rs) throws SQLException {
        Delivery d = new Delivery();
        d.setId(rs.getLong("id"));
        d.setOrderId(rs.getLong("order_id"));
        long tId = rs.getLong("transporter_id");
        if (!rs.wasNull()) d.setTransporterId(tId);
        d.setPickupLocation(rs.getString("pickup_location"));
        d.setDeliveryLocation(rs.getString("delivery_location"));
        d.setDistanceKm(rs.getBigDecimal("distance_km"));
        d.setDeliveryCost(rs.getBigDecimal("delivery_cost"));
        d.setStatus(DeliveryStatus.valueOf(rs.getString("status")));

        Timestamp pickup = rs.getTimestamp("pickup_time");
        if (pickup != null) d.setPickupTime(pickup.toLocalDateTime());

        Timestamp delivered = rs.getTimestamp("delivery_time");
        if (delivered != null) d.setDeliveryTime(delivered.toLocalDateTime());

        d.setTrackingCode(rs.getString("tracking_code"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) d.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) d.setUpdatedAt(updated.toLocalDateTime());

        try {
            d.setOrderNumber(rs.getString("order_number"));
            d.setTransporterName(rs.getString("transporter_name"));
        } catch (Exception ignored) {}

        return d;
    }
}
