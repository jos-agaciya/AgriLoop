package com.agriloop.service.intelligence;

import com.agriloop.model.WasteListing;
import com.agriloop.model.enums.WasteCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic rule-based market valuation and demand index engine for agricultural waste.
 */
public class ValueEstimationService {

    public record ValuationResult(
        BigDecimal estimatedPricePerTonMin,
        BigDecimal estimatedPricePerTonMax,
        BigDecimal estimatedPricePerTonAvg,
        BigDecimal totalEstimatedValueMin,
        BigDecimal totalEstimatedValueMax,
        String demandLevel,
        List<String> pricingFactors,
        String disclaimer
    ) {}

    private static ValueEstimationService instance;

    private ValueEstimationService() {}

    public static synchronized ValueEstimationService getInstance() {
        if (instance == null) {
            instance = new ValueEstimationService();
        }
        return instance;
    }

    /**
     * Calculates deterministic market estimated valuation for a given waste listing.
     */
    public ValuationResult estimateValue(WasteListing listing) {
        if (listing == null) {
            return new ValuationResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "MODERATE",
                List.of("No data available"),
                "AgriLoop Estimated Value (indicative benchmark based on regional biomass market trends)"
            );
        }

        WasteCategory category = listing.getCategory() != null ? listing.getCategory() : WasteCategory.CROP_RESIDUE;
        BigDecimal basePrice = new BigDecimal("1500.00");
        String demand = "MODERATE";

        switch (category) {
            case BAGASSE -> {
                basePrice = new BigDecimal("2400.00");
                demand = "VERY HIGH";
            }
            case HUSKS_AND_SHELLS -> {
                basePrice = new BigDecimal("2600.00");
                demand = "HIGH";
            }
            case STALK_AND_STRAW -> {
                basePrice = new BigDecimal("1950.00");
                demand = "HIGH";
            }
            case MANURE_AND_ORGANIC -> {
                basePrice = new BigDecimal("1200.00");
                demand = "MODERATE";
            }
            default -> {
                basePrice = new BigDecimal("1500.00");
                demand = "MODERATE";
            }
        }

        List<String> factors = new ArrayList<>();
        double multiplier = 1.0;

        // 1. Moisture Content Adjustment
        if (listing.getMoistureContentPct() != null) {
            double moisture = listing.getMoistureContentPct().doubleValue();
            if (moisture <= 12.0) {
                multiplier += 0.12;
                factors.add(String.format("Dry Grade (%.1f%% moisture): +12%% caloric density premium", moisture));
            } else if (moisture <= 18.0) {
                multiplier += 0.05;
                factors.add(String.format("Standard Grade (%.1f%% moisture): Optimal storage stability", moisture));
            } else if (moisture > 25.0) {
                multiplier -= 0.15;
                factors.add(String.format("High Moisture (%.1f%% moisture): -15%% drying & transport weight discount", moisture));
            }
        } else {
            factors.add("Standard Moisture assumption (15% avg)");
        }

        // 2. Quantity Bulk Factor
        BigDecimal qty = listing.getQuantityTons() != null ? listing.getQuantityTons() : BigDecimal.ONE;
        if (qty.compareTo(new BigDecimal("50.0")) >= 0) {
            multiplier += 0.08;
            factors.add("Commercial Bulk Lot (50+ Tons): +8% consolidated freight efficiency");
        } else if (qty.compareTo(new BigDecimal("10.0")) < 0) {
            multiplier -= 0.05;
            factors.add("Small Lot (<10 Tons): -5% localized aggregation cost");
        }

        BigDecimal avgPrice = basePrice.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal minPrice = avgPrice.multiply(new BigDecimal("0.92")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxPrice = avgPrice.multiply(new BigDecimal("1.08")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal minTotal = minPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxTotal = maxPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);

        return new ValuationResult(
            minPrice,
            maxPrice,
            avgPrice,
            minTotal,
            maxTotal,
            demand,
            factors,
            "AgriLoop Estimated Value (deterministic rule-based indicative benchmark; final price settled directly between buyer and seller)"
        );
    }
}
