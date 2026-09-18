package com.agriloop.service;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.AgriLoopException;
import com.agriloop.exception.ValidationException;
import com.agriloop.model.Delivery;
import com.agriloop.model.Order;
import com.agriloop.model.OrderItem;
import com.agriloop.model.Transaction;
import com.agriloop.model.TransporterProfile;
import com.agriloop.model.User;
import com.agriloop.model.WasteListing;
import com.agriloop.model.enums.DeliveryStatus;
import com.agriloop.model.enums.ListingStatus;
import com.agriloop.model.enums.OrderStatus;
import com.agriloop.model.enums.PaymentStatus;
import com.agriloop.model.enums.UserRole;
import com.agriloop.repository.DeliveryRepository;
import com.agriloop.repository.OrderRepository;
import com.agriloop.repository.TransactionRepository;
import com.agriloop.repository.UserRepository;
import com.agriloop.repository.WasteListingRepository;
import com.agriloop.repository.impl.JdbcDeliveryRepository;
import com.agriloop.repository.impl.JdbcOrderRepository;
import com.agriloop.repository.impl.JdbcTransactionRepository;
import com.agriloop.repository.impl.JdbcUserRepository;
import com.agriloop.repository.impl.JdbcWasteListingRepository;
import com.agriloop.service.intelligence.TransportMatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing the complete order lifecycle, purchase requests, and transactional boundaries.
 */
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static OrderService instance;

    private final OrderRepository orderRepo = new JdbcOrderRepository();
    private final WasteListingRepository listingRepo = new JdbcWasteListingRepository();
    private final DeliveryRepository deliveryRepo = new JdbcDeliveryRepository();
    private final TransactionRepository transactionRepo = new JdbcTransactionRepository();
    private final UserRepository userRepo = new JdbcUserRepository();

    private OrderService() {}

    public static synchronized OrderService getInstance() {
        if (instance == null) {
            instance = new OrderService();
        }
        return instance;
    }

    /**
     * Creates a real purchase request from a manufacturer to a farmer.
     */
    public Order placePurchaseRequest(Long buyerId, Long listingId, BigDecimal requestedQuantity, String deliveryAddress, String notes) {
        if (buyerId == null) throw new ValidationException("Buyer ID is required");
        if (listingId == null) throw new ValidationException("Listing ID is required");
        if (requestedQuantity == null || requestedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Requested quantity must be greater than zero");
        }
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            throw new ValidationException("Delivery destination address is required");
        }
        if (!com.agriloop.util.ValidationUtil.isValidFullAddress(deliveryAddress)) {
            throw new ValidationException("Please provide a full delivery address (e.g., Facility/Street, Area, City/District, State/PIN)");
        }

        WasteListing listing = listingRepo.findById(listingId)
            .orElseThrow(() -> new AgriLoopException("Listing not found"));

        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new AgriLoopException("Listing is no longer available for purchase (Status: " + listing.getStatus() + ")");
        }

        if (requestedQuantity.compareTo(listing.getQuantityTons()) > 0) {
            throw new ValidationException("Requested quantity (" + requestedQuantity + " Tons) exceeds available supply (" + listing.getQuantityTons() + " Tons)");
        }

        if (buyerId.equals(listing.getFarmerId())) {
            throw new ValidationException("Farmers cannot purchase their own waste listings");
        }

        BigDecimal subtotal = requestedQuantity.multiply(listing.getPricePerTon()).setScale(2, RoundingMode.HALF_UP);
        String orderNumber = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + (int)(Math.random() * 900 + 100);

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setBuyerId(buyerId);
        order.setSellerId(listing.getFarmerId());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(subtotal);
        order.setPaymentStatus(PaymentStatus.ESCROW_HELD);
        order.setDeliveryAddress(deliveryAddress.trim());
        order.setNotes(notes != null ? notes.trim() : "Standard procurement request");

        OrderItem item = new OrderItem();
        item.setListingId(listing.getId());
        item.setQuantityTons(requestedQuantity);
        item.setUnitPrice(listing.getPricePerTon());
        item.setSubtotal(subtotal);
        item.setListingTitle(listing.getTitle());
        order.setItems(List.of(item));

        Order saved = orderRepo.save(order);
        logger.info("Created purchase request #{} for buyer {} from farmer {}", orderNumber, buyerId, listing.getFarmerId());

        // 1. Notify Farmer (Seller)
        Optional<User> buyerUser = userRepo.findById(buyerId);
        String buyerName = buyerUser.map(User::getFullName).orElse("A Manufacturer");
        NotificationService.getInstance().pushNotification(
            listing.getFarmerId(),
            "ORDER_REQUEST",
            "New purchase request received",
            "Buyer " + buyerName + " requested " + requestedQuantity + " Tons of " + listing.getWasteType() + " at ₹" + subtotal + " (Order #" + orderNumber + ").",
            saved.getId()
        );

        // 2. Notify Buyer
        NotificationService.getInstance().pushNotification(
            buyerId,
            "ORDER_SUBMITTED",
            "Purchase Request Sent",
            "Your purchase request for " + requestedQuantity + " Tons of '" + listing.getTitle() + "' was submitted to the seller.",
            saved.getId()
        );

        return saved;
    }

    /**
     * Farmer accepts an incoming purchase request.
     * Transitions order to CONFIRMED, updates listing stock, creates a delivery dispatch job, and records transaction.
     */
    public Order acceptPurchaseRequest(Long farmerId, Long orderId) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new AgriLoopException("Order not found"));

        if (!order.getSellerId().equals(farmerId)) {
            throw new AgriLoopException("Unauthorized: This purchase request does not belong to your account");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AgriLoopException("Order cannot be accepted in its current state: " + order.getStatus());
        }

        // 1. Update Order Status
        order.setStatus(OrderStatus.CONFIRMED);
        order = orderRepo.save(order);

        // 2. Adjust Listing Quantity
        WasteListing listing = null;
        OrderItem item = null;
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            item = order.getItems().get(0);
            Optional<WasteListing> listingOpt = listingRepo.findById(item.getListingId());
            if (listingOpt.isPresent()) {
                listing = listingOpt.get();
                BigDecimal remaining = listing.getQuantityTons().subtract(item.getQuantityTons());
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    listing.setQuantityTons(BigDecimal.ZERO);
                    listing.setStatus(ListingStatus.SOLD_OUT);
                } else {
                    listing.setQuantityTons(remaining);
                }
                listingRepo.save(listing);

                // 3. Create Delivery Dispatch record for Transporters
                createDeliveryDispatch(order, listing, item.getQuantityTons());
            }
        }

        // 4. Record Escrow Transaction
        Transaction tx = new Transaction();
        tx.setOrderId(order.getId());
        tx.setPayerId(order.getBuyerId());
        tx.setPayeeId(order.getSellerId());
        tx.setAmount(order.getTotalAmount());
        tx.setPaymentMethod("ESCROW_BANK_TRANSFER");
        tx.setTransactionReference("TXN-" + System.currentTimeMillis() + "-" + order.getId());
        tx.setStatus("PENDING");
        try {
            transactionRepo.save(tx);
        } catch (Exception e) {
            logger.warn("Could not record initial transaction for order {}", order.getId(), e);
        }

        // 5. Notify Buyer of Acceptance
        String materialName = (listing != null) ? listing.getWasteType() : (item != null ? item.getListingTitle() : "materials");
        NotificationService.getInstance().pushNotification(
            order.getBuyerId(),
            "ORDER_ACCEPTED",
            "Purchase request accepted",
            "The seller has accepted your purchase request for Order #" + order.getOrderNumber() + " (" + materialName + "). Transport coordination has begun.",
            order.getId()
        );

        logger.info("Farmer {} accepted purchase request #{}", farmerId, order.getOrderNumber());
        return order;
    }

    /**
     * Farmer rejects an incoming purchase request.
     */
    public Order rejectPurchaseRequest(Long farmerId, Long orderId, String reason) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new AgriLoopException("Order not found"));

        if (!order.getSellerId().equals(farmerId)) {
            throw new AgriLoopException("Unauthorized: This purchase request does not belong to your account");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AgriLoopException("Order cannot be rejected in its current state: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        String reasonStr = (reason != null && !reason.isBlank()) ? reason : "Inventory or logistics unavailable";
        order.setNotes("Rejected by Seller: " + reasonStr);
        order = orderRepo.save(order);

        // Notify Buyer of Rejection
        NotificationService.getInstance().pushNotification(
            order.getBuyerId(),
            "ORDER_REJECTED",
            "Purchase request rejected",
            "Your purchase request for Order #" + order.getOrderNumber() + " was declined by the seller (" + reasonStr + ").",
            order.getId()
        );

        logger.info("Farmer {} rejected purchase request #{}", farmerId, order.getOrderNumber());
        return order;
    }

    /**
     * Buyer cancels an unconfirmed order.
     */
    public Order cancelOrder(Long buyerId, Long orderId) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new AgriLoopException("Order not found"));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new AgriLoopException("Unauthorized: This order does not belong to your account");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AgriLoopException("Confirmed or in-transit orders cannot be cancelled directly");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setNotes("Cancelled by Buyer");
        order = orderRepo.save(order);

        logger.info("Buyer {} cancelled order #{}", buyerId, order.getOrderNumber());
        return order;
    }

    private void createDeliveryDispatch(Order order, WasteListing listing, BigDecimal cargoTons) {
        Optional<Delivery> existing = deliveryRepo.findByOrderId(order.getId());
        if (existing.isPresent()) return;

        Delivery delivery = new Delivery();
        delivery.setOrderId(order.getId());
        delivery.setPickupLocation(listing.getLocation());
        delivery.setDeliveryLocation(order.getDeliveryAddress());

        // Deterministic geographic road-distance & cost calculation
        com.agriloop.util.GeoLocationUtil.RouteCalculation route = 
            com.agriloop.util.GeoLocationUtil.calculateRoute(listing.getLocation(), order.getDeliveryAddress(), cargoTons);
        
        BigDecimal distance = route.distanceKm() != null ? route.distanceKm() : BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal cost = route.deliveryCost() != null ? route.deliveryCost() : BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);

        delivery.setDistanceKm(distance);
        delivery.setDeliveryCost(cost);
        delivery.setStatus(DeliveryStatus.ASSIGNMENT_PENDING);
        delivery.setTrackingCode("TRK-" + (System.currentTimeMillis() % 1000000));

        Delivery savedDelivery = deliveryRepo.save(delivery);
        logger.info("Created delivery dispatch {} for order #{} (Distance: {} km, Cost: ₹{})", 
            savedDelivery.getTrackingCode(), order.getOrderNumber(), distance, cost);

        // Match and notify eligible transporters
        try {
            List<User> transporterUsers = userRepo.findByRole(UserRole.TRANSPORTER);
            List<TransporterProfile> profiles = new ArrayList<>();
            for (User u : transporterUsers) {
                userRepo.findTransporterProfile(u.getId()).ifPresent(profiles::add);
            }

            var matches = TransportMatchingService.getInstance().findSuitableTransporters(savedDelivery, cargoTons, profiles);
            for (var match : matches) {
                Long tpUserId = match.transporter().getUserId();
                NotificationService.getInstance().pushNotification(
                    tpUserId,
                    "DELIVERY_DISPATCH",
                    "New delivery request available",
                    "Cargo dispatch available: " + cargoTons + " Tons of " + listing.getWasteType() + " from " + listing.getLocation() + " to " + order.getDeliveryAddress() + " (Est. Payout: ₹" + cost + ").",
                    order.getId()
                );
            }
        } catch (Exception e) {
            logger.warn("Could not match or notify transporters: {}", e.getMessage());
        }
    }

    public List<Order> getOrdersForBuyer(Long buyerId) {
        if (buyerId == null) return List.of();
        return orderRepo.findByBuyerId(buyerId);
    }

    public List<Order> getSalesRequestsForFarmer(Long farmerId) {
        if (farmerId == null) return List.of();
        return orderRepo.findBySellerId(farmerId);
    }
}
