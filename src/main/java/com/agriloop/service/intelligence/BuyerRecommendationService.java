package com.agriloop.service.intelligence;

import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.WasteListing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic recommendation engine matching available marketplace listings to manufacturer procurement criteria.
 */
public class BuyerRecommendationService {

    public record RecommendedListing(
        WasteListing listing,
        int overallMatchScore,
        int materialCompatibilityScore,
        int quantityFitScore,
        int locationScore,
        int priceScore,
        String recommendationReason
    ) {}

    private static BuyerRecommendationService instance;

    private BuyerRecommendationService() {}

    public static synchronized BuyerRecommendationService getInstance() {
        if (instance == null) {
            instance = new BuyerRecommendationService();
        }
        return instance;
    }

    /**
     * Ranks and scores available marketplace listings for a specific manufacturer.
     */
    public List<RecommendedListing> getRecommendations(List<WasteListing> availableListings, ManufacturerProfile buyerProfile) {
        List<RecommendedListing> list = new ArrayList<>();
        if (availableListings == null || availableListings.isEmpty()) return list;

        String requiredWaste = buyerProfile != null && buyerProfile.getRequiredWasteTypes() != null 
            ? buyerProfile.getRequiredWasteTypes().toLowerCase() : "";
        String industry = buyerProfile != null && buyerProfile.getIndustryType() != null 
            ? buyerProfile.getIndustryType().toLowerCase() : "";
        String facilityAddress = buyerProfile != null && buyerProfile.getFacilityAddress() != null 
            ? buyerProfile.getFacilityAddress().toLowerCase() : "";
        BigDecimal capacity = buyerProfile != null && buyerProfile.getProcessingCapacityTons() != null 
            ? buyerProfile.getProcessingCapacityTons() : new BigDecimal("50.0");

        for (WasteListing listing : availableListings) {
            String title = listing.getTitle() != null ? listing.getTitle().toLowerCase() : "";
            String wasteType = listing.getWasteType() != null ? listing.getWasteType().toLowerCase() : "";
            String location = listing.getLocation() != null ? listing.getLocation().toLowerCase() : "";

            // 1. Material Compatibility (40%)
            int materialScore = 60; // base compatibility
            if (!requiredWaste.isBlank() && (requiredWaste.contains(wasteType) || title.contains(requiredWaste))) {
                materialScore = 100;
            } else if (industry.contains("energy") || industry.contains("biomass") || industry.contains("thermal")) {
                materialScore = 90;
            } else if (industry.contains("paper") || industry.contains("packaging")) {
                materialScore = 85;
            }

            // 2. Quantity Fit (25%)
            int qtyScore = 70;
            BigDecimal qty = listing.getQuantityTons() != null ? listing.getQuantityTons() : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) > 0 && capacity.compareTo(BigDecimal.ZERO) > 0) {
                double ratio = qty.doubleValue() / capacity.doubleValue();
                if (ratio >= 0.2 && ratio <= 1.0) {
                    qtyScore = 100;
                } else if (ratio > 1.0 && ratio <= 2.0) {
                    qtyScore = 85;
                } else {
                    qtyScore = 65;
                }
            }

            // 3. Location Proximity (20%)
            int locScore = 60;
            if (!facilityAddress.isBlank() && !location.isBlank()) {
                if (facilityAddress.contains(location) || location.contains(facilityAddress)) {
                    locScore = 100;
                } else {
                    // Check state/region common words
                    String[] tokens = facilityAddress.split("[,\\s]+");
                    for (String token : tokens) {
                        if (token.length() > 3 && location.contains(token)) {
                            locScore = 90;
                            break;
                        }
                    }
                }
            }

            // 4. Price Competitiveness (15%)
            int priceScore = 80;
            BigDecimal price = listing.getPricePerTon() != null ? listing.getPricePerTon() : new BigDecimal("2000");
            if (price.compareTo(new BigDecimal("1500")) <= 0) {
                priceScore = 95;
            } else if (price.compareTo(new BigDecimal("3000")) <= 0) {
                priceScore = 80;
            } else {
                priceScore = 65;
            }

            // Weighted Total Score
            int totalScore = (int) Math.round(
                (materialScore * 0.40) +
                (qtyScore * 0.25) +
                (locScore * 0.20) +
                (priceScore * 0.15)
            );

            String reason = String.format("High compatibility with %s • Optimal %.1f Ton load", 
                buyerProfile != null ? buyerProfile.getCompanyName() : "your facility", qty.doubleValue());

            list.add(new RecommendedListing(listing, totalScore, materialScore, qtyScore, locScore, priceScore, reason));
        }

        list.sort(Comparator.comparingInt(RecommendedListing::overallMatchScore).reversed());
        return list;
    }
}
