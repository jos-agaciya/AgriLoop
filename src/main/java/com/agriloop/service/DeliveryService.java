package com.agriloop.service;

import com.agriloop.exception.AgriLoopException;
import com.agriloop.model.Delivery;
import com.agriloop.model.Order;
import com.agriloop.model.OrderItem;
import com.agriloop.model.Transaction;
import com.agriloop.model.enums.DeliveryStatus;
import com.agriloop.model.enums.OrderStatus;
import com.agriloop.model.enums.PaymentStatus;
import com.agriloop.repository.DeliveryRepository;
import com.agriloop.repository.OrderRepository;
import com.agriloop.repository.TransactionRepository;
import com.agriloop.repository.impl.JdbcDeliveryRepository;
import com.agriloop.repository.impl.JdbcOrderRepository;
import com.agriloop.repository.impl.JdbcTransactionRepository;
import com.agriloop.service.intelligence.SustainabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing logistics assignments, tracking milestones, and status progressions.
 */
public class DeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);
    private static DeliveryService instance;

    private final DeliveryRepository deliveryRepo = new JdbcDeliveryRepository();
    private final OrderRepository orderRepo = new JdbcOrderRepository();
    private final TransactionRepository transactionRepo = new JdbcTransactionRepository();

    private DeliveryService() {}

    public static synchronized DeliveryService getInstance() {
        if (instance == null) {
            instance = new DeliveryService();
        }
        return instance;
    }

    /**
     * Transporter accepts an available delivery request.
     */
    public Delivery acceptDeliveryRequest(Long transporterId, Long deliveryId) {
        Delivery delivery = deliveryRepo.findById(deliveryId)
            .orElseThrow(() -> new AgriLoopException("Delivery request not found"));

        if (delivery.getStatus() != DeliveryStatus.ASSIGNMENT_PENDING) {
            throw new AgriLoopException("Delivery is no longer available for assignment (Status: " + delivery.getStatus() + ")");
        }

        delivery.setTransporterId(transporterId);
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        Delivery saved = deliveryRepo.save(delivery);

        // Update Order to PROCESSING
        Optional<Order> orderOpt = orderRepo.findById(delivery.getOrderId());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(OrderStatus.PROCESSING);
            orderRepo.save(order);

            // Notify Farmer & Buyer
            NotificationService.getInstance().pushNotification(
                order.getBuyerId(),
                "DELIVERY_UPDATE",
                "Transporter Assigned",
                "Transporter assigned for Order #" + order.getOrderNumber() + " (Tracking: " + delivery.getTrackingCode() + ").",
                order.getId()
            );
            NotificationService.getInstance().pushNotification(
                order.getSellerId(),
                "DELIVERY_UPDATE",
                "Transporter Assigned",
                "Transporter accepted dispatch for Order #" + order.getOrderNumber() + " and will arrive for pickup.",
                order.getId()
            );
        }

        logger.info("Transporter {} accepted delivery assignment #{}", transporterId, delivery.getTrackingCode());
        NotificationService.getInstance().pushNotification(
            transporterId,
            "DELIVERY_UPDATE",
            "Delivery Assignment Accepted",
            "You have accepted shipment " + delivery.getTrackingCode() + ". Proceed to pickup location at " + delivery.getPickupLocation() + ".",
            delivery.getOrderId()
        );

        return saved;
    }

    /**
     * Advances delivery status milestone: PICKED_UP -> IN_TRANSIT -> DELIVERED.
     */
    public Delivery advanceStatus(Long transporterId, Long deliveryId, DeliveryStatus targetStatus) {
        Delivery delivery = deliveryRepo.findById(deliveryId)
            .orElseThrow(() -> new AgriLoopException("Delivery not found"));

        if (!transporterId.equals(delivery.getTransporterId())) {
            throw new AgriLoopException("Unauthorized: This delivery is not assigned to your account");
        }

        Optional<Order> orderOpt = orderRepo.findById(delivery.getOrderId());

        switch (targetStatus) {
            case PICKED_UP -> {
                delivery.setStatus(DeliveryStatus.PICKED_UP);
                delivery.setPickupTime(LocalDateTime.now());
                updateOrderStatus(delivery.getOrderId(), OrderStatus.IN_TRANSIT);

                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    NotificationService.getInstance().pushNotification(
                        order.getBuyerId(),
                        "DELIVERY_UPDATE",
                        "Delivery picked up",
                        "Shipment for Order #" + order.getOrderNumber() + " has been picked up by transporter and is en route.",
                        order.getId()
                    );
                    NotificationService.getInstance().pushNotification(
                        order.getSellerId(),
                        "DELIVERY_UPDATE",
                        "Delivery picked up",
                        "Cargo for Order #" + order.getOrderNumber() + " was collected from your location.",
                        order.getId()
                    );
                }
            }
            case IN_TRANSIT -> {
                delivery.setStatus(DeliveryStatus.IN_TRANSIT);
                updateOrderStatus(delivery.getOrderId(), OrderStatus.IN_TRANSIT);

                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    NotificationService.getInstance().pushNotification(
                        order.getBuyerId(),
                        "DELIVERY_UPDATE",
                        "Delivery in transit",
                        "Shipment for Order #" + order.getOrderNumber() + " is currently in transit to " + delivery.getDeliveryLocation() + ".",
                        order.getId()
                    );
                }
            }
            case DELIVERED -> {
                delivery.setStatus(DeliveryStatus.DELIVERED);
                delivery.setDeliveryTime(LocalDateTime.now());
                onDeliveryCompleted(delivery);
            }
            default -> throw new AgriLoopException("Invalid status transition: " + targetStatus);
        }

        Delivery updated = deliveryRepo.save(delivery);
        logger.info("Delivery #{} advanced to status {}", delivery.getTrackingCode(), targetStatus);
        return updated;
    }

    private void onDeliveryCompleted(Delivery delivery) {
        // 1. Complete Order & Release Escrow
        Optional<Order> orderOpt = orderRepo.findById(delivery.getOrderId());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(OrderStatus.COMPLETED);
            order.setPaymentStatus(PaymentStatus.RELEASED_TO_SELLER);
            orderRepo.save(order);

            // 2. Settle Financial Transaction
            try {
                Transaction tx = new Transaction();
                tx.setOrderId(order.getId());
                tx.setPayerId(order.getBuyerId());
                tx.setPayeeId(order.getSellerId());
                tx.setAmount(order.getTotalAmount());
                tx.setPaymentMethod("ESCROW_BANK_TRANSFER");
                tx.setTransactionReference("PAY-OUT-" + System.currentTimeMillis() + "-" + order.getId());
                tx.setStatus("SUCCESS");
                transactionRepo.save(tx);
            } catch (Exception e) {
                logger.warn("Could not record payout transaction for order {}", order.getId(), e);
            }

            // 3. Record Sustainability Carbon Offset
            BigDecimal qtyTons = BigDecimal.ONE;
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                qtyTons = order.getItems().get(0).getQuantityTons();
            }
            SustainabilityService.getInstance().recordCompletedTransactionImpact(
                order.getSellerId(), order.getBuyerId(), qtyTons
            );

            // 4. Send targeted notifications to all 3 stakeholders
            NotificationService.getInstance().pushNotification(
                order.getBuyerId(),
                "ORDER_COMPLETED",
                "Delivery completed",
                "Cargo for Order #" + order.getOrderNumber() + " has been successfully delivered to your facility.",
                order.getId()
            );

            NotificationService.getInstance().pushNotification(
                order.getSellerId(),
                "ORDER_COMPLETED",
                "Delivery completed & Payment Released",
                "Cargo for Order #" + order.getOrderNumber() + " was delivered. Escrow payout of ₹" + order.getTotalAmount() + " released to your account.",
                order.getId()
            );

            if (delivery.getTransporterId() != null) {
                NotificationService.getInstance().pushNotification(
                    delivery.getTransporterId(),
                    "DELIVERY_COMPLETED",
                    "Delivery completed & Payout Credited",
                    "Shipment #" + delivery.getTrackingCode() + " marked delivered. Freight earnings of ₹" + delivery.getDeliveryCost() + " credited.",
                    order.getId()
                );
            }
        }
    }

    private void updateOrderStatus(Long orderId, OrderStatus status) {
        if (orderId == null) return;
        Optional<Order> orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            orderRepo.save(order);
        }
    }

    public List<Delivery> getAvailableRequests() {
        return deliveryRepo.findByStatus(DeliveryStatus.ASSIGNMENT_PENDING);
    }

    public List<Delivery> getActiveDeliveries(Long transporterId) {
        if (transporterId == null) return List.of();
        List<Delivery> all = deliveryRepo.findByTransporterId(transporterId);
        return all.stream()
            .filter(d -> d.getStatus() == DeliveryStatus.ACCEPTED || 
                         d.getStatus() == DeliveryStatus.PICKED_UP || 
                         d.getStatus() == DeliveryStatus.IN_TRANSIT)
            .toList();
    }

    public List<Delivery> getCompletedDeliveries(Long transporterId) {
        if (transporterId == null) return List.of();
        List<Delivery> all = deliveryRepo.findByTransporterId(transporterId);
        return all.stream()
            .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
            .toList();
    }

    public Optional<Delivery> getDeliveryForOrder(Long orderId) {
        if (orderId == null) return Optional.empty();
        return deliveryRepo.findByOrderId(orderId);
    }
}
