package com.agriloop.service.intelligence;

import com.agriloop.model.SustainabilityRecord;
import com.agriloop.repository.SustainabilityRepository;
import com.agriloop.repository.impl.JdbcSustainabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Deterministic carbon accounting and ecological impact calculation engine.
 */
public class SustainabilityService {
    private static final Logger logger = LoggerFactory.getLogger(SustainabilityService.class);
    private static SustainabilityService instance;

    private final SustainabilityRepository sustainabilityRepo = new JdbcSustainabilityRepository();

    // Standardized scientific bio-economy conversion factors:
    // 1 Ton crop residue diverted from burning = 1,250 kg CO2e net avoided emissions
    private static final BigDecimal CO2_PER_TON_KG = new BigDecimal("1250.00");
    // 1 Ton residue diverted from rotting in field water = 45 kg Methane (CH4) avoided
    private static final BigDecimal METHANE_PER_TON_KG = new BigDecimal("45.00");
    // 1 Ton dry agricultural biomass = approx 850 kWh thermal / electric potential
    private static final BigDecimal ENERGY_PER_TON_KWH = new BigDecimal("850.00");

    private SustainabilityService() {}

    public static synchronized SustainabilityService getInstance() {
        if (instance == null) {
            instance = new SustainabilityService();
        }
        return instance;
    }

    /**
     * Records verified environmental sustainability impact for a completed transaction.
     */
    public void recordCompletedTransactionImpact(Long farmerId, Long buyerId, BigDecimal quantityTons) {
        if (quantityTons == null || quantityTons.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal co2Saved = quantityTons.multiply(CO2_PER_TON_KG).setScale(2, RoundingMode.HALF_UP);
        BigDecimal methanePrevented = quantityTons.multiply(METHANE_PER_TON_KG).setScale(2, RoundingMode.HALF_UP);
        BigDecimal energyGen = quantityTons.multiply(ENERGY_PER_TON_KWH).setScale(2, RoundingMode.HALF_UP);

        LocalDate today = LocalDate.now();

        // 1. Record for Farmer / Seller
        if (farmerId != null) {
            SustainabilityRecord farmerRec = new SustainabilityRecord(
                farmerId, quantityTons, co2Saved, methanePrevented, energyGen, today
            );
            try {
                sustainabilityRepo.save(farmerRec);
                logger.info("Recorded farmer sustainability impact: {} tons / {} kg CO2 for user {}", quantityTons, co2Saved, farmerId);
            } catch (Exception e) {
                logger.error("Failed to record farmer sustainability impact", e);
            }
        }

        // 2. Record for Manufacturer / Buyer
        if (buyerId != null) {
            SustainabilityRecord buyerRec = new SustainabilityRecord(
                buyerId, quantityTons, co2Saved, methanePrevented, energyGen, today
            );
            try {
                sustainabilityRepo.save(buyerRec);
                logger.info("Recorded buyer sustainability impact: {} tons / {} kg CO2 for user {}", quantityTons, co2Saved, buyerId);
            } catch (Exception e) {
                logger.error("Failed to record buyer sustainability impact", e);
            }
        }
    }

    public record SustainabilityImpact(
        BigDecimal wasteDivertedTons,
        BigDecimal co2SavedKg,
        BigDecimal methanePreventedKg,
        BigDecimal energyGeneratedKwh
    ) {}

    public SustainabilityImpact calculateImpact(String wasteType, BigDecimal quantityTons) {
        BigDecimal tons = quantityTons != null ? quantityTons : BigDecimal.ZERO;
        BigDecimal co2 = tons.multiply(CO2_PER_TON_KG).setScale(2, RoundingMode.HALF_UP);
        BigDecimal methane = tons.multiply(METHANE_PER_TON_KG).setScale(2, RoundingMode.HALF_UP);
        BigDecimal energy = tons.multiply(ENERGY_PER_TON_KWH).setScale(2, RoundingMode.HALF_UP);
        return new SustainabilityImpact(tons, co2, methane, energy);
    }

    public BigDecimal calculateCo2Avoided(BigDecimal tons) {
        if (tons == null) return BigDecimal.ZERO;
        return tons.multiply(CO2_PER_TON_KG).setScale(2, RoundingMode.HALF_UP);
    }
}
