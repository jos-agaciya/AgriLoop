package com.agriloop.repository;

import com.agriloop.model.SustainabilityRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository interface for Environmental & Sustainability tracking.
 */
public interface SustainabilityRepository extends BaseRepository<SustainabilityRecord, Long> {
    List<SustainabilityRecord> findByUserId(Long userId);
    BigDecimal getCo2SavedByUserId(Long userId);
    BigDecimal getWasteDivertedByUserId(Long userId);
    BigDecimal getTotalWasteDivertedTons();
    BigDecimal getTotalCo2SavedKg();
    BigDecimal getTotalMethanePreventedKg();
    BigDecimal getTotalEnergyGeneratedKwh();
}
