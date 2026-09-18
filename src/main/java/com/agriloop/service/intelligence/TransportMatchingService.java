package com.agriloop.service.intelligence;

import com.agriloop.model.Delivery;
import com.agriloop.model.TransporterProfile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic matching engine for assigning optimal logistics carriers to agricultural biomass cargo.
 */
public class TransportMatchingService {

    public record TransportMatch(
        TransporterProfile transporter,
        int suitabilityScore,
        boolean isCapacitySufficient,
        BigDecimal estimatedCost,
        String recommendationNotes
    ) {}

    private static TransportMatchingService instance;

    private TransportMatchingService() {}

    public static synchronized TransportMatchingService getInstance() {
        if (instance == null) {
            instance = new TransportMatchingService();
        }
        return instance;
    }

    /**
     * Ranks and matches eligible transporters for a delivery dispatch.
     */
    public List<TransportMatch> findSuitableTransporters(Delivery delivery, BigDecimal cargoTons, List<TransporterProfile> transporters) {
        List<TransportMatch> matches = new ArrayList<>();
        if (transporters == null || transporters.isEmpty()) return matches;

        BigDecimal loadWeight = cargoTons != null ? cargoTons : new BigDecimal("10.0");
        BigDecimal distance = delivery != null && delivery.getDistanceKm() != null ? delivery.getDistanceKm() : new BigDecimal("45.0");

        for (TransporterProfile tp : transporters) {
            if (!tp.isAvailable()) continue;

            BigDecimal maxPayload = tp.getMaxPayloadTons() != null ? tp.getMaxPayloadTons() : BigDecimal.ZERO;
            boolean capacityOk = maxPayload.compareTo(loadWeight) >= 0;

            int capacityScore = capacityOk ? 100 : Math.max(30, (int) (maxPayload.doubleValue() / loadWeight.doubleValue() * 100));

            // Radius suitability
            BigDecimal radius = tp.getOperatingRadiusKm() != null ? tp.getOperatingRadiusKm() : new BigDecimal("50.0");
            int radiusScore = radius.compareTo(distance) >= 0 ? 100 : Math.max(40, (int) (radius.doubleValue() / distance.doubleValue() * 100));

            // Base rate: approx ₹35 per ton-km
            BigDecimal baseRatePerTonKm = new BigDecimal("35.00");
            BigDecimal estCost = baseRatePerTonKm.multiply(loadWeight).multiply(distance).divide(new BigDecimal("10.0"), 2, java.math.RoundingMode.HALF_UP);
            if (estCost.compareTo(new BigDecimal("1500.00")) < 0) {
                estCost = new BigDecimal("1500.00"); // Minimum trip rate
            }

            int overallScore = (int) Math.round((capacityScore * 0.60) + (radiusScore * 0.40));
            String notes = String.format("%s (Max %.1f Tons) • %.0f km radius capacity", 
                tp.getVehicleType(), maxPayload.doubleValue(), radius.doubleValue());

            matches.add(new TransportMatch(tp, overallScore, capacityOk, estCost, notes));
        }

        matches.sort(Comparator.comparingInt(TransportMatch::suitabilityScore).reversed());
        return matches;
    }
}
