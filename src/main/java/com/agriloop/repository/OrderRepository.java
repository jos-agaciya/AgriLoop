package com.agriloop.repository;

import com.agriloop.model.Order;
import com.agriloop.model.enums.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Orders and Order Items.
 */
public interface OrderRepository extends BaseRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByBuyerId(Long buyerId);
    List<Order> findBySellerId(Long sellerId);
    List<Order> findByStatus(OrderStatus status);
}
