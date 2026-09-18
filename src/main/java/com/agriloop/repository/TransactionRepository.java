package com.agriloop.repository;

import com.agriloop.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Financial Transactions.
 */
public interface TransactionRepository extends BaseRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionReference(String reference);
    List<Transaction> findByOrderId(Long orderId);
    List<Transaction> findByUserId(Long userId);
}
