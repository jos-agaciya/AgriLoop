package com.agriloop.model;

import java.math.BigDecimal;

/**
 * Extended profile details for Manufacturer / Industrial Buyer stakeholders.
 */
public class ManufacturerProfile extends BaseEntity {
    private Long userId;
    private String companyName;
    private String industryType;
    private String facilityAddress;
    private String requiredWasteTypes;
    private BigDecimal processingCapacityTons;
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;

    public ManufacturerProfile() {}

    public ManufacturerProfile(Long userId, String companyName, String industryType, String facilityAddress) {
        this.userId = userId;
        this.companyName = companyName;
        this.industryType = industryType;
        this.facilityAddress = facilityAddress;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getIndustryType() { return industryType; }
    public void setIndustryType(String industryType) { this.industryType = industryType; }

    public String getFacilityAddress() { return facilityAddress; }
    public void setFacilityAddress(String facilityAddress) { this.facilityAddress = facilityAddress; }

    public String getRequiredWasteTypes() { return requiredWasteTypes; }
    public void setRequiredWasteTypes(String requiredWasteTypes) { this.requiredWasteTypes = requiredWasteTypes; }

    public BigDecimal getProcessingCapacityTons() { return processingCapacityTons; }
    public void setProcessingCapacityTons(BigDecimal processingCapacityTons) { this.processingCapacityTons = processingCapacityTons; }

    public BigDecimal getGpsLatitude() { return gpsLatitude; }
    public void setGpsLatitude(BigDecimal gpsLatitude) { this.gpsLatitude = gpsLatitude; }

    public BigDecimal getGpsLongitude() { return gpsLongitude; }
    public void setGpsLongitude(BigDecimal gpsLongitude) { this.gpsLongitude = gpsLongitude; }
}
