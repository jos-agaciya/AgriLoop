package com.agriloop.repository.impl;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.DatabaseException;
import com.agriloop.model.Notification;
import com.agriloop.repository.NotificationRepository;
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
 * Real JDBC implementation of NotificationRepository.
 * Manages user-specific alert records persisted in MySQL.
 */
public class JdbcNotificationRepository implements NotificationRepository {
    private static final Logger logger = LoggerFactory.getLogger(JdbcNotificationRepository.class);

    public JdbcNotificationRepository() {
        ensureTableExists();
    }

    private void ensureTableExists() {
        String ddl = """
            CREATE TABLE IF NOT EXISTS `notifications` (
                `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                `recipient_user_id` BIGINT UNSIGNED NOT NULL,
                `notification_type` VARCHAR(50) NOT NULL,
                `title` VARCHAR(200) NOT NULL,
                `message` TEXT NOT NULL,
                `related_order_id` BIGINT UNSIGNED NULL,
                `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX `idx_notifications_recipient` (`recipient_user_id`),
                INDEX `idx_notifications_read` (`is_read`),
                INDEX `idx_notifications_created` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        } catch (SQLException e) {
            logger.warn("Could not auto-verify notifications table: {}", e.getMessage());
        }
    }

    @Override
    public Optional<Notification> findById(Long id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find notification by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Notification> findAll() {
        String sql = "SELECT * FROM notifications ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load notifications", e);
        }
        return list;
    }

    @Override
    public List<Notification> findByRecipientUserId(Long recipientUserId) {
        String sql = "SELECT * FROM notifications WHERE recipient_user_id = ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipientUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find notifications for user: " + recipientUserId, e);
        }
        return list;
    }

    @Override
    public List<Notification> findUnreadByRecipientUserId(Long recipientUserId) {
        String sql = "SELECT * FROM notifications WHERE recipient_user_id = ? AND is_read = FALSE ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipientUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find unread notifications for user: " + recipientUserId, e);
        }
        return list;
    }

    @Override
    public int countUnreadByRecipientUserId(Long recipientUserId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE recipient_user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipientUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count unread notifications for user: " + recipientUserId, e);
        }
        return 0;
    }

    @Override
    public Notification save(Notification notification) {
        if (notification.getId() == null) {
            return insert(notification);
        } else {
            return update(notification);
        }
    }

    private Notification insert(Notification n) {
        String sql = """
            INSERT INTO notifications (recipient_user_id, notification_type, title, message, related_order_id, is_read, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, n.getRecipientUserId());
            stmt.setString(2, n.getNotificationType());
            stmt.setString(3, n.getTitle());
            stmt.setString(4, n.getMessage());
            if (n.getRelatedOrderId() != null) {
                stmt.setLong(5, n.getRelatedOrderId());
            } else {
                stmt.setNull(5, java.sql.Types.BIGINT);
            }
            stmt.setBoolean(6, n.isRead());
            stmt.setTimestamp(7, Timestamp.valueOf(n.getCreatedAt() != null ? n.getCreatedAt() : LocalDateTime.now()));

            stmt.executeUpdate();
            try (ResultSet gk = stmt.getGeneratedKeys()) {
                if (gk.next()) {
                    n.setId(gk.getLong(1));
                }
            }
            return n;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert notification: " + e.getMessage(), e);
        }
    }

    private Notification update(Notification n) {
        String sql = "UPDATE notifications SET is_read = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, n.isRead());
            stmt.setLong(2, n.getId());
            stmt.executeUpdate();
            return n;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update notification: " + n.getId(), e);
        }
    }

    @Override
    public boolean markAsRead(Long notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark notification as read: " + notificationId, e);
        }
    }

    @Override
    public boolean markAllAsReadForUser(Long recipientUserId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE recipient_user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipientUserId);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark all notifications as read for user: " + recipientUserId, e);
        }
    }

    @Override
    public boolean deleteAllForUser(Long recipientUserId) {
        String sql = "DELETE FROM notifications WHERE recipient_user_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipientUserId);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete notifications for user: " + recipientUserId, e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete notification: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM notifications";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count notifications", e);
        }
        return 0;
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setRecipientUserId(rs.getLong("recipient_user_id"));
        n.setNotificationType(rs.getString("notification_type"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        long orderId = rs.getLong("related_order_id");
        if (!rs.wasNull()) {
            n.setRelatedOrderId(orderId);
        }
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            n.setCreatedAt(ts.toLocalDateTime());
        }
        return n;
    }
}
