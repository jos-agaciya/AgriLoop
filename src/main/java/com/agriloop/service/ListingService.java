package com.agriloop.service;

import com.agriloop.database.DatabaseManager;
import com.agriloop.exception.AgriLoopException;
import com.agriloop.exception.ValidationException;
import com.agriloop.model.WasteListing;
import com.agriloop.model.enums.ListingStatus;
import com.agriloop.repository.WasteListingRepository;
import com.agriloop.repository.impl.JdbcWasteListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service managing agricultural waste listings and safe lifecycle modifications.
 */
public class ListingService {
    private static final Logger logger = LoggerFactory.getLogger(ListingService.class);
    private static ListingService instance;

    private final WasteListingRepository listingRepo = new JdbcWasteListingRepository();

    private ListingService() {}

    public static synchronized ListingService getInstance() {
        if (instance == null) {
            instance = new ListingService();
        }
        return instance;
    }

    public WasteListing saveListing(WasteListing listing) {
        if (listing == null) throw new ValidationException("Listing cannot be null");
        if (listing.getTitle() == null || listing.getTitle().isBlank()) throw new ValidationException("Title is required");
        if (listing.getWasteType() == null || listing.getWasteType().isBlank()) throw new ValidationException("Waste type is required");
        if (listing.getQuantityTons() == null || listing.getQuantityTons().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity must be greater than zero");
        }
        if (listing.getPricePerTon() == null || listing.getPricePerTon().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price must be greater than zero");
        }

        return listingRepo.save(listing);
    }

    /**
     * Safely deletes or cancels a listing, ensuring no active unfulfilled orders exist.
     */
    public void safeDeleteListing(Long farmerId, Long listingId) {
        WasteListing listing = listingRepo.findById(listingId)
            .orElseThrow(() -> new AgriLoopException("Listing not found"));

        if (!farmerId.equals(listing.getFarmerId())) {
            throw new AgriLoopException("Unauthorized: This listing does not belong to your account");
        }

        // Verify no active unfulfilled orders exist
        if (hasActiveOrders(listingId)) {
            throw new AgriLoopException("Cannot remove listing: There are active pending or in-transit orders linked to this material.");
        }

        // If historical order items exist, deactivate listing status to preserve audit trail
        if (hasHistoricalOrderItems(listingId)) {
            listing.setStatus(ListingStatus.CANCELLED);
            listingRepo.save(listing);
        } else {
            listingRepo.deleteById(listingId);
        }

        logger.info("Farmer {} safely removed/deactivated waste listing #{}", farmerId, listingId);

        NotificationService.getInstance().pushNotification(
            "Listing Removed",
            "Listing '" + listing.getTitle() + "' was successfully deactivated.",
            "INFO"
        );
    }

    private boolean hasHistoricalOrderItems(Long listingId) {
        String sql = "SELECT COUNT(*) FROM order_items WHERE listing_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking order history for listing {}", listingId, e);
        }
        return false;
    }

    private boolean hasActiveOrders(Long listingId) {
        String sql = "SELECT COUNT(*) FROM orders o " +
                     "JOIN order_items oi ON o.id = oi.order_id " +
                     "WHERE oi.listing_id = ? AND o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'IN_TRANSIT')";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking active orders for listing {}", listingId, e);
        }
        return false;
    }

    public List<WasteListing> getListingsByFarmer(Long farmerId) {
        if (farmerId == null) return List.of();
        return listingRepo.findByFarmerId(farmerId);
    }

    public Optional<WasteListing> getListingById(Long id) {
        if (id == null) return Optional.empty();
        return listingRepo.findById(id);
    }
}
