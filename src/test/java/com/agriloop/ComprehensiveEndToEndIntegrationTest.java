package com.agriloop;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.AgriLoopException;
import com.agriloop.exception.ValidationException;
import com.agriloop.model.Delivery;
import com.agriloop.model.FarmerProfile;
import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.Order;
import com.agriloop.model.SustainabilityRecord;
import com.agriloop.model.TransporterProfile;
import com.agriloop.model.User;
import com.agriloop.model.WasteListing;
import com.agriloop.model.dashboard.FarmerDashboardData;
import com.agriloop.model.dashboard.ManufacturerDashboardData;
import com.agriloop.model.dashboard.TransporterDashboardData;
import com.agriloop.model.enums.DeliveryStatus;
import com.agriloop.model.enums.ListingStatus;
import com.agriloop.model.enums.OrderStatus;
import com.agriloop.model.enums.PaymentStatus;
import com.agriloop.model.enums.UserRole;
import com.agriloop.model.enums.WasteCategory;
import com.agriloop.repository.DeliveryRepository;
import com.agriloop.repository.OrderRepository;
import com.agriloop.repository.SustainabilityRepository;
import com.agriloop.repository.UserRepository;
import com.agriloop.repository.WasteListingRepository;
import com.agriloop.repository.impl.JdbcDeliveryRepository;
import com.agriloop.repository.impl.JdbcOrderRepository;
import com.agriloop.repository.impl.JdbcSustainabilityRepository;
import com.agriloop.repository.impl.JdbcUserRepository;
import com.agriloop.repository.impl.JdbcWasteListingRepository;
import com.agriloop.service.AuthService;
import com.agriloop.service.DashboardService;
import com.agriloop.service.DeliveryService;
import com.agriloop.service.ListingService;
import com.agriloop.service.OrderService;
import com.agriloop.service.intelligence.BuyerRecommendationService;
import com.agriloop.service.intelligence.SmartWasteMatchingService;
import com.agriloop.service.intelligence.SustainabilityService;
import com.agriloop.service.intelligence.TransportMatchingService;
import com.agriloop.service.intelligence.ValueEstimationService;
import com.agriloop.util.SecurityUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full End-to-End Integration Test Suite for AgriLoop Marketplace.
 * Validates complete multi-stakeholder workflows, intelligent engines,
 * order state machine, transport coordination, and sustainability ledger.
 * Automatically cleans up all test-generated records upon completion.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ComprehensiveEndToEndIntegrationTest {

    private static final UserRepository userRepo = new JdbcUserRepository();
    private static final WasteListingRepository listingRepo = new JdbcWasteListingRepository();
    private static final OrderRepository orderRepo = new JdbcOrderRepository();
    private static final DeliveryRepository deliveryRepo = new JdbcDeliveryRepository();
    private static final SustainabilityRepository sustainabilityRepo = new JdbcSustainabilityRepository();
    private static final com.agriloop.repository.NotificationRepository notifRepo = new com.agriloop.repository.impl.JdbcNotificationRepository();

    private static final AuthService authService = AuthService.getInstance();
    private static final ListingService listingService = ListingService.getInstance();
    private static final OrderService orderService = OrderService.getInstance();
    private static final DeliveryService deliveryService = DeliveryService.getInstance();
    private static final DashboardService dashboardService = DashboardService.getInstance();

    private static final SmartWasteMatchingService wasteMatchingService = SmartWasteMatchingService.getInstance();
    private static final ValueEstimationService valueEstimationService = ValueEstimationService.getInstance();
    private static final BuyerRecommendationService buyerRecService = BuyerRecommendationService.getInstance();
    private static final TransportMatchingService transportMatchingService = TransportMatchingService.getInstance();
    private static final SustainabilityService sustainabilityService = SustainabilityService.getInstance();

    // Test actor tracking IDs for isolated cleanup
    private static final List<Long> createdUserIds = new ArrayList<>();
    private static final List<Long> createdListingIds = new ArrayList<>();
    private static final List<Long> createdOrderIds = new ArrayList<>();
    private static final List<Long> createdDeliveryIds = new ArrayList<>();
    private static final List<Long> createdSustainabilityIds = new ArrayList<>();

    private static String farmerEmail;
    private static String mfgEmail;
    private static String transEmail;

    private static Long farmerId;
    private static Long mfgId;
    private static Long transId;

    private static Long primaryListingId;
    private static Long secondaryListingId;
    private static Long primaryOrderId;
    private static Long rejectedOrderId;
    private static Long activeDeliveryId;

    @BeforeAll
    static void setUp() {
        assertTrue(DatabaseManager.getInstance().isConnected(), "MySQL Database must be reachable");
        long ts = System.currentTimeMillis();
        farmerEmail = "e2e_farmer_" + ts + "@agriloop-test.com";
        mfgEmail = "e2e_mfg_" + ts + "@agriloop-test.com";
        transEmail = "e2e_trans_" + ts + "@agriloop-test.com";
    }

    @AfterAll
    static void tearDownAll() {
        // Absolute cleanup to ensure normal application database remains pristine
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            // 1. Delete sustainability records for test users
            if (!createdUserIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sustainability_records WHERE user_id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.executeUpdate();
                    }
                }
            }
            if (!createdSustainabilityIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sustainability_records WHERE id = ?")) {
                    for (Long sid : createdSustainabilityIds) {
                        ps.setLong(1, sid);
                        ps.executeUpdate();
                    }
                }
            }

            // 2. Delete deliveries
            if (!createdOrderIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM deliveries WHERE order_id = ?")) {
                    for (Long oid : createdOrderIds) {
                        ps.setLong(1, oid);
                        ps.executeUpdate();
                    }
                }
            }
            if (!createdDeliveryIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM deliveries WHERE id = ?")) {
                    for (Long did : createdDeliveryIds) {
                        ps.setLong(1, did);
                        ps.executeUpdate();
                    }
                }
            }

            // 3. Delete transactions
            if (!createdOrderIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE order_id = ?")) {
                    for (Long oid : createdOrderIds) {
                        ps.setLong(1, oid);
                        ps.executeUpdate();
                    }
                }
            }
            if (!createdUserIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE payer_id = ? OR payee_id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.setLong(2, uid);
                        ps.executeUpdate();
                    }
                }
            }

            // 4. Delete order items
            if (!createdOrderIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) {
                    for (Long oid : createdOrderIds) {
                        ps.setLong(1, oid);
                        ps.executeUpdate();
                    }
                }
            }

            // 5. Delete orders
            if (!createdOrderIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE id = ?")) {
                    for (Long oid : createdOrderIds) {
                        ps.setLong(1, oid);
                        ps.executeUpdate();
                    }
                }
            }
            if (!createdUserIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE buyer_id = ? OR seller_id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.setLong(2, uid);
                        ps.executeUpdate();
                    }
                }
            }

            // 6. Delete listings
            if (!createdListingIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM waste_listings WHERE id = ?")) {
                    for (Long lid : createdListingIds) {
                        ps.setLong(1, lid);
                        ps.executeUpdate();
                    }
                }
            }
            if (!createdUserIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM waste_listings WHERE farmer_id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.executeUpdate();
                    }
                }
            }

            // 7. Delete notifications, profiles and users
            if (!createdOrderIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notifications WHERE related_order_id = ?")) {
                    for (Long oid : createdOrderIds) {
                        ps.setLong(1, oid);
                        ps.executeUpdate();
                    }
                }
            }
            if (!createdUserIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notifications WHERE recipient_user_id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM farmer_profiles WHERE user_id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM manufacturer_profiles WHERE user_id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transporter_profiles WHERE user_id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    for (Long uid : createdUserIds) {
                        ps.setLong(1, uid);
                        ps.executeUpdate();
                    }
                }
            }

            conn.commit();
        } catch (Exception e) {
            System.err.println("Cleanup exception (non-fatal): " + e.getMessage());
        }
    }

    // =========================================================================
    // SECTION 1: AUTHENTICATION & ROLE PROFILES
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(1)
    void testFarmerRegistrationAndAuthentication() {
        // Register Farmer
        User farmer = authService.register(
            "Krishi Vikas Farmer",
            farmerEmail,
            "+91 9876500001",
            "SecureAgri@2026",
            "SecureAgri@2026",
            "Patiala Agricultural Belt, Punjab",
            UserRole.FARMER
        );

        assertNotNull(farmer);
        assertNotNull(farmer.getId());
        farmerId = farmer.getId();
        createdUserIds.add(farmerId);

        // Verify password hashing (BCrypt)
        assertTrue(SecurityUtil.verifyPassword("SecureAgri@2026", farmer.getPasswordHash()));
        assertFalse(SecurityUtil.verifyPassword("WrongPassword", farmer.getPasswordHash()));

        // Authenticate against database
        User authenticated = authService.login(farmerEmail, "SecureAgri@2026");
        assertNotNull(authenticated);
        assertEquals(UserRole.FARMER, authenticated.getRole());

        // Verify Farmer Profile was created
        Optional<FarmerProfile> fp = userRepo.findFarmerProfile(farmerId);
        assertTrue(fp.isPresent());
        assertEquals("Patiala Agricultural Belt, Punjab", fp.get().getFarmLocation());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void testManufacturerRegistrationAndAuthentication() {
        // Register Manufacturer
        User mfg = authService.register(
            "EcoMatrix Bio-Packaging Ltd",
            mfgEmail,
            "+91 9876500002",
            "SecureAgri@2026",
            "SecureAgri@2026",
            "Industrial Estate Phase 2, Panipat, Haryana",
            UserRole.MANUFACTURER
        );

        assertNotNull(mfg);
        assertNotNull(mfg.getId());
        mfgId = mfg.getId();
        createdUserIds.add(mfgId);

        User authenticated = authService.login(mfgEmail, "SecureAgri@2026");
        assertNotNull(authenticated);
        assertEquals(UserRole.MANUFACTURER, authenticated.getRole());

        Optional<ManufacturerProfile> mp = userRepo.findManufacturerProfile(mfgId);
        assertTrue(mp.isPresent());
        assertEquals("Industrial Estate Phase 2, Panipat, Haryana", mp.get().getFacilityAddress());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void testTransporterRegistrationAndAuthentication() {
        // Register Transporter
        User trans = authService.register(
            "NorthGate Green Transport",
            transEmail,
            "+91 9876500003",
            "SecureAgri@2026",
            "SecureAgri@2026",
            "Regional Hub",
            UserRole.TRANSPORTER
        );

        assertNotNull(trans);
        assertNotNull(trans.getId());
        transId = trans.getId();
        createdUserIds.add(transId);

        User authenticated = authService.login(transEmail, "SecureAgri@2026");
        assertNotNull(authenticated);
        assertEquals(UserRole.TRANSPORTER, authenticated.getRole());

        TransporterProfile tp = new TransporterProfile();
        tp.setUserId(transId);
        tp.setVehicleType("Medium Commercial Truck");
        tp.setVehicleNumber("PB-11-TG-4422");
        tp.setMaxPayloadTons(new BigDecimal("18.00"));
        tp.setOperatingRadiusKm(new BigDecimal("100.00"));
        tp.setLicenseNumber("DL-TRANS-88992");
        tp.setAvailable(true);
        userRepo.saveTransporterProfile(tp);

        Optional<TransporterProfile> savedTp = userRepo.findTransporterProfile(transId);
        assertTrue(savedTp.isPresent());
        assertEquals("PB-11-TG-4422", savedTp.get().getVehicleNumber());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void testAuthenticationRejections() {
        // Invalid password
        assertThrows(ValidationException.class, () -> authService.login(farmerEmail, "IncorrectPassword"));

        // Non-existent email
        assertThrows(ValidationException.class, () -> authService.login("nonexistent@domain.com", "SecureAgri@2026"));

        // Duplicate registration
        assertThrows(ValidationException.class, () -> authService.register(
            "Duplicate User", farmerEmail, "+91 0000000000", "Pass@123", "Pass@123", "Loc", UserRole.FARMER
        ));
    }

    // =========================================================================
    // SECTION 2: WASTE LISTING & INTELLIGENT RULE-BASED ENGINES
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(5)
    void testFarmerCreatesAgriculturalWasteListing() {
        WasteListing listing = new WasteListing();
        listing.setFarmerId(farmerId);
        listing.setTitle("Dry Rice Straw Bales");
        listing.setWasteType("Rice Straw");
        listing.setCategory(WasteCategory.CROP_RESIDUE);
        listing.setDescription("High density square bales, moisture under 12%, stored under covered shed.");
        listing.setQuantityTons(new BigDecimal("40.00"));
        listing.setPricePerTon(new BigDecimal("1850.00"));
        listing.setMoistureContentPct(new BigDecimal("11.50"));
        listing.setLocation("Patiala, Punjab");
        listing.setAvailableFrom(LocalDate.now());
        listing.setAvailableUntil(LocalDate.now().plusMonths(3));
        listing.setStatus(ListingStatus.AVAILABLE);

        WasteListing saved = listingService.saveListing(listing);
        assertNotNull(saved.getId());
        primaryListingId = saved.getId();
        createdListingIds.add(primaryListingId);

        // Verification from database
        Optional<WasteListing> fetched = listingRepo.findById(primaryListingId);
        assertTrue(fetched.isPresent());
        assertEquals("Dry Rice Straw Bales", fetched.get().getTitle());
        assertEquals(new BigDecimal("40.00"), fetched.get().getQuantityTons());
        assertEquals(new BigDecimal("1850.00"), fetched.get().getPricePerTon());
        assertEquals(ListingStatus.AVAILABLE, fetched.get().getStatus());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void testSmartWasteMatchingEngine() {
        Optional<WasteListing> listingOpt = listingRepo.findById(primaryListingId);
        assertTrue(listingOpt.isPresent());

        List<SmartWasteMatchingService.IndustrialMatch> matches = wasteMatchingService.findMatches(listingOpt.get());
        assertNotNull(matches);
        assertFalse(matches.isEmpty(), "Must identify applicable industrial use cases");
        assertTrue(matches.stream().anyMatch(m -> m.suitabilityScore() > 70));
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    void testRuleBasedValueEstimationEngine() {
        Optional<WasteListing> listingOpt = listingRepo.findById(primaryListingId);
        assertTrue(listingOpt.isPresent());

        ValueEstimationService.ValuationResult estimate = valueEstimationService.estimateValue(listingOpt.get());
        assertNotNull(estimate);
        assertNotNull(estimate.estimatedPricePerTonAvg());
        assertNotNull(estimate.totalEstimatedValueMin());
        assertTrue(estimate.estimatedPricePerTonAvg().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(estimate.demandLevel());
        assertFalse(estimate.pricingFactors().isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    void testBuyerRecommendationEngine() {
        Optional<ManufacturerProfile> mpOpt = userRepo.findManufacturerProfile(mfgId);
        assertTrue(mpOpt.isPresent());

        List<WasteListing> available = listingRepo.findByStatus(ListingStatus.AVAILABLE);
        List<BuyerRecommendationService.RecommendedListing> recommendations = 
            buyerRecService.getRecommendations(available, mpOpt.get());
        
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty(), "Should recommend available active listings");
        assertTrue(recommendations.stream().anyMatch(r -> r.listing().getId().equals(primaryListingId)));
        assertTrue(recommendations.get(0).overallMatchScore() > 0);
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    void testListingUpdateAndRetrieval() {
        Optional<WasteListing> opt = listingRepo.findById(primaryListingId);
        assertTrue(opt.isPresent());
        WasteListing update = opt.get();
        update.setTitle("Dry Rice Straw Bales (Updated)");
        update.setPricePerTon(new BigDecimal("1900.00"));
        update.setMoistureContentPct(new BigDecimal("10.00"));

        listingService.saveListing(update);

        Optional<WasteListing> fetched = listingRepo.findById(primaryListingId);
        assertTrue(fetched.isPresent());
        assertEquals(new BigDecimal("1900.00"), fetched.get().getPricePerTon());
        assertEquals("Dry Rice Straw Bales (Updated)", fetched.get().getTitle());

        List<WasteListing> farmerListings = listingService.getListingsByFarmer(farmerId);
        assertFalse(farmerListings.isEmpty());
        assertTrue(farmerListings.stream().anyMatch(l -> l.getId().equals(primaryListingId)));
    }

    // =========================================================================
    // SECTION 3: ORDER CREATION, PURCHASE REQUEST & ACCEPTANCE
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(10)
    void testManufacturerPlacesOrder() {
        // Manufacturer purchases 15 Tons out of 40 Tons available
        Order order = orderService.placePurchaseRequest(
            mfgId,
            primaryListingId,
            new BigDecimal("15.00"),
            "Industrial Estate Phase 2, Panipat, Haryana",
            "Urgent delivery for boiler feed."
        );

        assertNotNull(order);
        assertNotNull(order.getId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentStatus.ESCROW_HELD, order.getPaymentStatus());
        assertEquals(new BigDecimal("28500.00"), order.getTotalAmount()); // 15 tons * 1900.00

        primaryOrderId = order.getId();
        createdOrderIds.add(primaryOrderId);

        // Verify manufacturer can see it under My Orders
        List<Order> mfgOrders = orderService.getOrdersForBuyer(mfgId);
        assertTrue(mfgOrders.stream().anyMatch(o -> o.getId().equals(primaryOrderId)));

        // Verify farmer can see it under Sales & Requests
        List<Order> farmerRequests = orderService.getSalesRequestsForFarmer(farmerId);
        assertTrue(farmerRequests.stream().anyMatch(o -> o.getId().equals(primaryOrderId)));

        // Verify Farmer received purchase request notification
        var farmerNotifs = notifRepo.findByRecipientUserId(farmerId);
        assertFalse(farmerNotifs.isEmpty());
        assertTrue(farmerNotifs.stream().anyMatch(n -> "ORDER_REQUEST".equals(n.getNotificationType())));
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    void testFarmerAcceptsOrderAndDeliveryDispatched() {
        // Farmer accepts the order
        orderService.acceptPurchaseRequest(farmerId, primaryOrderId);

        // Verify Order status transition: PENDING -> CONFIRMED
        Optional<Order> orderOpt = orderRepo.findById(primaryOrderId);
        assertTrue(orderOpt.isPresent());
        assertEquals(OrderStatus.CONFIRMED, orderOpt.get().getStatus());

        // Verify Listing quantity reduced from 40.00 to 25.00
        Optional<WasteListing> listingOpt = listingRepo.findById(primaryListingId);
        assertTrue(listingOpt.isPresent());
        assertEquals(new BigDecimal("25.00"), listingOpt.get().getQuantityTons());
        assertEquals(ListingStatus.AVAILABLE, listingOpt.get().getStatus());

        // Verify Delivery record automatically created in ASSIGNMENT_PENDING
        Optional<Delivery> deliveryOpt = deliveryRepo.findByOrderId(primaryOrderId);
        assertTrue(deliveryOpt.isPresent(), "Delivery dispatch must be created upon order acceptance");
        Delivery delivery = deliveryOpt.get();
        assertEquals(DeliveryStatus.ASSIGNMENT_PENDING, delivery.getStatus());
        assertNotNull(delivery.getTrackingCode());
        assertTrue(delivery.getTrackingCode().startsWith("TRK-"));

        activeDeliveryId = delivery.getId();
        createdDeliveryIds.add(activeDeliveryId);

        // Verify Buyer received acceptance notification
        var mfgNotifs = notifRepo.findByRecipientUserId(mfgId);
        assertTrue(mfgNotifs.stream().anyMatch(n -> "ORDER_ACCEPTED".equals(n.getNotificationType())));

        // Verify Transporter received delivery dispatch notification
        var transNotifs = notifRepo.findByRecipientUserId(transId);
        assertTrue(transNotifs.stream().anyMatch(n -> "DELIVERY_DISPATCH".equals(n.getNotificationType())));
    }

    // =========================================================================
    // SECTION 4: SMART TRANSPORT MATCHING & DELIVERY LIFECYCLE
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(12)
    void testSmartTransportMatchingEngine() {
        Optional<Delivery> delOpt = deliveryRepo.findById(activeDeliveryId);
        assertTrue(delOpt.isPresent());

        Optional<TransporterProfile> tpOpt = userRepo.findTransporterProfile(transId);
        assertTrue(tpOpt.isPresent());

        List<TransportMatchingService.TransportMatch> matches = 
            transportMatchingService.findSuitableTransporters(delOpt.get(), new BigDecimal("15.00"), List.of(tpOpt.get()));
        
        assertNotNull(matches);
        assertFalse(matches.isEmpty(), "Should match registered transporter");
        assertTrue(matches.get(0).isCapacitySufficient());
        assertTrue(matches.get(0).suitabilityScore() > 50);
    }

    @Test
    @org.junit.jupiter.api.Order(13)
    void testTransporterAcceptsAndExecutesDelivery() {
        // 1. Transporter sees open delivery requests
        List<Delivery> openRequests = deliveryService.getAvailableRequests();
        assertTrue(openRequests.stream().anyMatch(d -> d.getId().equals(activeDeliveryId)));

        // 2. Transporter accepts delivery assignment
        deliveryService.acceptDeliveryRequest(transId, activeDeliveryId);

        Optional<Delivery> del1 = deliveryRepo.findById(activeDeliveryId);
        assertTrue(del1.isPresent());
        assertEquals(DeliveryStatus.ACCEPTED, del1.get().getStatus());
        assertEquals(transId, del1.get().getTransporterId());

        // 3. Mark Picked Up
        deliveryService.advanceStatus(transId, activeDeliveryId, DeliveryStatus.PICKED_UP);
        Optional<Delivery> del2 = deliveryRepo.findById(activeDeliveryId);
        assertTrue(del2.isPresent());
        assertEquals(DeliveryStatus.PICKED_UP, del2.get().getStatus());
        assertNotNull(del2.get().getPickupTime());

        // 4. Mark In Transit
        deliveryService.advanceStatus(transId, activeDeliveryId, DeliveryStatus.IN_TRANSIT);
        Optional<Delivery> del3 = deliveryRepo.findById(activeDeliveryId);
        assertTrue(del3.isPresent());
        assertEquals(DeliveryStatus.IN_TRANSIT, del3.get().getStatus());

        // Verify Order is also IN_TRANSIT
        Optional<Order> ord1 = orderRepo.findById(primaryOrderId);
        assertTrue(ord1.isPresent());
        assertEquals(OrderStatus.IN_TRANSIT, ord1.get().getStatus());

        // 5. Mark Delivered
        deliveryService.advanceStatus(transId, activeDeliveryId, DeliveryStatus.DELIVERED);
        Optional<Delivery> del4 = deliveryRepo.findById(activeDeliveryId);
        assertTrue(del4.isPresent());
        assertEquals(DeliveryStatus.DELIVERED, del4.get().getStatus());
        assertNotNull(del4.get().getDeliveryTime());

        // Verify Order is COMPLETED & Payment released to Seller
        Optional<Order> ord2 = orderRepo.findById(primaryOrderId);
        assertTrue(ord2.isPresent());
        assertEquals(OrderStatus.COMPLETED, ord2.get().getStatus());
        assertEquals(PaymentStatus.RELEASED_TO_SELLER, ord2.get().getPaymentStatus());

        // Verify final delivery completed notifications
        var finalMfgNotifs = notifRepo.findByRecipientUserId(mfgId);
        assertTrue(finalMfgNotifs.stream().anyMatch(n -> "ORDER_COMPLETED".equals(n.getNotificationType())));

        var finalFarmerNotifs = notifRepo.findByRecipientUserId(farmerId);
        assertTrue(finalFarmerNotifs.stream().anyMatch(n -> "ORDER_COMPLETED".equals(n.getNotificationType())));

        var finalTransNotifs = notifRepo.findByRecipientUserId(transId);
        assertTrue(finalTransNotifs.stream().anyMatch(n -> "DELIVERY_COMPLETED".equals(n.getNotificationType())));
    }

    // =========================================================================
    // SECTION 5: SUSTAINABILITY ACCOUNTING & REAL METRICS
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(14)
    void testSustainabilityAccountingVerification() {
        // Check sustainability record created for this delivery (15 tons diverted)
        List<SustainabilityRecord> records = sustainabilityRepo.findByUserId(farmerId);
        assertFalse(records.isEmpty(), "Sustainability record must be created upon completion");
        
        SustainabilityRecord rec = records.get(0);
        createdSustainabilityIds.add(rec.getId());
        assertEquals(new BigDecimal("15.00"), rec.getWasteDivertedTons());
        
        // 15 tons * 1250 kg CO2/ton = 18750 kg CO2 saved
        assertEquals(new BigDecimal("18750.00"), rec.getCo2SavedKg());

        // Test deterministic calculation engine
        SustainabilityService.SustainabilityImpact calc = 
            sustainabilityService.calculateImpact("Rice Straw", new BigDecimal("15.00"));
        assertEquals(new BigDecimal("18750.00"), calc.co2SavedKg());
    }

    @Test
    @org.junit.jupiter.api.Order(15)
    void testRoleDashboardMetricsReflectRealData() {
        // Farmer Dashboard
        FarmerDashboardData farmerData = dashboardService.getFarmerDashboardData(farmerId);
        assertEquals(new BigDecimal("15.00"), farmerData.getWasteSoldTons());
        assertEquals(new BigDecimal("28500.00"), farmerData.getTotalEarnings());
        assertEquals(new BigDecimal("18750.00"), farmerData.getCo2ImpactKg());

        // Manufacturer Dashboard
        ManufacturerDashboardData mfgData = dashboardService.getManufacturerDashboardData(mfgId);
        assertEquals(new BigDecimal("15.00"), mfgData.getMaterialsPurchasedTons());
        assertEquals(new BigDecimal("28500.00"), mfgData.getTotalSpent());

        // Transporter Dashboard
        TransporterDashboardData transData = dashboardService.getTransporterDashboardData(transId);
        assertEquals(1, transData.getCompletedDeliveriesCount());
        assertTrue(transData.getTotalEarnings().compareTo(BigDecimal.ZERO) > 0);
    }

    // =========================================================================
    // SECTION 6: ORDER REJECTION & LISTING SAFE DELETION
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(16)
    void testOrderRejectionLifecycle() {
        // Create second listing
        WasteListing listing2 = new WasteListing();
        listing2.setFarmerId(farmerId);
        listing2.setTitle("Corn Cobs Residue");
        listing2.setWasteType("Corn Cob");
        listing2.setCategory(WasteCategory.HUSKS_AND_SHELLS);
        listing2.setDescription("Raw shelled corn cobs.");
        listing2.setQuantityTons(new BigDecimal("10.00"));
        listing2.setPricePerTon(new BigDecimal("1400.00"));
        listing2.setLocation("Patiala, Punjab");
        listing2.setAvailableFrom(LocalDate.now());
        listing2.setStatus(ListingStatus.AVAILABLE);

        WasteListing saved2 = listingService.saveListing(listing2);
        secondaryListingId = saved2.getId();
        createdListingIds.add(secondaryListingId);

        // Buyer places order
        Order order2 = orderService.placePurchaseRequest(
            mfgId, secondaryListingId, new BigDecimal("5.00"), "Sector 25 Industrial Area, Panipat, Haryana 132103", "Test rejection"
        );
        rejectedOrderId = order2.getId();
        createdOrderIds.add(rejectedOrderId);

        // Farmer rejects request
        orderService.rejectPurchaseRequest(farmerId, rejectedOrderId, "Material not available");

        Optional<Order> rejectedOpt = orderRepo.findById(rejectedOrderId);
        assertTrue(rejectedOpt.isPresent());
        assertEquals(OrderStatus.CANCELLED, rejectedOpt.get().getStatus());

        // Safe listing deletion: listing with cancelled order item history is safely deactivated
        assertDoesNotThrow(() -> listingService.safeDeleteListing(farmerId, secondaryListingId));
        Optional<WasteListing> delOpt = listingRepo.findById(secondaryListingId);
        assertTrue(delOpt.isPresent());
        assertEquals(ListingStatus.CANCELLED, delOpt.get().getStatus());

        // Test hard deletion on completely unreferenced listing
        WasteListing listing3 = new WasteListing();
        listing3.setFarmerId(farmerId);
        listing3.setTitle("Unreferenced Stubble");
        listing3.setWasteType("Stubble");
        listing3.setCategory(WasteCategory.CROP_RESIDUE);
        listing3.setDescription("Unreferenced test residue.");
        listing3.setQuantityTons(new BigDecimal("5.00"));
        listing3.setPricePerTon(new BigDecimal("1000.00"));
        listing3.setLocation("Patiala, Punjab");
        listing3.setAvailableFrom(LocalDate.now());
        listing3.setStatus(ListingStatus.AVAILABLE);

        WasteListing saved3 = listingService.saveListing(listing3);
        assertNotNull(saved3.getId());
        assertDoesNotThrow(() -> listingService.safeDeleteListing(farmerId, saved3.getId()));
        Optional<WasteListing> delOpt3 = listingRepo.findById(saved3.getId());
        assertTrue(delOpt3.isEmpty());
    }

    // =========================================================================
    // SECTION 7: PROFILE UPDATE & AVATAR PERSISTENCE
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(17)
    void testProfileUpdateAndAvatarPersistence() {
        Optional<User> userOpt = userRepo.findById(farmerId);
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();

        user.setFullName("Sardar Ramesh Singh");
        user.setPhone("+91 9876599999");
        user.setBio("Organic residue supplier and progressive farmer.");
        user.setAvatarUrl("file:///C:/Users/JOSAGACIYA/Pictures/avatar_farmer.png");

        userRepo.save(user);

        Optional<User> reloadedOpt = userRepo.findById(farmerId);
        assertTrue(reloadedOpt.isPresent());
        User reloaded = reloadedOpt.get();
        assertEquals("Sardar Ramesh Singh", reloaded.getFullName());
        assertEquals("+91 9876599999", reloaded.getPhone());
        assertEquals("Organic residue supplier and progressive farmer.", reloaded.getBio());
        assertEquals("file:///C:/Users/JOSAGACIYA/Pictures/avatar_farmer.png", reloaded.getAvatarUrl());
    }
}
