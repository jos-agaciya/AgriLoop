package com.agriloop.service;

import com.agriloop.database.DatabaseManager;
import com.agriloop.model.Delivery;
import com.agriloop.model.Order;
import com.agriloop.model.WasteListing;
import com.agriloop.model.dashboard.FarmerDashboardData;
import com.agriloop.model.dashboard.ManufacturerDashboardData;
import com.agriloop.model.dashboard.TransporterDashboardData;
import com.agriloop.model.enums.DeliveryStatus;
import com.agriloop.model.enums.ListingStatus;
import com.agriloop.repository.DeliveryRepository;
import com.agriloop.repository.OrderRepository;
import com.agriloop.repository.SustainabilityRepository;
import com.agriloop.repository.WasteListingRepository;
import com.agriloop.repository.impl.JdbcDeliveryRepository;
import com.agriloop.repository.impl.JdbcOrderRepository;
import com.agriloop.repository.impl.JdbcSustainabilityRepository;
import com.agriloop.repository.impl.JdbcWasteListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading role-specific real metrics and data models directly from the database.
 */
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static DashboardService instance;

    private final WasteListingRepository listingRepo = new JdbcWasteListingRepository();
    private final OrderRepository orderRepo = new JdbcOrderRepository();
    private final DeliveryRepository deliveryRepo = new JdbcDeliveryRepository();
    private final SustainabilityRepository sustainabilityRepo = new JdbcSustainabilityRepository();

    private DashboardService() {}

    public static synchronized DashboardService getInstance() {
        if (instance == null) {
            instance = new DashboardService();
        }
        return instance;
    }

    /**
     * Loads live database metrics and activity for a Farmer / Seller.
     */
    public FarmerDashboardData getFarmerDashboardData(Long userId) {
        FarmerDashboardData data = new FarmerDashboardData();
        if (userId == null) return data;

        // 1. Active Listings Count
        data.setActiveListingsCount(queryCount(
            "SELECT COUNT(*) FROM waste_listings WHERE farmer_id = ? AND status = 'AVAILABLE'", userId));

        // 2. Waste Available (Tons)
        data.setWasteAvailableTons(querySum(
            "SELECT COALESCE(SUM(quantity_tons), 0) FROM waste_listings WHERE farmer_id = ? AND status = 'AVAILABLE'", userId));

        // 3. Pending Purchase Requests Count
        data.setPendingRequestsCount(queryCount(
            "SELECT COUNT(*) FROM orders WHERE seller_id = ? AND status = 'PENDING'", userId));

        // 4. Completed Sales Count
        data.setCompletedSalesCount(queryCount(
            "SELECT COUNT(*) FROM orders WHERE seller_id = ? AND status IN ('DELIVERED', 'COMPLETED')", userId));

        // 5. Total Earnings (Settled completed sales)
        BigDecimal earnings = querySum(
            "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE seller_id = ? AND status IN ('DELIVERED', 'COMPLETED')", userId);
        data.setTotalEarnings(earnings);

        // Waste Sold
        BigDecimal wasteSold = querySum(
            "SELECT COALESCE(SUM(oi.quantity_tons), 0) FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.seller_id = ? AND o.status IN ('DELIVERED', 'COMPLETED')", userId);
        data.setWasteSoldTons(wasteSold);

        // CO2 Impact
        BigDecimal co2Db = sustainabilityRepo.getCo2SavedByUserId(userId);
        if (co2Db.compareTo(BigDecimal.ZERO) == 0 && data.getWasteSoldTons().compareTo(BigDecimal.ZERO) > 0) {
            data.setCo2ImpactKg(data.getWasteSoldTons().multiply(new BigDecimal("1250")));
        } else {
            data.setCo2ImpactKg(co2Db);
        }

        // Recent Listings
        try {
            List<WasteListing> listings = listingRepo.findByFarmerId(userId);
            data.setRecentListings(listings.size() > 5 ? listings.subList(0, 5) : listings);
        } catch (Exception e) {
            logger.warn("Could not fetch recent listings for farmer {}", userId, e);
        }

        // Incoming Requests
        try {
            List<Order> orders = orderRepo.findBySellerId(userId);
            data.setIncomingRequests(orders.size() > 5 ? orders.subList(0, 5) : orders);
        } catch (Exception e) {
            logger.warn("Could not fetch incoming requests for farmer {}", userId, e);
        }

        return data;
    }

    /**
     * Loads live database metrics and activity for a Manufacturer / Buyer.
     */
    public ManufacturerDashboardData getManufacturerDashboardData(Long userId) {
        ManufacturerDashboardData data = new ManufacturerDashboardData();
        if (userId == null) return data;

        // 1. Materials Purchased Tons
        BigDecimal purchasedTons = querySum(
            "SELECT COALESCE(SUM(oi.quantity_tons), 0) FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.buyer_id = ? AND o.status IN ('DELIVERED', 'COMPLETED')", userId);
        data.setMaterialsPurchasedTons(purchasedTons);

        // 2. Active Orders Count
        data.setActiveOrdersCount(queryCount(
            "SELECT COUNT(*) FROM orders WHERE buyer_id = ? AND status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'IN_TRANSIT')", userId));

        // 3. Pending Deliveries Count
        data.setPendingDeliveriesCount(queryCount(
            "SELECT COUNT(*) FROM deliveries d JOIN orders o ON d.order_id = o.id WHERE o.buyer_id = ? AND d.status IN ('ASSIGNMENT_PENDING', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT')", userId));

        // 4. Total Spent
        BigDecimal totalSpent = querySum(
            "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE buyer_id = ? AND status IN ('DELIVERED', 'COMPLETED')", userId);
        data.setTotalSpent(totalSpent);

        // 5. CO2 Impact
        BigDecimal co2Db = sustainabilityRepo.getCo2SavedByUserId(userId);
        if (co2Db.compareTo(BigDecimal.ZERO) == 0 && data.getMaterialsPurchasedTons().compareTo(BigDecimal.ZERO) > 0) {
            data.setCo2ImpactKg(data.getMaterialsPurchasedTons().multiply(new BigDecimal("1250")));
        } else {
            data.setCo2ImpactKg(co2Db);
        }

        // Recent Orders
        try {
            List<Order> orders = orderRepo.findByBuyerId(userId);
            data.setRecentOrders(orders.size() > 5 ? orders.subList(0, 5) : orders);
        } catch (Exception e) {
            logger.warn("Could not fetch recent orders for manufacturer {}", userId, e);
        }

        // Recommended / Available materials
        try {
            List<WasteListing> available = listingRepo.findByStatus(ListingStatus.AVAILABLE);
            data.setRecommendedMaterials(available.size() > 5 ? available.subList(0, 5) : available);
        } catch (Exception e) {
            logger.warn("Could not fetch available listings for manufacturer", e);
        }

        return data;
    }

    /**
     * Loads live database metrics and activity for a Transporter.
     */
    public TransporterDashboardData getTransporterDashboardData(Long userId) {
        TransporterDashboardData data = new TransporterDashboardData();
        if (userId == null) return data;

        // 1. Pending Requests Count (Available dispatch opportunities)
        data.setPendingRequestsCount(queryCount(
            "SELECT COUNT(*) FROM deliveries WHERE status = 'ASSIGNMENT_PENDING'"));

        // 2. Active Deliveries Count (Assigned to this transporter and in progress)
        data.setActiveDeliveriesCount(queryCount(
            "SELECT COUNT(*) FROM deliveries WHERE transporter_id = ? AND status IN ('ACCEPTED', 'PICKED_UP', 'IN_TRANSIT')", userId));

        // 3. Completed Deliveries Count
        data.setCompletedDeliveriesCount(queryCount(
            "SELECT COUNT(*) FROM deliveries WHERE transporter_id = ? AND status = 'DELIVERED'", userId));

        // 4. Total Earnings
        BigDecimal earnings = querySum(
            "SELECT COALESCE(SUM(delivery_cost), 0) FROM deliveries WHERE transporter_id = ? AND status = 'DELIVERED'", userId);
        data.setTotalEarnings(earnings);

        // 5. Distance Covered Km
        BigDecimal distance = querySum(
            "SELECT COALESCE(SUM(distance_km), 0) FROM deliveries WHERE transporter_id = ? AND status = 'DELIVERED'", userId);
        data.setDistanceCoveredKm(distance);

        // Recent Deliveries
        try {
            List<Delivery> deliveries = deliveryRepo.findByTransporterId(userId);
            if (deliveries.isEmpty()) {
                // If transporter has no assigned deliveries, show pending requests
                deliveries = deliveryRepo.findByStatus(DeliveryStatus.ASSIGNMENT_PENDING);
            }
            data.setRecentDeliveries(deliveries.size() > 5 ? deliveries.subList(0, 5) : deliveries);
        } catch (Exception e) {
            logger.warn("Could not fetch deliveries for transporter {}", userId, e);
        }

        return data;
    }

    private long queryCount(String sql, Object... params) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.warn("Query count failed: {}", e.getMessage());
        }
        return 0;
    }

    private BigDecimal querySum(String sql, Object... params) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal result = rs.getBigDecimal(1);
                    return result != null ? result : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            logger.warn("Query sum failed: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}
