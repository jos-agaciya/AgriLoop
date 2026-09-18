package com.agriloop.repository.impl;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.DatabaseException;
import com.agriloop.model.Order;
import com.agriloop.model.OrderItem;
import com.agriloop.model.enums.OrderStatus;
import com.agriloop.model.enums.PaymentStatus;
import com.agriloop.repository.OrderRepository;
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
 * Real JDBC implementation of OrderRepository.
 */
public class JdbcOrderRepository implements OrderRepository {
    private static final Logger logger = LoggerFactory.getLogger(JdbcOrderRepository.class);

    @Override
    public Optional<Order> findById(Long id) {
        String sql = "SELECT o.*, b.full_name as buyer_name, s.full_name as seller_name FROM orders o " +
                     "JOIN users b ON o.buyer_id = b.id JOIN users s ON o.seller_id = s.id WHERE o.id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(findItemsForOrder(conn, order.getId()));
                    return Optional.of(order);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find order by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        String sql = "SELECT o.*, b.full_name as buyer_name, s.full_name as seller_name FROM orders o " +
                     "JOIN users b ON o.buyer_id = b.id JOIN users s ON o.seller_id = s.id WHERE o.order_number = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(findItemsForOrder(conn, order.getId()));
                    return Optional.of(order);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find order by number: " + orderNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Order> findAll() {
        String sql = "SELECT o.*, b.full_name as buyer_name, s.full_name as seller_name FROM orders o " +
                     "JOIN users b ON o.buyer_id = b.id JOIN users s ON o.seller_id = s.id ORDER BY o.id DESC";
        List<Order> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all orders", e);
        }
        return list;
    }

    @Override
    public List<Order> findByBuyerId(Long buyerId) {
        String sql = "SELECT o.*, b.full_name as buyer_name, s.full_name as seller_name FROM orders o " +
                     "JOIN users b ON o.buyer_id = b.id JOIN users s ON o.seller_id = s.id WHERE o.buyer_id = ? ORDER BY o.id DESC";
        List<Order> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, buyerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch orders for buyer: " + buyerId, e);
        }
        return list;
    }

    @Override
    public List<Order> findBySellerId(Long sellerId) {
        String sql = "SELECT o.*, b.full_name as buyer_name, s.full_name as seller_name FROM orders o " +
                     "JOIN users b ON o.buyer_id = b.id JOIN users s ON o.seller_id = s.id WHERE o.seller_id = ? ORDER BY o.id DESC";
        List<Order> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch orders for seller: " + sellerId, e);
        }
        return list;
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        String sql = "SELECT o.*, b.full_name as buyer_name, s.full_name as seller_name FROM orders o " +
                     "JOIN users b ON o.buyer_id = b.id JOIN users s ON o.seller_id = s.id WHERE o.status = ? ORDER BY o.id DESC";
        List<Order> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch orders by status: " + status, e);
        }
        return list;
    }

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            String sql = "INSERT INTO orders (order_number, buyer_id, seller_id, status, total_amount, payment_status, delivery_address, notes) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, order.getOrderNumber());
                stmt.setLong(2, order.getBuyerId());
                stmt.setLong(3, order.getSellerId());
                stmt.setString(4, order.getStatus().name());
                stmt.setBigDecimal(5, order.getTotalAmount());
                stmt.setString(6, order.getPaymentStatus().name());
                stmt.setString(7, order.getDeliveryAddress());
                stmt.setString(8, order.getNotes());

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) order.setId(rs.getLong(1));
                }

                // Save items
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    saveOrderItems(conn, order.getId(), order.getItems());
                }

                order.setCreatedAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                return order;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert order: " + order.getOrderNumber(), e);
            }
        } else {
            String sql = "UPDATE orders SET status = ?, total_amount = ?, payment_status = ?, delivery_address = ?, notes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, order.getStatus().name());
                stmt.setBigDecimal(2, order.getTotalAmount());
                stmt.setString(3, order.getPaymentStatus().name());
                stmt.setString(4, order.getDeliveryAddress());
                stmt.setString(5, order.getNotes());
                stmt.setLong(6, order.getId());

                stmt.executeUpdate();
                order.setUpdatedAt(LocalDateTime.now());
                return order;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update order: " + order.getId(), e);
            }
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM orders WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete order by ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count orders", e);
        }
        return 0;
    }

    private List<OrderItem> findItemsForOrder(Connection conn, Long orderId) throws SQLException {
        String sql = "SELECT oi.*, l.title as listing_title FROM order_items oi " +
                     "JOIN waste_listings l ON oi.listing_id = l.id WHERE oi.order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getLong("id"));
                    item.setOrderId(rs.getLong("order_id"));
                    item.setListingId(rs.getLong("listing_id"));
                    item.setQuantityTons(rs.getBigDecimal("quantity_tons"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    item.setListingTitle(rs.getString("listing_title"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private void saveOrderItems(Connection conn, Long orderId, List<OrderItem> items) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, listing_id, quantity_tons, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (OrderItem item : items) {
                stmt.setLong(1, orderId);
                stmt.setLong(2, item.getListingId());
                stmt.setBigDecimal(3, item.getQuantityTons());
                stmt.setBigDecimal(4, item.getUnitPrice());
                stmt.setBigDecimal(5, item.getSubtotal());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setOrderNumber(rs.getString("order_number"));
        o.setBuyerId(rs.getLong("buyer_id"));
        o.setSellerId(rs.getLong("seller_id"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        o.setDeliveryAddress(rs.getString("delivery_address"));
        o.setNotes(rs.getString("notes"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) o.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) o.setUpdatedAt(updated.toLocalDateTime());

        try {
            o.setBuyerName(rs.getString("buyer_name"));
            o.setSellerName(rs.getString("seller_name"));
        } catch (Exception ignored) {}

        return o;
    }
}
