package com.agriloop.model;

import com.agriloop.model.enums.ListingStatus;
import com.agriloop.model.enums.WasteCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an agricultural waste listing created by a Farmer.
 */
public class WasteListing extends BaseEntity {
    private Long farmerId;
    private String title;
    private String wasteType;
    private WasteCategory category;
    private String description;
    private BigDecimal quantityTons;
    private BigDecimal pricePerTon;
    private BigDecimal moistureContentPct;
    private String location;
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
    private ListingStatus status;
    private LocalDate availableFrom;
    private LocalDate availableUntil;

    // Transient UI helper properties
    private String farmerName;

    public WasteListing() {
        this.status = ListingStatus.AVAILABLE;
        this.category = WasteCategory.CROP_RESIDUE;
        this.availableFrom = LocalDate.now();
    }

    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }

    public WasteCategory getCategory() { return category; }
    public void setCategory(WasteCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getQuantityTons() { return quantityTons; }
    public void setQuantityTons(BigDecimal quantityTons) { this.quantityTons = quantityTons; }

    public BigDecimal getPricePerTon() { return pricePerTon; }
    public void setPricePerTon(BigDecimal pricePerTon) { this.pricePerTon = pricePerTon; }

    public BigDecimal getMoistureContentPct() { return moistureContentPct; }
    public void setMoistureContentPct(BigDecimal moistureContentPct) { this.moistureContentPct = moistureContentPct; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getGpsLatitude() { return gpsLatitude; }
    public void setGpsLatitude(BigDecimal gpsLatitude) { this.gpsLatitude = gpsLatitude; }

    public BigDecimal getGpsLongitude() { return gpsLongitude; }
    public void setGpsLongitude(BigDecimal gpsLongitude) { this.gpsLongitude = gpsLongitude; }

    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }

    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate availableFrom) { this.availableFrom = availableFrom; }

    public LocalDate getAvailableUntil() { return availableUntil; }
    public void setAvailableUntil(LocalDate availableUntil) { this.availableUntil = availableUntil; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public BigDecimal getTotalValue() {
        if (quantityTons != null && pricePerTon != null) {
            return quantityTons.multiply(pricePerTon);
        }
        return BigDecimal.ZERO;
    }
}
