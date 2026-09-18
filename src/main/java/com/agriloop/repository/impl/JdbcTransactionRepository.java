package com.agriloop.repository.impl;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.DatabaseException;
import com.agriloop.model.Transaction;
import com.agriloop.repository.TransactionRepository;
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
 * Real JDBC implementation of TransactionRepository.
 */
public class JdbcTransactionRepository implements TransactionRepository {
    private static final Logger logger = LoggerFactory.getLogger(JdbcTransactionRepository.class);

    @Override
    public Optional<Transaction> findById(Long id) {
        String sql = "SELECT t.*, p.full_name as payer_name, e.full_name as payee_name FROM transactions t " +
                     "JOIN users p ON t.payer_id = p.id JOIN users e ON t.payee_id = e.id WHERE t.id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapTransaction(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find transaction by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Transaction> findByTransactionReference(String reference) {
        String sql = "SELECT t.*, p.full_name as payer_name, e.full_name as payee_name FROM transactions t " +
                     "JOIN users p ON t.payer_id = p.id JOIN users e ON t.payee_id = e.id WHERE t.transaction_reference = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reference);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapTransaction(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find transaction by reference: " + reference, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transaction> findAll() {
        String sql = "SELECT t.*, p.full_name as payer_name, e.full_name as payee_name FROM transactions t " +
                     "JOIN users p ON t.payer_id = p.id JOIN users e ON t.payee_id = e.id ORDER BY t.id DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapTransaction(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all transactions", e);
        }
        return list;
    }

    @Override
    public List<Transaction> findByOrderId(Long orderId) {
        String sql = "SELECT t.*, p.full_name as payer_name, e.full_name as payee_name FROM transactions t " +
                     "JOIN users p ON t.payer_id = p.id JOIN users e ON t.payee_id = e.id WHERE t.order_id = ? ORDER BY t.id DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch transactions for order: " + orderId, e);
        }
        return list;
    }

    @Override
    public List<Transaction> findByUserId(Long userId) {
        String sql = "SELECT t.*, p.full_name as payer_name, e.full_name as payee_name FROM transactions t " +
                     "JOIN users p ON t.payer_id = p.id JOIN users e ON t.payee_id = e.id " +
                     "WHERE t.payer_id = ? OR t.payee_id = ? ORDER BY t.id DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch transactions for user: " + userId, e);
        }
        return list;
    }

    @Override
    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            String sql = "INSERT INTO transactions (order_id, payer_id, payee_id, amount, payment_method, transaction_reference, status) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, transaction.getOrderId());
                stmt.setLong(2, transaction.getPayerId());
                stmt.setLong(3, transaction.getPayeeId());
                stmt.setBigDecimal(4, transaction.getAmount());
                stmt.setString(5, transaction.getPaymentMethod());
                stmt.setString(6, transaction.getTransactionReference());
                stmt.setString(7, transaction.getStatus());

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) transaction.setId(rs.getLong(1));
                }
                transaction.setCreatedAt(LocalDateTime.now());
                return transaction;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert transaction: " + transaction.getTransactionReference(), e);
            }
        } else {
            String sql = "UPDATE transactions SET status = ? WHERE id = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, transaction.getStatus());
                stmt.setLong(2, transaction.getId());
                stmt.executeUpdate();
                return transaction;
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update transaction: " + transaction.getId(), e);
            }
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete transaction by ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM transactions";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count transactions", e);
        }
        return 0;
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setOrderId(rs.getLong("order_id"));
        t.setPayerId(rs.getLong("payer_id"));
        t.setPayeeId(rs.getLong("payee_id"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setPaymentMethod(rs.getString("payment_method"));
        t.setTransactionReference(rs.getString("transaction_reference"));
        t.setStatus(rs.getString("status"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toLocalDateTime());

        try {
            t.setPayerName(rs.getString("payer_name"));
            t.setPayeeName(rs.getString("payee_name"));
        } catch (Exception ignored) {}

        return t;
    }
}
