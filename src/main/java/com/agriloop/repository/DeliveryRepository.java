package com.agriloop.repository;

import com.agriloop.model.Delivery;
import com.agriloop.model.enums.DeliveryStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Logistics and Delivery records.
 */
public interface DeliveryRepository extends BaseRepository<Delivery, Long> {
    Optional<Delivery> findByOrderId(Long orderId);
    Optional<Delivery> findByTrackingCode(String trackingCode);
    List<Delivery> findByTransporterId(Long transporterId);
    List<Delivery> findByStatus(DeliveryStatus status);
}
