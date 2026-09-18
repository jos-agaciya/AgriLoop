package com.agriloop.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Environmental impact record (CO2 diversion, methane prevention, energy valorization).
 */
public class SustainabilityRecord {
    private Long id;
    private Long userId;
    private BigDecimal wasteDivertedTons;
    private BigDecimal co2SavedKg;
    private BigDecimal methanePreventedKg;
    private BigDecimal energyGeneratedKwh;
    private LocalDate calculationDate;
    private LocalDateTime createdAt;

    public SustainabilityRecord() {
        this.wasteDivertedTons = BigDecimal.ZERO;
        this.co2SavedKg = BigDecimal.ZERO;
        this.methanePreventedKg = BigDecimal.ZERO;
        this.energyGeneratedKwh = BigDecimal.ZERO;
        this.calculationDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    public SustainabilityRecord(Long userId, BigDecimal wasteDivertedTons, BigDecimal co2SavedKg, BigDecimal methanePreventedKg, BigDecimal energyGeneratedKwh, LocalDate calculationDate) {
        this.userId = userId;
        this.wasteDivertedTons = wasteDivertedTons != null ? wasteDivertedTons : BigDecimal.ZERO;
        this.co2SavedKg = co2SavedKg != null ? co2SavedKg : BigDecimal.ZERO;
        this.methanePreventedKg = methanePreventedKg != null ? methanePreventedKg : BigDecimal.ZERO;
        this.energyGeneratedKwh = energyGeneratedKwh != null ? energyGeneratedKwh : BigDecimal.ZERO;
        this.calculationDate = calculationDate != null ? calculationDate : LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getWasteDivertedTons() { return wasteDivertedTons; }
    public void setWasteDivertedTons(BigDecimal wasteDivertedTons) { this.wasteDivertedTons = wasteDivertedTons; }

    public BigDecimal getCo2SavedKg() { return co2SavedKg; }
    public void setCo2SavedKg(BigDecimal co2SavedKg) { this.co2SavedKg = co2SavedKg; }

    public BigDecimal getMethanePreventedKg() { return methanePreventedKg; }
    public void setMethanePreventedKg(BigDecimal methanePreventedKg) { this.methanePreventedKg = methanePreventedKg; }

    public BigDecimal getEnergyGeneratedKwh() { return energyGeneratedKwh; }
    public void setEnergyGeneratedKwh(BigDecimal energyGeneratedKwh) { this.energyGeneratedKwh = energyGeneratedKwh; }

    public LocalDate getCalculationDate() { return calculationDate; }
    public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
