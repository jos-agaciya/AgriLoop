package com.agriloop.model;

import java.math.BigDecimal;

/**
 * Extended profile details for Farmer / Seller stakeholders.
 */
public class FarmerProfile extends BaseEntity {
    private Long userId;
    private String farmName;
    private String farmLocation;
    private BigDecimal farmSizeAcres;
    private String primaryCropTypes;
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;

    public FarmerProfile() {}

    public FarmerProfile(Long userId, String farmName, String farmLocation) {
        this.userId = userId;
        this.farmName = farmName;
        this.farmLocation = farmLocation;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getFarmLocation() { return farmLocation; }
    public void setFarmLocation(String farmLocation) { this.farmLocation = farmLocation; }

    public BigDecimal getFarmSizeAcres() { return farmSizeAcres; }
    public void setFarmSizeAcres(BigDecimal farmSizeAcres) { this.farmSizeAcres = farmSizeAcres; }

    public String getPrimaryCropTypes() { return primaryCropTypes; }
    public void setPrimaryCropTypes(String primaryCropTypes) { this.primaryCropTypes = primaryCropTypes; }

    public BigDecimal getGpsLatitude() { return gpsLatitude; }
    public void setGpsLatitude(BigDecimal gpsLatitude) { this.gpsLatitude = gpsLatitude; }

    public BigDecimal getGpsLongitude() { return gpsLongitude; }
    public void setGpsLongitude(BigDecimal gpsLongitude) { this.gpsLongitude = gpsLongitude; }
}
